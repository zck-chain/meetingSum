package com.meetingsum.pipeline;

import com.meetingsum.config.AppProperties;
import com.meetingsum.model.enums.ErrorCode;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.nio.file.Path;
import java.util.concurrent.TimeUnit;

@Component
public class AudioExtractor {

    private static final Logger log = LoggerFactory.getLogger(AudioExtractor.class);

    private final AppProperties props;

    public AudioExtractor(AppProperties props) {
        this.props = props;
    }

    public String extractAudio(String inputPath, String outputDir) throws IOException, InterruptedException {
        Path outputDirPath = Path.of(outputDir);
        outputDirPath.toFile().mkdirs();

        String outputFileName = getFileStem(inputPath) + "_audio.wav";
        Path outputPath = outputDirPath.resolve(outputFileName);

        ProcessBuilder pb = new ProcessBuilder(
                props.getFfmpegPath(),
                "-i", inputPath,
                "-ar", "16000",
                "-ac", "1",
                "-map", "0:a:0",
                "-y",
                outputPath.toString()
        );
        pb.redirectErrorStream(true);
        Process process = pb.start();

        int timeoutSeconds = props.getTimeout().getAudioExtractionSeconds();
        boolean finished = process.waitFor(timeoutSeconds, TimeUnit.SECONDS);

        if (!finished) {
            process.destroyForcibly();
            throw new PipelineException(ErrorCode.AUDIO_EXTRACTION_TIMEOUT,
                    "FFmpeg audio extraction timed out after " + timeoutSeconds + "s");
        }
        if (process.exitValue() != 0) {
            String stderr = new String(process.getInputStream().readAllBytes());
            throw new PipelineException(ErrorCode.AUDIO_EXTRACTION_FAILED,
                    "FFmpeg audio extraction failed: " + stderr);
        }

        log.info("Audio extracted to {}", outputPath);
        return outputPath.toString();
    }

    public double getMediaDuration(String inputPath) throws IOException, InterruptedException {
        ProcessBuilder pb = new ProcessBuilder(
                props.getFfprobePath(),
                "-v", "error",
                "-show_entries", "format=duration",
                "-of", "default=noprint_wrappers=1:nokey=1",
                inputPath
        );
        Process process = pb.start();
        boolean finished = process.waitFor(1, TimeUnit.MINUTES);

        if (!finished) {
            process.destroyForcibly();
            throw new PipelineException(ErrorCode.AUDIO_EXTRACTION_TIMEOUT, "ffprobe timed out");
        }
        if (process.exitValue() != 0) {
            String stderr = new String(process.getErrorStream().readAllBytes());
            throw new PipelineException(ErrorCode.AUDIO_EXTRACTION_FAILED, "ffprobe failed: " + stderr);
        }

        String output = new String(process.getInputStream().readAllBytes()).trim();
        return Double.parseDouble(output);
    }

    private String getFileStem(String path) {
        String name = Path.of(path).getFileName().toString();
        int dot = name.lastIndexOf('.');
        return dot > 0 ? name.substring(0, dot) : name;
    }
}
