package com.gitguild.backend.assistant.service;

import com.gitguild.backend.assistant.config.AssistantAiProperties;
import java.util.Optional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Service;

@Primary
@Service
public class AssistantAnswerOrchestrator implements AssistantAnswerService {

    private static final Logger log = LoggerFactory.getLogger(AssistantAnswerOrchestrator.class);

    private final AssistantAiProperties aiProperties;
    private final AssistantFallbackService fallbackService;
    private final Optional<AssistantAiClient> aiClient;

    public AssistantAnswerOrchestrator(
            AssistantAiProperties aiProperties,
            AssistantFallbackService fallbackService,
            Optional<AssistantAiClient> aiClient) {
        this.aiProperties = aiProperties;
        this.fallbackService = fallbackService;
        this.aiClient = aiClient;
    }

    @Override
    public AssistantAnswerResult answer(AssistantChatContext context) {
        if (!aiProperties.isReady() || aiClient.isEmpty()) {
            return fallbackService.answer(context);
        }

        try {
            return aiClient
                    .flatMap(client -> client.tryAnswer(context))
                    .orElseGet(() -> fallbackService.answer(context));
        } catch (RuntimeException ex) {
            log.warn(
                    "Assistant AI provider failed; falling back to FAQ. provider={}, errorType={}, message={}",
                    aiProperties.provider(),
                    ex.getClass().getSimpleName(),
                    ex.getMessage());
            return fallbackService.answer(context);
        }
    }

    public AssistantAnswerResult answer(String message, String page) {
        return answer(AssistantChatContext.anonymous(message, page));
    }
}
