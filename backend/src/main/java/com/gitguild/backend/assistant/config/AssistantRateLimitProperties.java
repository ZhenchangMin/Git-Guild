package com.gitguild.backend.assistant.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "assistant.rate-limit")
public record AssistantRateLimitProperties(
        boolean enabled,
        int maxRequests,
        int windowSeconds) {

    public AssistantRateLimitProperties {
        if (maxRequests <= 0) {
            maxRequests = 10;
        }
        if (windowSeconds <= 0) {
            windowSeconds = 60;
        }
    }
}
