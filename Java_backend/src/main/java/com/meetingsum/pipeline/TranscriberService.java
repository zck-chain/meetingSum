package com.meetingsum.pipeline;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.meetingsum.config.AppProperties;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.io.IOException;
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
        boolean finished = process.waitFor(30, TimeUnit.MINUTES);

        if (!finished) {
            process.destroyForcibly();
            throw new RuntimeException("Whisper transcription timed out");
        }

        String stderr = new String(process.getErrorStream().readAllBytes());
        if (!stderr.isEmpty()) {
            log.warn("Whisper stderr: {}", stderr);
        }

        if (process.exitValue() != 0) {
            throw new RuntimeException("Whisper transcription failed: " + stderr);
        }

        String stdout = new String(process.getInputStream().readAllBytes());
        WhisperResult result = objectMapper.readValue(stdout, WhisperResult.class);
        log.info("Transcription complete, {} segments, language: {}", result.segments().size(), result.language());
        return result;
    }

    private String findScriptPath() {
        // Look for the script in project root first, then in working directory
        Path projectRoot = Path.of(props.getWhisperScriptPath());
        if (projectRoot.toFile().exists()) {
            return projectRoot.toString();
        }
        // Try relative to classpath / working dir
        return props.getWhisperScriptPath();
    }

    private String findPython() {
        // Try python3 first, then python
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
