package com.meetingsum.pipeline;

import com.meetingsum.model.dto.SummaryData;
import com.meetingsum.model.entity.Meeting;
import com.meetingsum.pipeline.TranscriberService.WhisperResult;
import com.meetingsum.pipeline.TranscriberService.WhisperResult.Segment;
import org.apache.poi.xwpf.usermodel.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Component
public class ExportService {

    private static final Logger log = LoggerFactory.getLogger(ExportService.class);

    private static final String MD_TEMPLATE = """
            # %s

            ## 基本信息
            - 日期：%s
            - 时长：%s
            - 原始文件：%s

            ## 会议摘要
            %s

            ## 关键讨论点
            %s

            ## 决策记录
            %s

            ## 行动项
            %s

            ## 标签
            %s

            ---

            > 由 MeetingSum 自动生成
            """;

    public String generateMarkdown(Meeting meeting, SummaryData summary, String outputDir) throws IOException {
        Path outputPath = Path.of(outputDir).resolve(meeting.getId() + "_summary.md");
        outputPath.getParent().toFile().mkdirs();

        String dateStr = meeting.getCreatedAt() != null
                ? meeting.getCreatedAt().toLocalDate().toString()
                : LocalDateTime.now().toLocalDate().toString();

        String filename = meeting.getOriginalFile() != null
                ? Path.of(meeting.getOriginalFile()).getFileName().toString()
                : "unknown";

        String content = String.format(MD_TEMPLATE,
                summary.title() != null && !summary.title().isBlank() ? summary.title() : (meeting.getTitle() != null ? meeting.getTitle() : "未命名会议"),
                dateStr,
                fmtDuration(meeting.getDurationSeconds()),
                filename,
                summary.summary(),
                formatKeyPoints(summary.keyPoints()),
                formatDecisions(summary.decisions()),
                formatActionItems(summary.actionItems()),
                summary.tags() != null && !summary.tags().isEmpty() ? String.join(", ", summary.tags()) : "_无_"
        );

        Files.writeString(outputPath, content, StandardCharsets.UTF_8);
        return outputPath.toString();
    }

    public String generateTranscriptMarkdown(Meeting meeting, WhisperResult transcription, String outputDir) throws IOException {
        Path outputPath = Path.of(outputDir).resolve(meeting.getId() + "_transcript.md");
        outputPath.getParent().toFile().mkdirs();

        StringBuilder sb = new StringBuilder();
        sb.append("# 完整转录文本 — ").append(meeting.getTitle() != null ? meeting.getTitle() : "未命名会议").append("\n\n");
        sb.append("日期：").append(meeting.getCreatedAt() != null ? meeting.getCreatedAt().toLocalDate().toString() : "").append("\n");
        sb.append("时长：").append(fmtDuration(meeting.getDurationSeconds())).append("\n\n");
        sb.append("---\n\n");

        for (Segment seg : transcription.segments()) {
            String ts = String.format("[%s -> %s]", fmtTime(seg.start()), fmtTime(seg.end()));
            String prefix = seg.speaker() != null ? "[" + seg.speaker() + "] " : "";
            sb.append(ts).append(" ").append(prefix).append(seg.text()).append("\n");
        }

        sb.append("\n> 由 MeetingSum 自动生成\n");
        Files.writeString(outputPath, sb.toString(), StandardCharsets.UTF_8);
        return outputPath.toString();
    }

    public String generateDocx(Meeting meeting, SummaryData summary, String outputDir) throws IOException {
        Path outputPath = Path.of(outputDir).resolve(meeting.getId() + "_summary.docx");
        outputPath.getParent().toFile().mkdirs();

        try (XWPFDocument doc = new XWPFDocument()) {
            String title = summary.title() != null && !summary.title().isBlank()
                    ? summary.title()
                    : (meeting.getTitle() != null ? meeting.getTitle() : "未命名会议");

            doc.createParagraph().createRun().setText(title);
            doc.createParagraph().createRun().setBold(true);

            addHeading(doc, "基本信息", 2);
            String dateStr = meeting.getCreatedAt() != null
                    ? meeting.getCreatedAt().toLocalDate().toString()
                    : "";
            doc.createParagraph().createRun().setText("日期：" + dateStr);
            doc.createParagraph().createRun().setText("时长：" + fmtDuration(meeting.getDurationSeconds()));
            String filename = meeting.getOriginalFile() != null
                    ? Path.of(meeting.getOriginalFile()).getFileName().toString()
                    : "未知";
            doc.createParagraph().createRun().setText("原始文件：" + filename);

            addHeading(doc, "会议摘要", 2);
            doc.createParagraph().createRun().setText(summary.summary() != null ? summary.summary() : "");

            addHeading(doc, "关键讨论点", 2);
            if (summary.keyPoints() != null) {
                for (int i = 0; i < summary.keyPoints().size(); i++) {
                    var kp = summary.keyPoints().get(i);
                    String prefix = "high".equals(kp.importance()) ? "【重要】" : "";
                    XWPFParagraph p = doc.createParagraph();
                    p.createRun().setText((i + 1) + ". " + prefix + (kp.topic() != null ? kp.topic() : "议题"));
                    doc.createParagraph().createRun().setText(kp.content() != null ? kp.content() : "");
                }
            }

            addHeading(doc, "决策记录", 2);
            if (summary.decisions() != null) {
                for (var d : summary.decisions()) {
                    String proposer = d.proposer() != null ? d.proposer() : "未指定";
                    doc.createParagraph().createRun().setText(
                            (d.content() != null ? d.content() : "") + " — 提出人：" + proposer
                    );
                }
            }

            addHeading(doc, "行动项", 2);
            if (summary.actionItems() != null) {
                for (var item : summary.actionItems()) {
                    String assignee = item.assignee() != null ? item.assignee() : "待指定";
                    String deadline = item.deadline() != null ? item.deadline() : "待定";
                    doc.createParagraph().createRun().setText(
                            (item.content() != null ? item.content() : "") + " — 负责人：" + assignee + " — 截止日期：" + deadline
                    );
                }
            }

            if (summary.tags() != null && !summary.tags().isEmpty()) {
                addHeading(doc, "标签", 2);
                doc.createParagraph().createRun().setText(String.join(", ", summary.tags()));
            }

            doc.createParagraph();
            doc.createParagraph().createRun().setText("由 MeetingSum 自动生成");

            try (FileOutputStream fos = new FileOutputStream(outputPath.toFile())) {
                doc.write(fos);
            }
        }

        return outputPath.toString();
    }

    public Map<String, String> exportAll(Meeting meeting, SummaryData summary, WhisperResult transcription, String outputDir) throws IOException {
        Map<String, String> results = new LinkedHashMap<>();

        String mdPath = generateMarkdown(meeting, summary, outputDir);
        results.put("md", mdPath);

        String transcriptPath = generateTranscriptMarkdown(meeting, transcription, outputDir);
        results.put("transcript", transcriptPath);

        try {
            String docxPath = generateDocx(meeting, summary, outputDir);
            results.put("docx", docxPath);
        } catch (Exception e) {
            log.warn("DOCX generation failed, continuing without it", e);
        }

        return results;
    }

    private void addHeading(XWPFDocument doc, String text, int level) {
        XWPFParagraph p = doc.createParagraph();
        XWPFRun run = p.createRun();
        run.setText(text);
        run.setBold(true);
        run.setFontSize(level == 1 ? 18 : 14);
    }

    private String fmtDuration(Integer seconds) {
        if (seconds == null) return "--:--:--";
        int h = seconds / 3600;
        int m = (seconds % 3600) / 60;
        int s = seconds % 60;
        return String.format("%02d:%02d:%02d", h, m, s);
    }

    private String fmtTime(double seconds) {
        int h = (int) (seconds / 3600);
        int m = (int) ((seconds % 3600) / 60);
        int s = (int) (seconds % 60);
        return String.format("%02d:%02d:%02d", h, m, s);
    }

    private String formatKeyPoints(List<SummaryData.KeyPoint> keyPoints) {
        if (keyPoints == null || keyPoints.isEmpty()) return "_无_";
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < keyPoints.size(); i++) {
            var kp = keyPoints.get(i);
            String badge = "high".equals(kp.importance()) ? "【重要】" : "";
            sb.append(i + 1).append(". **").append(kp.topic() != null ? kp.topic() : "议题").append("** ").append(badge).append("\n");
            sb.append("   ").append(kp.content() != null ? kp.content() : "").append("\n");
        }
        return sb.toString();
    }

    private String formatDecisions(List<SummaryData.Decision> decisions) {
        if (decisions == null || decisions.isEmpty()) return "_无_";
        StringBuilder sb = new StringBuilder();
        for (var d : decisions) {
            String proposer = d.proposer() != null ? d.proposer() : "未指定";
            sb.append("- [ ] ").append(d.content() != null ? d.content() : "").append(" — 提出人：").append(proposer).append("\n");
        }
        return sb.toString();
    }

    private String formatActionItems(List<SummaryData.ActionItem> items) {
        if (items == null || items.isEmpty()) return "_无_";
        StringBuilder sb = new StringBuilder();
        for (var item : items) {
            String assignee = item.assignee() != null ? item.assignee() : "待指定";
            String deadline = item.deadline() != null ? item.deadline() : "待定";
            sb.append("- [ ] ").append(item.content() != null ? item.content() : "")
                    .append(" — 负责人：").append(assignee)
                    .append(" — 截止日期：").append(deadline).append("\n");
        }
        return sb.toString();
    }
}
