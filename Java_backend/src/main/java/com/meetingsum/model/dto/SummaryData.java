package com.meetingsum.model.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.List;

public record SummaryData(
        String title,
        String summary,
        @JsonProperty("key_points") List<KeyPoint> keyPoints,
        List<Decision> decisions,
        @JsonProperty("action_items") List<ActionItem> actionItems,
        List<String> tags
) {
    public record KeyPoint(String topic, String content, String importance) {}
    public record Decision(String content, String proposer) {}
    public record ActionItem(String content, String assignee, String deadline) {}
}
