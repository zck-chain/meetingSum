package com.meetingsum.model.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

public record UploadResponse(
        @JsonProperty("meeting_id") String meetingId,
        @JsonProperty("task_id") String taskId,
        String filename,
        String status
) {}
