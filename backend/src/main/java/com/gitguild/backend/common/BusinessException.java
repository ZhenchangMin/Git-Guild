package com.gitguild.backend.common;

import org.springframework.http.HttpStatus;

/** 带业务错误码和 HTTP 状态码的异常，用于对齐 API 规范中的失败响应。 */
public class BusinessException extends RuntimeException {

    private final String code;
    private final HttpStatus status;
    private final String details;

    public BusinessException(String code, HttpStatus status, String message) {
        this(code, status, message, null);
    }

    public BusinessException(String code, HttpStatus status, String message, String details) {
        super(message);
        this.code = code;
        this.status = status;
        this.details = details;
    }

    public String getCode() {
        return code;
    }

    public HttpStatus getStatus() {
        return status;
    }

    public String getDetails() {
        return details;
    }
}
