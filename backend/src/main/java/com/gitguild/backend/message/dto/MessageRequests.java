package com.gitguild.backend.message.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public final class MessageRequests {

    private MessageRequests() {
    }

    public record SendMessageRequest(
            @NotBlank(message = "信笺内容不能为空")
            @Size(max = 1000, message = "信笺内容不能超过 1000 字")
            String content) {
    }
}
