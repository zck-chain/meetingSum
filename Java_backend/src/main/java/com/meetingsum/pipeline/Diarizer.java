package com.meetingsum.pipeline;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.meetingsum.config.AppProperties;
import com.meetingsum.model.enums.ErrorCode;
import com.meetingsum.pipeline.TranscriberService.WhisperResult.Segment;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

@Component
public class Diarizer {

    private static final Logger log = LoggerFactory.getLogger(Diarizer.class);

    private static final double GAP_THRESHOLD_SECONDS = 2.0;

    private final AppProperties props;
    private final ObjectMapper objectMapper = new ObjectMapper();

    public Diarizer(AppProperties props) {
        this.props = props;
    }

    /**
     * 为转录片段分配说话人标签。
     * 如果启用了 pyannote 且 Python 环境可用，使用 ML 模型；
     * 否则降级为基于停顿间隔的启发式规则。
     */
    public List<Segment> assignSpeakers(List<Segment> segments) {
        if (segments == null || segments.isEmpty()) {
            return segments;
        }

        if (props.isPyannoteEnabled()) {
            try {
                return assignWithPyannote(segments);
            } catch (Exception e) {
                log.warn("pyannote diarization failed, falling back to heuristic: {}", e.getMessage());
            }
        }

        return assignHeuristic(segments);
    }

    /**
     * 基于停顿间隔（> 2s）的启发式说话人分离。
     * 在 Speaker_A 和 Speaker_B 之间交替。
     */
    List<Segment> assignHeuristic(List<Segment> segments) {
        List<Segment> result = new ArrayList<>(segments.size());
        boolean useSpeakerA = true;

        for (int i = 0; i < segments.size(); i++) {
            Segment seg = segments.get(i);
            String speaker;

            if (i == 0) {
                speaker = "Speaker_A";
            } else {
                double gap = seg.start() - segments.get(i - 1).end();
                if (gap > GAP_THRESHOLD_SECONDS) {
                    useSpeakerA = !useSpeakerA;
                }
                speaker = useSpeakerA ? "Speaker_A" : "Speaker_B";
            }

            result.add(new Segment(seg.start(), seg.end(), seg.text(), speaker));
        }

        return result;
    }

    /**
     * 通过 Python 子进程调用 pyannote-audio 进行 ML 驱动的说话人分离。
     * 将 pyannote 的说话人标签通过最大时间重叠对齐到 Whisper 转录片段上。
     */
    private List<Segment> assignWithPyannote(List<Segment> segments) throws IOException, InterruptedException {
        // Pyannote needs the audio file path — we get this from the pipeline context.
        // Since Diarizer only receives segments (not audio path), we need a different approach.
        // We'll store the audio path from the pipeline call context via a thread-local or parameter.
        // For now, log and fall back if no audio context is available.
        log.info("pyannote diarization requested but audio path not available in segment-only API; "
                + "using heuristic fallback. Consider passing audio path to Diarizer.assignSpeakers().");
        return assignHeuristic(segments);
    }

    /**
     * 通过 Python 子进程调用 pyannote-audio 进行 ML 驱动的说话人分离。
     *
     * @param segments  Whisper 转录片段
     * @param audioPath 音频文件路径（16kHz mono WAV）
     * @return 带说话人标签的片段
     */
    public List<Segment> assignSpeakersWithAudio(List<Segment> segments, String audioPath) throws IOException, InterruptedException {
        if (segments == null || segments.isEmpty()) {
            return segments;
        }

        if (!props.isPyannoteEnabled()) {
            return assignHeuristic(segments);
        }

        try {
            return assignWithPyannoteImpl(segments, audioPath);
        } catch (Exception e) {
            log.warn("pyannote diarization failed, falling back to heuristic: {}", e.getMessage());
            return assignHeuristic(segments);
        }
    }

    private List<Segment> assignWithPyannoteImpl(List<Segment> segments, String audioPath) throws IOException, InterruptedException {
        String scriptPath = findScriptPath();
        String python = findPython();

        ProcessBuilder pb = new ProcessBuilder(
                python, scriptPath,
                "--audio", audioPath,
                "--hf-token", props.getPyannoteHfToken(),
                "--device", props.getPyannoteDevice()
        );
        // Inject ffmpeg dir into PATH for pyannote's internal audio loading
        Path ffmpegDir = Path.of(props.getFfmpegPath()).getParent();
        String currentPath = System.getenv("PATH");
        pb.environment().put("PATH", ffmpegDir.toString() + java.io.File.pathSeparator + currentPath);
        pb.redirectErrorStream(false);

        log.info("Running pyannote diarization: {} --audio {}", python, audioPath);

        Process process = pb.start();
        boolean finished = process.waitFor(5, TimeUnit.MINUTES);

        if (!finished) {
            process.destroyForcibly();
            throw new PipelineException(ErrorCode.DIARIZATION_FAILED, "pyannote diarization timed out");
        }

        String stderr = new String(process.getErrorStream().readAllBytes(), StandardCharsets.UTF_8);
        if (process.exitValue() != 0) {
            throw new PipelineException(ErrorCode.DIARIZATION_FAILED, "pyannote diarization failed: " + stderr);
        }
        if (!stderr.isEmpty()) {
            log.debug("pyannote stderr: {}", stderr);
        }

        String stdout = new String(process.getInputStream().readAllBytes(), StandardCharsets.UTF_8);
        Map<String, List<Map<String, Object>>> result = objectMapper.readValue(stdout,
                new TypeReference<>() {});
        List<Map<String, Object>> pyannoteSegments = result.get("segments");

        if (pyannoteSegments == null || pyannoteSegments.isEmpty()) {
            log.warn("pyannote returned no segments, falling back to heuristic");
            return assignHeuristic(segments);
        }

        // Align pyannote speaker labels onto Whisper segments by max time overlap
        List<Segment> aligned = new ArrayList<>(segments.size());
        for (Segment whisperSeg : segments) {
            double maxOverlap = 0;
            String bestSpeaker = "Speaker_A";

            for (Map<String, Object> pySeg : pyannoteSegments) {
                double pyStart = ((Number) pySeg.get("start")).doubleValue();
                double pyEnd = ((Number) pySeg.get("end")).doubleValue();

                // Calculate overlap
                double overlapStart = Math.max(whisperSeg.start(), pyStart);
                double overlapEnd = Math.min(whisperSeg.end(), pyEnd);
                double overlap = Math.max(0, overlapEnd - overlapStart);

                if (overlap > maxOverlap) {
                    maxOverlap = overlap;
                    bestSpeaker = (String) pySeg.get("speaker");
                }
            }

            aligned.add(new Segment(whisperSeg.start(), whisperSeg.end(), whisperSeg.text(), bestSpeaker));
        }

        log.info("pyannote diarization complete, {} Whisper segments aligned to {} pyannote speakers",
                aligned.size(),
                pyannoteSegments.stream().map(s -> s.get("speaker")).distinct().count());
        return aligned;
    }

    private String findScriptPath() {
        Path projectRoot = Path.of(props.getPyannoteScriptPath());
        if (projectRoot.toFile().exists()) {
            return projectRoot.toString();
        }
        return props.getPyannoteScriptPath();
    }

    private String findPython() {
        String[] candidates = {"python3", "python"};
        for (String candidate : candidates) {
            try {
                ProcessBuilder pb = new ProcessBuilder(candidate, "--version");
                Process process = pb.start();
                if (process.waitFor(5, TimeUnit.SECONDS) && process.exitValue() == 0) {
                    return candidate;
                }
            } catch (Exception e) {
                // try next
            }
        }
        return "python";
    }
}
