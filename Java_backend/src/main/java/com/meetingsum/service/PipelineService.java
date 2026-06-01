package com.meetingsum.service;

import com.meetingsum.config.AppProperties;
import com.meetingsum.model.dto.SummaryData;
import com.meetingsum.model.dto.TaskStatusResponse;
import com.meetingsum.model.entity.Meeting;
import com.meetingsum.model.enums.ErrorCode;
import com.meetingsum.model.enums.MeetingStatus;
import com.meetingsum.model.enums.TaskStage;
import com.meetingsum.model.enums.TaskStatus;
import com.meetingsum.pipeline.*;
import com.meetingsum.pipeline.TranscriberService.WhisperResult;
import com.meetingsum.websocket.WebSocketSessionManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
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
    private final WebSocketSessionManager wsSessionManager;

    public PipelineService(MeetingService meetingService, TaskService taskService,
                           FileStorageService fileStorageService, AppProperties props,
                           AudioExtractor audioExtractor, TranscriberService transcriberService,
                           Diarizer diarizer, SummarizerService summarizerService,
                           ExportService exportService, WebSocketSessionManager wsSessionManager) {
        this.meetingService = meetingService;
        this.taskService = taskService;
        this.fileStorageService = fileStorageService;
        this.props = props;
        this.audioExtractor = audioExtractor;
        this.transcriberService = transcriberService;
        this.diarizer = diarizer;
        this.summarizerService = summarizerService;
        this.exportService = exportService;
        this.wsSessionManager = wsSessionManager;
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
        long pipeStartTime = System.currentTimeMillis();
        int globalTimeoutSec = props.getTimeout().getGlobalSeconds();

        try {
            // Mark processing
            meetingService.updateStatus(meetingId, MeetingStatus.PROCESSING);
            taskService.updateProgress(taskId, TaskStage.EXTRACTING_AUDIO, 0, TaskStatus.PROCESSING);

            Meeting meeting = meetingService.findByIdOrThrow(meetingId);
            String inputPath = meeting.getOriginalFile();
            if (inputPath == null || !Files.exists(Path.of(inputPath))) {
                throw new PipelineException(ErrorCode.AUDIO_EXTRACTION_FAILED,
                        "Input file not found: " + inputPath);
            }

            String meetingDir = Path.of(props.getOutputDir(), meetingId).toString();
            Files.createDirectories(Path.of(meetingDir));

            // ---- Stage 1: Audio extraction ----
            checkGlobalTimeout(pipeStartTime, globalTimeoutSec);
            log.info("Stage 1/4: Extracting audio");
            emitProgress(taskId, TaskStage.EXTRACTING_AUDIO, 0);
            String audioPath;
            try {
                audioPath = audioExtractor.extractAudio(inputPath, meetingDir);
                double duration = audioExtractor.getMediaDuration(inputPath);
                meetingService.updateDuration(meetingId, (int) duration);
            } catch (PipelineException e) {
                throw e;
            } catch (Exception e) {
                throw new PipelineException(ErrorCode.AUDIO_EXTRACTION_FAILED, e.getMessage(), e);
            }
            emitProgress(taskId, TaskStage.EXTRACTING_AUDIO, 100);

            // ---- Stage 2: Transcription ----
            checkGlobalTimeout(pipeStartTime, globalTimeoutSec);
            log.info("Stage 2/4: Transcribing");
            emitProgress(taskId, TaskStage.TRANSCRIBING, 0);
            WhisperResult transcription;
            List<TranscriberService.WhisperResult.Segment> diarizedSegments;
            try {
                transcription = transcriberService.transcribe(audioPath);

                // Diarization — try pyannote (with audio path) first, fall back to heuristic
                try {
                    diarizedSegments = diarizer.assignSpeakersWithAudio(transcription.segments(), audioPath);
                } catch (Exception e) {
                    log.warn("Diarization failed, using default speaker assignment: {}", e.getMessage());
                    diarizedSegments = diarizer.assignSpeakers(transcription.segments());
                }

                // Rebuild transcription text from diarized segments
                StringBuilder fullTextBuilder = new StringBuilder();
                for (var seg : diarizedSegments) {
                    if (!fullTextBuilder.isEmpty()) fullTextBuilder.append(" ");
                    fullTextBuilder.append(seg.text());
                }
                meetingService.updateTranscript(meetingId, fixEncoding(fullTextBuilder.toString()));
            } catch (PipelineException e) {
                throw e;
            } catch (Exception e) {
                throw new PipelineException(ErrorCode.TRANSCRIPTION_FAILED, e.getMessage(), e);
            }
            emitProgress(taskId, TaskStage.TRANSCRIBING, 100);

            // Rebuild full text again for summarization (same logic)
            StringBuilder fullTextBuilder2 = new StringBuilder();
            for (var seg : diarizedSegments) {
                if (!fullTextBuilder2.isEmpty()) fullTextBuilder2.append(" ");
                fullTextBuilder2.append(seg.text());
            }
            String fullText = fixEncoding(fullTextBuilder2.toString());

            // ---- Stage 3: Summarization ----
            checkGlobalTimeout(pipeStartTime, globalTimeoutSec);
            log.info("Stage 3/4: Summarizing");
            emitProgress(taskId, TaskStage.SUMMARIZING, 0);
            SummaryData summary;
            try {
                // Refresh meeting to get latest data including custom template
                Meeting refreshedMeeting = meetingService.findByIdOrThrow(meetingId);
                String customTemplate = refreshedMeeting.getCustomSummaryTemplate();
                summary = summarizerService.summarize(fullText, customTemplate);
                meetingService.updateSummary(meetingId, summary);
            } catch (PipelineException e) {
                throw e;
            } catch (Exception e) {
                throw new PipelineException(ErrorCode.LLM_API_ERROR, e.getMessage(), e);
            }
            emitProgress(taskId, TaskStage.SUMMARIZING, 100);

            // ---- Stage 4: Export ----
            checkGlobalTimeout(pipeStartTime, globalTimeoutSec);
            log.info("Stage 4/4: Exporting documents");
            emitProgress(taskId, TaskStage.EXPORTING, 0);
            try {
                exportService.exportAll(meeting, summary, transcription, meetingDir);
            } catch (Exception e) {
                throw new PipelineException(ErrorCode.EXPORT_FAILED, e.getMessage(), e);
            }
            emitProgress(taskId, TaskStage.EXPORTING, 100);

            // ---- Success ----
            taskService.markCompleted(taskId);
            meetingService.updateStatus(meetingId, MeetingStatus.COMPLETED);

            // Send WebSocket completed event
            try {
                com.meetingsum.model.entity.Task completedTask = taskService.findByIdOrThrow(taskId);
                TaskStatusResponse taskResponse = taskService.getTaskStatus(taskId);
                wsSessionManager.sendCompleted(taskId, taskResponse);
            } catch (Exception wsEx) {
                log.debug("WebSocket completed notification failed (non-critical): {}", wsEx.getMessage());
            }

            long elapsed = (System.currentTimeMillis() - pipeStartTime) / 1000;
            log.info("Pipeline completed for meeting {} in {}s", meetingId, elapsed);

        } catch (PipelineException e) {
            log.error("Pipeline failed for meeting {}: [{}] {}", meetingId, e.getErrorCode().getCode(), e.getMessage());
            taskService.markFailed(taskId, e.getMessage(), e.getErrorCode());
            meetingService.updateStatus(meetingId, MeetingStatus.FAILED);
            try {
                Meeting m = meetingService.findByIdOrThrow(meetingId);
                m.setErrorMessage(e.getMessage());
            } catch (Exception ignored) {}

            // Send WebSocket failed event
            try {
                wsSessionManager.sendFailed(taskId, e.getMessage(), e.getErrorCode().getCode());
            } catch (Exception wsEx) {
                log.debug("WebSocket failed notification error (non-critical): {}", wsEx.getMessage());
            }

        } catch (Exception e) {
            log.error("Pipeline failed unexpectedly for meeting {}: {}", meetingId, e.getMessage());
            taskService.markFailed(taskId, e.getMessage(), ErrorCode.UNKNOWN_ERROR);
            meetingService.updateStatus(meetingId, MeetingStatus.FAILED);
            try {
                Meeting m = meetingService.findByIdOrThrow(meetingId);
                m.setErrorMessage(e.getMessage());
            } catch (Exception ignored) {}

            // Send WebSocket failed event
            try {
                wsSessionManager.sendFailed(taskId, e.getMessage(), ErrorCode.UNKNOWN_ERROR.getCode());
            } catch (Exception wsEx) {
                log.debug("WebSocket failed notification error (non-critical): {}", wsEx.getMessage());
            }
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

        // Write to DB (source of truth)
        taskService.updateProgress(taskId, stage, totalProgress, status);

        // Push via WebSocket (best-effort, non-blocking)
        try {
            wsSessionManager.sendProgress(taskId, stage, totalProgress);
        } catch (Exception e) {
            log.debug("WebSocket progress push failed (non-critical): {}", e.getMessage());
        }
    }

    /**
     * 检查全局超时。如果总耗时超过配置的全局超时时间，抛出 PIPELINE_TIMEOUT。
     */
    private void checkGlobalTimeout(long startTimeMs, int globalTimeoutSec) {
        long elapsed = (System.currentTimeMillis() - startTimeMs) / 1000;
        if (elapsed > globalTimeoutSec) {
            throw new PipelineException(ErrorCode.PIPELINE_TIMEOUT,
                    "Pipeline exceeded global timeout of " + globalTimeoutSec + "s (elapsed: " + elapsed + "s)");
        }
    }

    /**
     * 修复编码乱码：当 Python 子进程输出 UTF-8 但 Java 用平台默认编码（如 GBK）读取时，
     * 中文字符会出现 U+FFFD 替换字符。此方法尝试反向修复。
     * <p>
     * 策略：检测包含替换字符 → 用平台编码回编字节 → 用 UTF-8 重新解码
     */
    private String fixEncoding(String text) {
        if (text == null || text.isBlank()) return text;
        if (!text.contains("�")) return text; // 没有乱码标记，无需修复

        log.warn("Detected encoding corruption (U+FFFD), attempting fix...");

        // 策略1：用平台默认编码（如 GBK/CP936）回编，再用 UTF-8 解码
        try {
            Charset platformCharset = Charset.defaultCharset();
            byte[] bytes = text.getBytes(platformCharset);
            String fixed = new String(bytes, StandardCharsets.UTF_8);
            if (!fixed.contains("�") && looksLikeValidText(fixed)) {
                log.info("Encoding fixed via {} → UTF-8", platformCharset.name());
                return fixed;
            }
        } catch (Exception ignored) {}

        // 策略2：用 ISO-8859-1 回编（无损），再用 UTF-8 解码
        try {
            byte[] bytes = text.getBytes(StandardCharsets.ISO_8859_1);
            String fixed = new String(bytes, StandardCharsets.UTF_8);
            if (!fixed.contains("�") && looksLikeValidText(fixed)) {
                log.info("Encoding fixed via ISO-8859-1 → UTF-8");
                return fixed;
            }
        } catch (Exception ignored) {}

        log.warn("Unable to fix encoding, returning original text");
        return text;
    }

    /** 简单检测文本是否"看起来正常"（包含中文字符或合理的标点） */
    private boolean looksLikeValidText(String text) {
        if (text == null || text.isBlank()) return false;
        // 检查是否包含中文字符（CJK统一表意文字范围）
        long chineseCount = text.codePoints()
                .filter(cp -> (cp >= 0x4E00 && cp <= 0x9FFF)
                        || (cp >= 0x3400 && cp <= 0x4DBF)
                        || (cp >= 0xF900 && cp <= 0xFAFF))
                .count();
        // 至少要有若干个中文字符才算有效修复
        return chineseCount >= 5 || text.length() > 20;
    }

    public record PipelineStartEvent(String meetingId, String taskId) {}
}
