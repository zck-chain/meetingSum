package com.meetingsum.model.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

public record TaskStatusResponse(
        @JsonProperty("task_id") String taskId,
        @JsonProperty("meeting_id") String meetingId,
        String stage,
        Integer progress,
        String status,
        @JsonProperty("result_path") String resultPath,
        @JsonProperty("created_at") String createdAt,
        @JsonProperty("completed_at") String completedAt,
        @JsonProperty("error_code") String errorCode,
        @JsonProperty("error_detail") String errorDetail
) {}
