package com.meetingsum.pipeline;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.meetingsum.config.AppProperties;
import com.meetingsum.model.enums.ErrorCode;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.util.List;
import java.util.concurrent.TimeUnit;

@Component
public class TranscriberService {

    private static final Logger log = LoggerFactory.getLogger(TranscriberService.class);
    private final ObjectMapper objectMapper = new ObjectMapper();
    private final AppProperties props;

    public TranscriberService(AppProperties props) {
        this.props = props;
    }

    public WhisperResult transcribe(String audioPath) throws IOException, InterruptedException {
        String scriptPath = findScriptPath();
        String python = findPython();

        ProcessBuilder pb = new ProcessBuilder(
                python, scriptPath,
                "--audio", audioPath,
                "--model", props.getWhisperModel(),
                "--device", props.getWhisperDevice()
        );
        // Add ffmpeg directory to PATH so whisper library can find it
        Path ffmpegDir = Path.of(props.getFfmpegPath()).getParent();
        String currentPath = System.getenv("PATH");
        pb.environment().put("PATH", ffmpegDir.toString() + java.io.File.pathSeparator + currentPath);
        pb.redirectErrorStream(false);

        log.info("Running Whisper transcription: {} --audio {} --model {}", python, audioPath, props.getWhisperModel());

        Process process = pb.start();

        int timeoutSeconds = props.getTimeout().getTranscriptionSeconds();
        boolean finished = process.waitFor(timeoutSeconds, TimeUnit.SECONDS);

        if (!finished) {
            process.destroyForcibly();
            throw new PipelineException(ErrorCode.TRANSCRIPTION_TIMEOUT,
                    "Whisper transcription timed out after " + timeoutSeconds + "s");
        }

        String stderr = new String(process.getErrorStream().readAllBytes(), StandardCharsets.UTF_8);
        if (!stderr.isEmpty()) {
            log.warn("Whisper stderr: {}", stderr);
        }

        if (process.exitValue() != 0) {
            throw new PipelineException(ErrorCode.TRANSCRIPTION_FAILED,
                    "Whisper transcription failed: " + stderr);
        }

        String stdout = new String(process.getInputStream().readAllBytes(), StandardCharsets.UTF_8);
        WhisperResult result;
        try {
            result = objectMapper.readValue(stdout, WhisperResult.class);
        } catch (IOException e) {
            throw new PipelineException(ErrorCode.TRANSCRIPTION_INVALID_OUTPUT,
                    "Failed to parse Whisper output as JSON", e);
        }
        log.info("Transcription complete, {} segments, language: {}", result.segments().size(), result.language());
        return result;
    }

    private String findScriptPath() {
        Path projectRoot = Path.of(props.getWhisperScriptPath());
        if (projectRoot.toFile().exists()) {
            return projectRoot.toString();
        }
        return props.getWhisperScriptPath();
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

    public record WhisperResult(
            List<Segment> segments,
            @JsonProperty("full_text") String fullText,
            String language
    ) {
        public record Segment(double start, double end, String text, String speaker) {}
    }
}
