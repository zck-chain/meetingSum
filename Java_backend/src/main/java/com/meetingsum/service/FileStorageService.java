package com.meetingsum.service;

import com.meetingsum.config.AppProperties;
import com.meetingsum.util.IdGenerator;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Comparator;

@Service
public class FileStorageService {

    private final AppProperties props;

    public FileStorageService(AppProperties props) {
        this.props = props;
    }

    public String saveUploadFile(MultipartFile file) throws IOException {
        String originalFilename = file.getOriginalFilename();
        String safeName = com.meetingsum.util.FileValidationUtils.safeFilename(
                originalFilename != null ? originalFilename : "file.bin"
        );
        String extension = "";
        if (safeName.contains(".")) {
            extension = safeName.substring(safeName.lastIndexOf('.'));
        }

        String hashedName = IdGenerator.hashFilename(safeName) + extension;
        Path uploadDir = Path.of(props.getUploadDir()).toAbsolutePath().normalize();
        Files.createDirectories(uploadDir);
        Path filePath = uploadDir.resolve(hashedName);
        file.transferTo(filePath.toFile());

        return filePath.toString();
    }

    public void deleteMeetingFiles(String meetingId) {
        // Delete output directory
        Path outputDir = Path.of(props.getOutputDir()).resolve(meetingId);
        try {
            if (Files.exists(outputDir)) {
                Files.walk(outputDir)
                        .sorted(Comparator.reverseOrder())
                        .forEach(path -> {
                            try {
                                Files.delete(path);
                            } catch (IOException ignored) {}
                        });
            }
        } catch (IOException ignored) {}
    }

    public void deleteOriginalFile(String filePath) {
        if (filePath != null) {
            try {
                Files.deleteIfExists(Path.of(filePath));
            } catch (IOException ignored) {}
        }
    }

    public Path getExportFilePath(String meetingId, String format) {
        Path outputDir = Path.of(props.getOutputDir()).resolve(meetingId);
        return switch (format) {
            case "md" -> outputDir.resolve(meetingId + "_summary.md");
            case "transcript" -> outputDir.resolve(meetingId + "_transcript.md");
            case "docx" -> outputDir.resolve(meetingId + "_summary.docx");
            default -> null;
        };
    }
}
