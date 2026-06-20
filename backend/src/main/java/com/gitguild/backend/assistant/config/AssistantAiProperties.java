package com.gitguild.backend.assistant.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "assistant.ai")
public record AssistantAiProperties(
        boolean enabled,
        String provider,
        String baseUrl,
        String apiKey,
        String model,
        int timeoutMs,
        int maxPromptChars) {

    public AssistantAiProperties {
        if (provider == null || provider.isBlank()) {
            provider = "openai-compatible";
        }
        if (timeoutMs <= 0) {
            timeoutMs = 8000;
        }
        if (maxPromptChars <= 0) {
            maxPromptChars = 4000;
        }
    }

    public boolean isReady() {
        return enabled && hasText(baseUrl) && hasText(apiKey) && hasText(model);
    }

    private boolean hasText(String value) {
        return value != null && !value.isBlank();
    }
}
