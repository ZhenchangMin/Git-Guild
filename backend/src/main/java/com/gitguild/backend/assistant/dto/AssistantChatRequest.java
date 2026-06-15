package com.gitguild.backend.assistant.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public class AssistantChatRequest {

    @NotBlank(message = "message cannot be blank")
    @Size(max = 500, message = "message length cannot exceed 500")
    private String message;

    @Size(max = 64, message = "page length cannot exceed 64")
    private String page;

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public String getPage() {
        return page;
    }

    public void setPage(String page) {
        this.page = page;
    }
}
