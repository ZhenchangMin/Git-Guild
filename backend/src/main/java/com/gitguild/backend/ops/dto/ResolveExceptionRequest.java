package com.gitguild.backend.ops.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * 管理员处置异常的请求体：处理动作 + 必填处理说明（写入异常日志）。
 */
public record ResolveExceptionRequest(
        @NotBlank(message = "处理动作为必填项") String action,
        @NotBlank(message = "处理说明为必填项")
        @Size(max = 1000, message = "处理说明不能超过 1000 字") String comment) {
}
