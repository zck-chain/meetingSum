package com.meetingsum.model.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

public record MeetingListItem(
        String id,
        String title,
        @JsonProperty("original_format") String originalFormat,
        @JsonProperty("duration_seconds") Integer durationSeconds,
        @JsonProperty("file_size_bytes") Long fileSizeBytes,
        String status,
        @JsonProperty("created_at") String createdAt,
        @JsonProperty("updated_at") String updatedAt
) {}
