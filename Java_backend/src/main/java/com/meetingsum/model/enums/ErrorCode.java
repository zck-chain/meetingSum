package com.meetingsum.model.enums;

import com.fasterxml.jackson.annotation.JsonValue;

/**
 * 管道错误码 — 按阶段前缀分组，支持前端差异化展示。
 */
public enum ErrorCode {

    // ---- 音频提取阶段 ----
    AUDIO_EXTRACTION_FAILED("AUDIO_EXTRACTION_FAILED", "音频提取失败，文件可能已损坏"),
    AUDIO_EXTRACTION_TIMEOUT("AUDIO_EXTRACTION_TIMEOUT", "音频提取超时"),

    // ---- 语音转写阶段 ----
    TRANSCRIPTION_FAILED("TRANSCRIPTION_FAILED", "语音转文字失败"),
    TRANSCRIPTION_TIMEOUT("TRANSCRIPTION_TIMEOUT", "语音转文字超时"),
    TRANSCRIPTION_INVALID_OUTPUT("TRANSCRIPTION_INVALID_OUTPUT", "转写输出格式异常"),

    // ---- 说话人分离 ----
    DIARIZATION_FAILED("DIARIZATION_FAILED", "说话人分离失败"),

    // ---- LLM 摘要阶段 ----
    LLM_TIMEOUT("LLM_TIMEOUT", "LLM 请求超时，请稍后重试"),
    LLM_INVALID_RESPONSE("LLM_INVALID_RESPONSE", "LLM 返回格式异常"),
    LLM_API_ERROR("LLM_API_ERROR", "LLM API 调用失败"),

    // ---- 导出阶段 ----
    EXPORT_FAILED("EXPORT_FAILED", "文档导出失败"),

    // ---- 全局 ----
    PIPELINE_TIMEOUT("PIPELINE_TIMEOUT", "处理超时"),
    UNKNOWN_ERROR("UNKNOWN_ERROR", "未知错误");

    private final String code;
    private final String defaultMessage;

    ErrorCode(String code, String defaultMessage) {
        this.code = code;
        this.defaultMessage = defaultMessage;
    }

    @JsonValue
    public String getCode() {
        return code;
    }

    public String getDefaultMessage() {
        return defaultMessage;
    }
}
