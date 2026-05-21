package com.gitguild.backend.common;

import com.fasterxml.jackson.annotation.JsonInclude;
import java.time.OffsetDateTime;

/** 统一 HTTP 响应体。所有接口返回值均包装为此类型。 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public class ApiResponse<T> {

    private final String code;
    private final String message;
    private final T data;
    private final String timestamp;
    private final String traceId;

    private ApiResponse(String code, String message, T data) {
        this.code = code;
        this.message = message;
        this.data = data;
        this.timestamp = OffsetDateTime.now().toString();
        this.traceId = null;
    }

    public static <T> ApiResponse<T> success(T data) {
        return new ApiResponse<>("SUCCESS", "操作成功", data);
    }

    public static ApiResponse<Void> success() {
        return new ApiResponse<>("SUCCESS", "操作成功", null);
    }

    public static <T> ApiResponse<T> error(String code, String message) {
        return new ApiResponse<>(code, message, null);
    }

    public String getCode()      { return code; }
    public String getMessage()   { return message; }
    public T getData()           { return data; }
    public String getTimestamp() { return timestamp; }
    public String getTraceId()   { return traceId; }
}
