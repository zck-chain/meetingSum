package com.meetingsum.pipeline;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.meetingsum.config.AppProperties;
import com.meetingsum.model.dto.SummaryData;
import com.meetingsum.model.enums.ErrorCode;
import dev.langchain4j.data.message.SystemMessage;
import dev.langchain4j.data.message.UserMessage;
import dev.langchain4j.model.anthropic.AnthropicChatModel;
import dev.langchain4j.model.openai.OpenAiChatModel;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;

@Component
public class SummarizerService {

    private static final Logger log = LoggerFactory.getLogger(SummarizerService.class);

    private static final String SYSTEM_PROMPT = """
            你是一个专业的会议纪要整理助手。你的任务是根据提供的会议转录文本，生成结构化的会议纪要。请严格按照以下 JSON 格式输出（不要输出其他内容）：

            {
              "title": "会议标题（从内容中推断，不超过30字）",
              "summary": "会议整体摘要，200-300字，概括会议目的、主要讨论内容和结论",
              "key_points": [
                {"topic": "议题名称", "content": "讨论内容摘要", "importance": "high|medium|low"}
              ],
              "decisions": [
                {"content": "决策内容", "proposer": "提出人或null"}
              ],
              "action_items": [
                {"content": "行动项", "assignee": "负责人或null", "deadline": "截止日期或null"}
              ],
              "tags": ["标签1", "标签2"]
            }

            要求：
            - 使用中文输出
            - 关键讨论点按重要性排序
            - 行动项必须包含负责人和截止日期（如果原文提到）
            - 去除口语化的填充词和重复内容
            - 保持客观，不添加原文未提及的信息""";

    private static final int CHUNK_SIZE = 4000;

    private final AppProperties props;
    private final ObjectMapper objectMapper;

    /** 当前调用使用的模板（可能来自三级配置的任意一级） */
    private volatile String currentTemplate;

    public SummarizerService(AppProperties props, ObjectMapper objectMapper) {
        this.props = props;
        this.objectMapper = objectMapper;
    }

    public SummaryData summarize(String transcriptText) {
        return summarize(transcriptText, null);
    }

    /**
     * 使用三级优先级解析 System Prompt：
     * 1. 会议级自定义模板（customTemplate 参数）
     * 2. YAML 全局配置（app.summary-template）
     * 3. 硬编码默认值（SYSTEM_PROMPT）
     */
    public SummaryData summarize(String transcriptText, String customTemplate) {
        if (transcriptText == null || transcriptText.isBlank()) {
            throw new IllegalArgumentException("Transcript text is empty");
        }

        this.currentTemplate = resolveTemplate(customTemplate);
        log.debug("Using summary template (first 80 chars): {}",
                currentTemplate.length() > 80 ? currentTemplate.substring(0, 80) + "..." : currentTemplate);

        List<String> chunks = chunkText(transcriptText);

        if (chunks.size() == 1) {
            String prompt = "请根据以下会议转录文本生成结构化的会议纪要：\n\n" + transcriptText;
            String raw = callLlm(prompt, 4096);
            return parseSummaryJson(raw);
        }

        List<String> chunkSummaries = new ArrayList<>();
        for (int i = 0; i < chunks.size(); i++) {
            String prompt = "请对以下会议转录文本片段（第" + (i + 1) + "/" + chunks.size() + "部分）生成段落摘要：\n\n" + chunks.get(i);
            String raw = callLlm(prompt, 1024);
            chunkSummaries.add(raw.trim());
        }

        StringBuilder combined = new StringBuilder();
        for (int i = 0; i < chunkSummaries.size(); i++) {
            combined.append("第").append(i + 1).append("部分摘要：\n").append(chunkSummaries.get(i)).append("\n");
        }
        String globalPrompt = "以下是多个会议片段的摘要，请基于这些摘要生成最终的完整结构化会议纪要：\n\n" + combined;
        String raw = callLlm(globalPrompt, 4096);
        return parseSummaryJson(raw);
    }

    /**
     * 三级模板解析：会议级 > YAML 配置 > 硬编码默认值
     */
    String resolveTemplate(String customTemplate) {
        if (customTemplate != null && !customTemplate.isBlank()) {
            return customTemplate;
        }
        if (props.getSummaryTemplate() != null && !props.getSummaryTemplate().isBlank()) {
            return props.getSummaryTemplate();
        }
        return SYSTEM_PROMPT;
    }

    String callLlm(String userPrompt, int maxTokens) {
        if ("claude".equalsIgnoreCase(props.getLlmProvider())) {
            return callClaude(userPrompt, maxTokens);
        } else if ("deepseek".equalsIgnoreCase(props.getLlmProvider())) {
            return callDeepSeek(userPrompt, maxTokens);
        } else {
            throw new IllegalArgumentException("Unsupported LLM provider: " + props.getLlmProvider());
        }
    }

    private String callClaude(String userPrompt, int maxTokens) {
        int timeoutSeconds = props.getTimeout().getSummarizationSeconds();
        AnthropicChatModel model = AnthropicChatModel.builder()
                .apiKey(props.getAnthropicApiKey())
                .modelName(props.getAnthropicModel())
                .maxTokens(maxTokens)
                .timeout(Duration.ofSeconds(timeoutSeconds))
                .build();

        try {
            var response = model.generate(
                    new SystemMessage(currentTemplate != null ? currentTemplate : SYSTEM_PROMPT),
                    new UserMessage(userPrompt)
            );
            String text = response.content().text();
            log.debug("Claude response length: {}", text.length());
            return text;
        } catch (Exception e) {
            throw new PipelineException(ErrorCode.LLM_API_ERROR, "Claude API call failed: " + e.getMessage(), e);
        }
    }

    private String callDeepSeek(String userPrompt, int maxTokens) {
        int timeoutSeconds = props.getTimeout().getSummarizationSeconds();
        OpenAiChatModel model = OpenAiChatModel.builder()
                .apiKey(props.getDeepseekApiKey())
                .modelName(props.getDeepseekModel())
                .baseUrl(props.getDeepseekBaseUrl())
                .maxTokens(maxTokens)
                .timeout(Duration.ofSeconds(timeoutSeconds))
                .build();

        try {
            var response = model.generate(
                    new SystemMessage(currentTemplate != null ? currentTemplate : SYSTEM_PROMPT),
                    new UserMessage(userPrompt)
            );
            String text = response.content().text();
            log.debug("DeepSeek response length: {}", text.length());
            return text;
        } catch (Exception e) {
            throw new PipelineException(ErrorCode.LLM_API_ERROR, "DeepSeek API call failed: " + e.getMessage(), e);
        }
    }

    SummaryData parseSummaryJson(String raw) {
        String text = raw.trim();
        // Strip markdown code blocks
        if (text.startsWith("```")) {
            text = text.substring(text.indexOf('\n') + 1);
            if (text.endsWith("```")) {
                text = text.substring(0, text.length() - 3);
            }
            text = text.trim();
        }
        if (text.startsWith("```json")) {
            text = text.substring(7).trim();
            if (text.endsWith("```")) {
                text = text.substring(0, text.length() - 3);
            }
            text = text.trim();
        }

        try {
            return objectMapper.readValue(text, SummaryData.class);
        } catch (JsonProcessingException e) {
            log.error("Failed to parse summary JSON: {}", text);
            throw new PipelineException(ErrorCode.LLM_INVALID_RESPONSE,
                    "Failed to parse LLM response as JSON", e);
        }
    }

    List<String> chunkText(String text) {
        if (text.length() <= CHUNK_SIZE) {
            return List.of(text);
        }
        List<String> chunks = new ArrayList<>();
        int start = 0;
        while (start < text.length()) {
            int end = Math.min(start + CHUNK_SIZE, text.length());
            if (end < text.length()) {
                int brk = text.lastIndexOf('\n', end);
                if (brk == -1 || brk < start + CHUNK_SIZE / 2) {
                    brk = text.lastIndexOf('。', end);
                }
                if (brk == -1 || brk < start + CHUNK_SIZE / 2) {
                    brk = text.lastIndexOf(' ', end);
                }
                if (brk != -1 && brk > start) {
                    end = brk + 1;
                }
            }
            chunks.add(text.substring(start, end));
            start = end;
        }
        return chunks;
    }
}
