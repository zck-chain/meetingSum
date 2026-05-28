package com.meetingsum.controller;

import com.meetingsum.model.entity.Meeting;
import com.meetingsum.model.enums.MeetingStatus;
import com.meetingsum.service.FileStorageService;
import com.meetingsum.service.MeetingService;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.nio.charset.StandardCharsets;
import java.nio.file.Path;

@RestController
@RequestMapping("/api/v1/meetings")
public class ExportController {

    private final MeetingService meetingService;
    private final FileStorageService fileStorageService;

    public ExportController(MeetingService meetingService, FileStorageService fileStorageService) {
        this.meetingService = meetingService;
        this.fileStorageService = fileStorageService;
    }

    @GetMapping("/{meetingId}/export")
    public ResponseEntity<Resource> exportMeeting(
            @PathVariable String meetingId,
            @RequestParam(defaultValue = "md") String format) {

        Meeting meeting = meetingService.findByIdOrThrow(meetingId);

        if (meeting.getStatus() != MeetingStatus.COMPLETED) {
            throw new IllegalArgumentException("Meeting processing not yet completed");
        }

        if (!format.equals("md") && !format.equals("transcript") && !format.equals("docx")) {
            throw new IllegalArgumentException("Unsupported format: " + format);
        }

        Path filePathObj = fileStorageService.getExportFilePath(meetingId, format);
        if (filePathObj == null || !filePathObj.toFile().exists()) {
            throw new RuntimeException("Export file not found: " + format);
        }

        FileSystemResource resource = new FileSystemResource(filePathObj.toFile());

        MediaType mediaType = switch (format) {
            case "docx" -> MediaType.parseMediaType("application/vnd.openxmlformats-officedocument.wordprocessingml.document");
            default -> MediaType.parseMediaType("text/markdown");
        };

        String title = meeting.getTitle() != null ? meeting.getTitle() : "summary";
        String downloadName = switch (format) {
            case "md" -> title + "_摘要.md";
            case "transcript" -> title + "_转录.md";
            case "docx" -> title + "_摘要.docx";
            default -> "download";
        };

        String encodedName = java.util.Base64.getEncoder().encodeToString(downloadName.getBytes(StandardCharsets.UTF_8));

        return ResponseEntity.ok()
                .contentType(mediaType)
                .header(HttpHeaders.CONTENT_DISPOSITION,
                        "attachment; filename=\"" + downloadName + "\"; filename*=UTF-8''" + encodedName)
                .body(resource);
    }
}
