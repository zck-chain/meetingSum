package com.meetingsum.controller;

import com.meetingsum.config.AppProperties;
import com.meetingsum.model.dto.*;
import com.meetingsum.model.entity.Meeting;
import com.meetingsum.model.entity.Task;
import com.meetingsum.service.FileStorageService;
import com.meetingsum.service.MeetingService;
import com.meetingsum.service.PipelineService;
import com.meetingsum.service.TaskService;
import com.meetingsum.util.FileValidationUtils;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Path;

@RestController
@RequestMapping("/api/v1/meetings")
public class MeetingController {

    private final MeetingService meetingService;
    private final TaskService taskService;
    private final FileStorageService fileStorageService;
    private final AppProperties props;
    private final ApplicationEventPublisher eventPublisher;

    public MeetingController(MeetingService meetingService, TaskService taskService,
                             FileStorageService fileStorageService, AppProperties props,
                             ApplicationEventPublisher eventPublisher) {
        this.meetingService = meetingService;
        this.taskService = taskService;
        this.fileStorageService = fileStorageService;
        this.props = props;
        this.eventPublisher = eventPublisher;
    }

    @PostMapping("/upload")
    public ResponseEntity<UploadResponse> uploadMeeting(@RequestParam("file") MultipartFile file) throws IOException {
        if (file.isEmpty() || file.getOriginalFilename() == null) {
            throw new IllegalArgumentException("Filename is required");
        }

        String filename = file.getOriginalFilename();

        if (!FileValidationUtils.isFormatAllowed(filename, props.getAllowedFormatList())) {
            throw new IllegalArgumentException("Unsupported format. Allowed: " + props.getAllowedFormats());
        }

        if (file.getSize() > props.getMaxFileSizeBytes()) {
            throw new PayloadTooLargeException("File too large. Max: " + props.getMaxFileSizeMb() + "MB");
        }

        String filePath = fileStorageService.saveUploadFile(file);
        String extension = FileValidationUtils.getFileExtension(filename);
        String title = filename.contains(".") ? filename.substring(0, filename.lastIndexOf('.')) : filename;

        Meeting meeting = meetingService.createMeeting(title, filePath, extension, file.getSize());
        Task task = taskService.createTask(meeting.getId());

        eventPublisher.publishEvent(new PipelineService.PipelineStartEvent(meeting.getId(), task.getId()));

        return ResponseEntity.status(HttpStatus.CREATED).body(
                new UploadResponse(meeting.getId(), task.getId(), filename, "pending")
        );
    }

    @GetMapping("")
    public MeetingListResponse listMeetings(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(name = "page_size", defaultValue = "20") int pageSize,
            @RequestParam(required = false) String status,
            @RequestParam(required = false) String search) {
        return meetingService.listMeetings(page, pageSize, status, search);
    }

    @GetMapping("/{meetingId}")
    public MeetingDetailResponse getMeeting(@PathVariable String meetingId) {
        return meetingService.getMeeting(meetingId);
    }

    @DeleteMapping("/{meetingId}")
    public DeleteResponse deleteMeeting(@PathVariable String meetingId) {
        meetingService.deleteMeeting(meetingId);
        return new DeleteResponse("deleted");
    }
}
