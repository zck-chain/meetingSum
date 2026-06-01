package com.meetingsum.pipeline;

import com.meetingsum.model.enums.ErrorCode;

/**
 * 管道异常 — 携带 ErrorCode 的自定义运行时异常。
 * PipelineService 中每阶段独立捕获，提取错误码后写入数据库并通过 WebSocket 推送。
 */
public class PipelineException extends RuntimeException {

    private final ErrorCode errorCode;

    public PipelineException(ErrorCode errorCode, String detail) {
        super(detail);
        this.errorCode = errorCode;
    }

    public PipelineException(ErrorCode errorCode, String detail, Throwable cause) {
        super(detail, cause);
        this.errorCode = errorCode;
    }

    public ErrorCode getErrorCode() {
        return errorCode;
    }
}
