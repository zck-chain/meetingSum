package com.meetingsum.model.enums;

import com.fasterxml.jackson.annotation.JsonValue;

public enum TaskStage {
    QUEUED("queued"),
    EXTRACTING_AUDIO("extracting_audio"),
    TRANSCRIBING("transcribing"),
    SUMMARIZING("summarizing"),
    EXPORTING("exporting"),
    COMPLETED("completed"),
    FAILED("failed");

    private final String value;

    TaskStage(String value) {
        this.value = value;
    }

    @JsonValue
    public String getValue() {
        return value;
    }

    public int getWeight() {
        return switch (this) {
            case EXTRACTING_AUDIO -> 20;
            case TRANSCRIBING -> 50;
            case SUMMARIZING -> 20;
            case EXPORTING -> 10;
            default -> 0;
        };
    }

    public int getCumulativeStart() {
        return switch (this) {
            case EXTRACTING_AUDIO -> 0;
            case TRANSCRIBING -> 20;
            case SUMMARIZING -> 70;
            case EXPORTING -> 90;
            case COMPLETED -> 100;
            default -> 0;
        };
    }
}
