package com.meetingsum.controller;

import com.meetingsum.model.dto.TaskStatusResponse;
import com.meetingsum.service.TaskService;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/tasks")
public class TaskController {

    private final TaskService taskService;

    public TaskController(TaskService taskService) {
        this.taskService = taskService;
    }

    @GetMapping("/{taskId}/status")
    public TaskStatusResponse getTaskStatus(@PathVariable String taskId) {
        return taskService.getTaskStatus(taskId);
    }
}
