package com.meetingsum.model.entity;

import com.meetingsum.model.enums.MeetingStatus;
import com.meetingsum.util.IdGenerator;
import jakarta.persistence.*;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "meetings")
public class Meeting {

    @Id
    @Column(length = 32)
    private String id = IdGenerator.generateId();

    @Column(length = 255)
    private String title;

    @Column(name = "original_file", length = 500)
    private String originalFile;

    @Column(name = "original_format", length = 20)
    private String originalFormat;

    @Column(name = "duration_seconds")
    private Integer durationSeconds;

    @Column(name = "file_size_bytes")
    private Long fileSizeBytes;

    @Enumerated(EnumType.STRING)
    @Column(length = 20)
    private MeetingStatus status = MeetingStatus.PENDING;

    @Column(name = "transcript_text", columnDefinition = "CLOB")
    private String transcriptText;

    @Column(name = "summary_json", columnDefinition = "CLOB")
    private String summaryJson;

    @Column(name = "error_message", columnDefinition = "CLOB")
    private String errorMessage;

    @Column(name = "custom_summary_template", columnDefinition = "CLOB")
    private String customSummaryTemplate;

    @Column(name = "created_at")
    private LocalDateTime createdAt = LocalDateTime.now();

    @Column(name = "updated_at")
    private LocalDateTime updatedAt = LocalDateTime.now();

    @OneToMany(mappedBy = "meeting", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<Task> tasks = new ArrayList<>();

    @PreUpdate
    void onUpdate() {
        this.updatedAt = LocalDateTime.now();
    }

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }

    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }

    public String getOriginalFile() { return originalFile; }
    public void setOriginalFile(String originalFile) { this.originalFile = originalFile; }

    public String getOriginalFormat() { return originalFormat; }
    public void setOriginalFormat(String originalFormat) { this.originalFormat = originalFormat; }

    public Integer getDurationSeconds() { return durationSeconds; }
    public void setDurationSeconds(Integer durationSeconds) { this.durationSeconds = durationSeconds; }

    public Long getFileSizeBytes() { return fileSizeBytes; }
    public void setFileSizeBytes(Long fileSizeBytes) { this.fileSizeBytes = fileSizeBytes; }

    public MeetingStatus getStatus() { return status; }
    public void setStatus(MeetingStatus status) { this.status = status; }

    public String getTranscriptText() { return transcriptText; }
    public void setTranscriptText(String transcriptText) { this.transcriptText = transcriptText; }

    public String getSummaryJson() { return summaryJson; }
    public void setSummaryJson(String summaryJson) { this.summaryJson = summaryJson; }

    public String getErrorMessage() { return errorMessage; }
    public void setErrorMessage(String errorMessage) { this.errorMessage = errorMessage; }

    public String getCustomSummaryTemplate() { return customSummaryTemplate; }
    public void setCustomSummaryTemplate(String customSummaryTemplate) { this.customSummaryTemplate = customSummaryTemplate; }

    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }

    public LocalDateTime getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(LocalDateTime updatedAt) { this.updatedAt = updatedAt; }

    public List<Task> getTasks() { return tasks; }
    public void setTasks(List<Task> tasks) { this.tasks = tasks; }
}
