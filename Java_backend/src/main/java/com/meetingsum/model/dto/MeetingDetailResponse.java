package com.meetingsum.model.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

public record MeetingDetailResponse(
        String id,
        String title,
        @JsonProperty("original_file") String originalFile,
        @JsonProperty("original_format") String originalFormat,
        @JsonProperty("duration_seconds") Integer durationSeconds,
        @JsonProperty("file_size_bytes") Long fileSizeBytes,
        String status,
        @JsonProperty("summary_json") SummaryData summaryJson,
        @JsonProperty("transcript_text") String transcriptText,
        @JsonProperty("error_message") String errorMessage,
        @JsonProperty("created_at") String createdAt,
        @JsonProperty("updated_at") String updatedAt
) {}
