package com.meetingsum.service;

import com.meetingsum.model.dto.TaskStatusResponse;
import com.meetingsum.model.entity.Task;
import com.meetingsum.model.enums.ErrorCode;
import com.meetingsum.model.enums.TaskStage;
import com.meetingsum.model.enums.TaskStatus;
import com.meetingsum.repository.TaskRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

@Service
public class TaskService {

    private static final DateTimeFormatter ISO_FORMAT = DateTimeFormatter.ISO_LOCAL_DATE_TIME;

    private final TaskRepository taskRepository;

    public TaskService(TaskRepository taskRepository) {
        this.taskRepository = taskRepository;
    }

    @Transactional
    public Task createTask(String meetingId) {
        Task task = new Task();
        task.setMeetingId(meetingId);
        task.setStatus(TaskStatus.PENDING);
        task.setStage(TaskStage.QUEUED);
        task.setProgress(0);
        return taskRepository.save(task);
    }

    @Transactional(readOnly = true)
    public Task findByIdOrThrow(String taskId) {
        return taskRepository.findById(taskId)
                .orElseThrow(() -> new TaskNotFoundException(taskId));
    }

    @Transactional(readOnly = true)
    public TaskStatusResponse getTaskStatus(String taskId) {
        Task task = findByIdOrThrow(taskId);
        return new TaskStatusResponse(
                task.getId(),
                task.getMeetingId(),
                task.getStage() != null ? task.getStage().getValue() : null,
                task.getProgress(),
                task.getStatus() != null ? task.getStatus().getValue() : null,
                task.getResultPath(),
                task.getCreatedAt() != null ? task.getCreatedAt().format(ISO_FORMAT) : null,
                task.getCompletedAt() != null ? task.getCompletedAt().format(ISO_FORMAT) : null,
                task.getErrorCode(),
                task.getErrorMessage()
        );
    }

    @Transactional
    public void updateProgress(String taskId, TaskStage stage, Integer percent, TaskStatus status) {
        Task task = findByIdOrThrow(taskId);
        if (stage != null) task.setStage(stage);
        if (percent != null) task.setProgress(percent);
        if (status != null) task.setStatus(status);
        taskRepository.save(task);
    }

    @Transactional
    public void markCompleted(String taskId) {
        Task task = findByIdOrThrow(taskId);
        task.setStage(TaskStage.COMPLETED);
        task.setProgress(100);
        task.setStatus(TaskStatus.COMPLETED);
        task.setCompletedAt(LocalDateTime.now());
        taskRepository.save(task);
    }

    @Transactional
    public void markFailed(String taskId, String errorMessage) {
        markFailed(taskId, errorMessage, ErrorCode.UNKNOWN_ERROR);
    }

    @Transactional
    public void markFailed(String taskId, String errorMessage, ErrorCode errorCode) {
        Task task = findByIdOrThrow(taskId);
        task.setStage(TaskStage.FAILED);
        task.setStatus(TaskStatus.FAILED);
        task.setErrorCode(errorCode != null ? errorCode.getCode() : ErrorCode.UNKNOWN_ERROR.getCode());
        task.setErrorMessage(errorMessage);
        task.setCompletedAt(LocalDateTime.now());
        taskRepository.save(task);
    }

    public static class TaskNotFoundException extends RuntimeException {
        public TaskNotFoundException(String id) {
            super("Task not found: " + id);
        }
    }
}
