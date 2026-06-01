package com.meetingsum.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.meetingsum.config.AppProperties;
import com.meetingsum.model.dto.MeetingDetailResponse;
import com.meetingsum.model.dto.MeetingListItem;
import com.meetingsum.model.dto.MeetingListResponse;
import com.meetingsum.model.dto.SummaryData;
import com.meetingsum.model.entity.Meeting;
import com.meetingsum.model.enums.MeetingStatus;
import com.meetingsum.repository.MeetingRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.format.DateTimeFormatter;
import java.util.List;

@Service
public class MeetingService {

    private static final Logger log = LoggerFactory.getLogger(MeetingService.class);
    private static final DateTimeFormatter ISO_FORMAT = DateTimeFormatter.ISO_LOCAL_DATE_TIME;

    private final MeetingRepository meetingRepository;
    private final FileStorageService fileStorageService;
    private final ObjectMapper objectMapper;

    public MeetingService(MeetingRepository meetingRepository, FileStorageService fileStorageService, ObjectMapper objectMapper) {
        this.meetingRepository = meetingRepository;
        this.fileStorageService = fileStorageService;
        this.objectMapper = objectMapper;
    }

    @Transactional
    public Meeting createMeeting(String title, String filePath, String originalFormat, Long fileSizeBytes) {
        Meeting meeting = new Meeting();
        meeting.setTitle(title);
        meeting.setOriginalFile(filePath);
        meeting.setOriginalFormat(originalFormat);
        meeting.setFileSizeBytes(fileSizeBytes);
        meeting.setStatus(MeetingStatus.PENDING);
        return meetingRepository.save(meeting);
    }

    @Transactional(readOnly = true)
    public Meeting findByIdOrThrow(String meetingId) {
        return meetingRepository.findById(meetingId)
                .orElseThrow(() -> new MeetingNotFoundException(meetingId));
    }

    @Transactional(readOnly = true)
    public MeetingDetailResponse getMeeting(String meetingId) {
        Meeting meeting = findByIdOrThrow(meetingId);
        return toDetailResponse(meeting);
    }

    @Transactional(readOnly = true)
    public MeetingListResponse listMeetings(int page, int pageSize, String status, String search) {
        Pageable pageable = PageRequest.of(page - 1, pageSize, Sort.by(Sort.Direction.DESC, "createdAt"));

        Page<Meeting> result;
        if (status != null && !status.isBlank() && search != null && !search.isBlank()) {
            result = meetingRepository.findByStatusAndTitleContainingIgnoreCase(status, search, pageable);
        } else if (status != null && !status.isBlank()) {
            result = meetingRepository.findByStatus(status, pageable);
        } else if (search != null && !search.isBlank()) {
            result = meetingRepository.findByTitleContainingIgnoreCase(search, pageable);
        } else {
            result = meetingRepository.findAll(pageable);
        }

        List<MeetingListItem> items = result.getContent().stream()
                .map(this::toListItem)
                .toList();

        return new MeetingListResponse(
                items,
                result.getTotalElements(),
                result.getNumber() + 1,
                result.getSize(),
                result.getTotalPages()
        );
    }

    @Transactional
    public void deleteMeeting(String meetingId) {
        Meeting meeting = findByIdOrThrow(meetingId);
        String originalFile = meeting.getOriginalFile();
        fileStorageService.deleteOriginalFile(originalFile);
        fileStorageService.deleteMeetingFiles(meetingId);
        meetingRepository.delete(meeting);
    }

    @Transactional
    public void updateStatus(String meetingId, MeetingStatus status) {
        Meeting meeting = findByIdOrThrow(meetingId);
        meeting.setStatus(status);
        meetingRepository.save(meeting);
    }

    @Transactional
    public void updateTranscript(String meetingId, String transcriptText) {
        Meeting meeting = findByIdOrThrow(meetingId);
        meeting.setTranscriptText(transcriptText);
        meetingRepository.save(meeting);
    }

    @Transactional
    public void updateSummary(String meetingId, SummaryData summary) {
        Meeting meeting = findByIdOrThrow(meetingId);
        try {
            meeting.setSummaryJson(objectMapper.writeValueAsString(summary));
        } catch (JsonProcessingException e) {
            throw new RuntimeException("Failed to serialize summary JSON", e);
        }
        meetingRepository.save(meeting);
    }

    @Transactional
    public void updateDuration(String meetingId, Integer durationSeconds) {
        Meeting meeting = findByIdOrThrow(meetingId);
        meeting.setDurationSeconds(durationSeconds);
        meetingRepository.save(meeting);
    }

    @Transactional
    public void updateSummaryTemplate(String meetingId, String template) {
        Meeting meeting = findByIdOrThrow(meetingId);
        meeting.setCustomSummaryTemplate(template);
        meetingRepository.save(meeting);
    }

    private MeetingDetailResponse toDetailResponse(Meeting m) {
        SummaryData summaryJson = null;
        if (m.getSummaryJson() != null && !m.getSummaryJson().isBlank()) {
            try {
                summaryJson = objectMapper.readValue(m.getSummaryJson(), SummaryData.class);
            } catch (JsonProcessingException e) {
                log.warn("Failed to parse summary_json for meeting {}", m.getId());
            }
        }

        return new MeetingDetailResponse(
                m.getId(),
                m.getTitle(),
                m.getOriginalFile(),
                m.getOriginalFormat(),
                m.getDurationSeconds(),
                m.getFileSizeBytes(),
                m.getStatus() != null ? m.getStatus().getValue() : null,
                summaryJson,
                m.getTranscriptText(),
                m.getErrorMessage(),
                m.getCreatedAt() != null ? m.getCreatedAt().toString() : null,
                m.getUpdatedAt() != null ? m.getUpdatedAt().toString() : null
        );
    }

    private MeetingListItem toListItem(Meeting m) {
        return new MeetingListItem(
                m.getId(),
                m.getTitle(),
                m.getOriginalFormat(),
                m.getDurationSeconds(),
                m.getFileSizeBytes(),
                m.getStatus() != null ? m.getStatus().getValue() : null,
                m.getCreatedAt() != null ? m.getCreatedAt().toString() : null,
                m.getUpdatedAt() != null ? m.getUpdatedAt().toString() : null
        );
    }

    public static class MeetingNotFoundException extends RuntimeException {
        public MeetingNotFoundException(String id) {
            super("Meeting not found: " + id);
        }
    }
}
