package com.meetingsum.service;

import com.meetingsum.config.AppProperties;
import com.meetingsum.model.dto.SummaryData;
import com.meetingsum.model.entity.Meeting;
import com.meetingsum.model.enums.MeetingStatus;
import com.meetingsum.model.enums.TaskStage;
import com.meetingsum.model.enums.TaskStatus;
import com.meetingsum.pipeline.*;
import com.meetingsum.pipeline.TranscriberService.WhisperResult;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

@Service
public class PipelineService {

    private static final Logger log = LoggerFactory.getLogger(PipelineService.class);

    private final MeetingService meetingService;
    private final TaskService taskService;
    private final FileStorageService fileStorageService;
    private final AppProperties props;
    private final AudioExtractor audioExtractor;
    private final TranscriberService transcriberService;
    private final Diarizer diarizer;
    private final SummarizerService summarizerService;
    private final ExportService exportService;

    public PipelineService(MeetingService meetingService, TaskService taskService,
                           FileStorageService fileStorageService, AppProperties props,
                           AudioExtractor audioExtractor, TranscriberService transcriberService,
                           Diarizer diarizer, SummarizerService summarizerService,
                           ExportService exportService) {
        this.meetingService = meetingService;
        this.taskService = taskService;
        this.fileStorageService = fileStorageService;
        this.props = props;
        this.audioExtractor = audioExtractor;
        this.transcriberService = transcriberService;
        this.diarizer = diarizer;
        this.summarizerService = summarizerService;
        this.exportService = exportService;
    }

    @Async("pipelineExecutor")
    @EventListener
    public void onPipelineStart(PipelineStartEvent event) {
        String meetingId = event.meetingId();
        String taskId = event.taskId();
        runPipeline(meetingId, taskId);
    }

    public void runPipeline(String meetingId, String taskId) {
        log.info("Starting pipeline for meeting {} task {}", meetingId, taskId);

        try {
            // Mark processing
            meetingService.updateStatus(meetingId, MeetingStatus.PROCESSING);
            taskService.updateProgress(taskId, TaskStage.EXTRACTING_AUDIO, 0, TaskStatus.PROCESSING);

            Meeting meeting = meetingService.findByIdOrThrow(meetingId);
            String inputPath = meeting.getOriginalFile();
            if (inputPath == null || !Files.exists(Path.of(inputPath))) {
                throw new IllegalStateException("Input file not found: " + inputPath);
            }

            String meetingDir = Path.of(props.getOutputDir(), meetingId).toString();
            Files.createDirectories(Path.of(meetingDir));

            // Stage 1: Audio extraction
            log.info("Stage 1/4: Extracting audio");
            emitProgress(taskId, TaskStage.EXTRACTING_AUDIO, 0);
            String audioPath = audioExtractor.extractAudio(inputPath, meetingDir);
            double duration = audioExtractor.getMediaDuration(inputPath);
            meetingService.updateDuration(meetingId, (int) duration);
            emitProgress(taskId, TaskStage.EXTRACTING_AUDIO, 100);

            // Stage 2: Transcription
            log.info("Stage 2/4: Transcribing");
            emitProgress(taskId, TaskStage.TRANSCRIBING, 0);
            WhisperResult transcription = transcriberService.transcribe(audioPath);
            List<TranscriberService.WhisperResult.Segment> diarizedSegments = diarizer.assignSpeakers(transcription.segments());

            // Rebuild transcription with diarized segments
            StringBuilder fullTextBuilder = new StringBuilder();
            for (var seg : diarizedSegments) {
                if (!fullTextBuilder.isEmpty()) fullTextBuilder.append(" ");
                fullTextBuilder.append(seg.text());
            }
            meetingService.updateTranscript(meetingId, fullTextBuilder.toString());
            emitProgress(taskId, TaskStage.TRANSCRIBING, 100);

            // Stage 3: Summarization
            log.info("Stage 3/4: Summarizing");
            emitProgress(taskId, TaskStage.SUMMARIZING, 0);
            SummaryData summary = summarizerService.summarize(fullTextBuilder.toString());
            meetingService.updateSummary(meetingId, summary);
            emitProgress(taskId, TaskStage.SUMMARIZING, 100);

            // Stage 4: Export
            log.info("Stage 4/4: Exporting documents");
            emitProgress(taskId, TaskStage.EXPORTING, 0);
            exportService.exportAll(meeting, summary, transcription, meetingDir);
            emitProgress(taskId, TaskStage.EXPORTING, 100);

            // Mark completed
            taskService.markCompleted(taskId);
            meetingService.updateStatus(meetingId, MeetingStatus.COMPLETED);
            log.info("Pipeline completed for meeting {}", meetingId);

        } catch (Exception e) {
            log.error("Pipeline failed for meeting {}: {}", meetingId, e.getMessage());
            taskService.markFailed(taskId, e.getMessage());
            meetingService.updateStatus(meetingId, MeetingStatus.FAILED);
            try {
                Meeting m = meetingService.findByIdOrThrow(meetingId);
                m.setErrorMessage(e.getMessage());
            } catch (Exception ignored) {}
        }
    }

    private void emitProgress(String taskId, TaskStage stage, int stagePercent) {
        int cumulative = stage.getCumulativeStart();
        int weight = stage.getWeight();
        int totalProgress = cumulative + (stagePercent * weight / 100);
        totalProgress = Math.min(totalProgress, 100);

        TaskStatus status = TaskStatus.PROCESSING;
        if (stage == TaskStage.COMPLETED) {
            status = TaskStatus.COMPLETED;
        }

        taskService.updateProgress(taskId, stage, totalProgress, status);
    }

    public record PipelineStartEvent(String meetingId, String taskId) {}
}
