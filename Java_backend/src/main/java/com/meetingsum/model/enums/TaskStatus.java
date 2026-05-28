package com.meetingsum.model.enums;

import com.fasterxml.jackson.annotation.JsonValue;

public enum TaskStatus {
    PENDING("pending"),
    PROCESSING("processing"),
    COMPLETED("completed"),
    FAILED("failed");

    private final String value;

    TaskStatus(String value) {
        this.value = value;
    }

    @JsonValue
    public String getValue() {
        return value;
    }
}
