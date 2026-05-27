package com.gitguild.backend.common;

import com.fasterxml.jackson.annotation.JsonInclude;
import java.time.OffsetDateTime;
import java.util.UUID;

/** 统一 HTTP 响应体。所有接口返回值均包装为此类型。 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public class ApiResponse<T> {

    private final String code;
    private final String message;
    private final T data;
    private final String details;
    private final String timestamp;
    private final String traceId;

    private ApiResponse(String code, String message, T data, String details) {
        this.code = code;
        this.message = message;
        this.data = data;
        this.details = details;
        this.timestamp = OffsetDateTime.now().toString();
        this.traceId = "req-" + UUID.randomUUID();
    }

    public static <T> ApiResponse<T> success(T data) {
        return success("OK", data);
    }

    public static <T> ApiResponse<T> success(String message, T data) {
        return new ApiResponse<>("SUCCESS", message, data, null);
    }

    public static ApiResponse<Void> success() {
        return new ApiResponse<>("SUCCESS", "OK", null, null);
    }

    public static ApiResponse<Void> error(String code, String message) {
        return error(code, message, null);
    }

    public static ApiResponse<Void> error(String code, String message, String details) {
        return new ApiResponse<>(code, message, null, details);
    }

    public String getCode()      { return code; }
    public String getMessage()   { return message; }
    public T getData()           { return data; }
    public String getDetails()   { return details; }
    public String getTimestamp() { return timestamp; }
    public String getTraceId()   { return traceId; }
}
