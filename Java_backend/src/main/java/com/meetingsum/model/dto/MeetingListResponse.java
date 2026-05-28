package com.meetingsum.model.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.List;

public record MeetingListResponse(
        List<MeetingListItem> items,
        long total,
        int page,
        @JsonProperty("page_size") int pageSize,
        @JsonProperty("total_pages") int totalPages
) {}
