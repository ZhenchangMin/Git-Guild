package com.gitguild.backend.assistant.service;

import static org.assertj.core.api.Assertions.assertThat;

import com.gitguild.backend.assistant.config.AssistantAiProperties;
import com.gitguild.backend.assistant.dto.AssistantAnswerSource;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Test;

class AssistantAnswerOrchestratorTest {

    @Test
    void shouldUseFallbackWhenAiIsDisabled() {
        AssistantAnswerOrchestrator orchestrator = new AssistantAnswerOrchestrator(
                properties(false, "", ""),
                new AssistantFallbackService(),
                Optional.of(context -> Optional.of(aiAnswer())));

        AssistantAnswerResult result = orchestrator.answer("如何接取委托？", "hall");

        assertThat(result.source()).isEqualTo(AssistantAnswerSource.FAQ);
        assertThat(result.answer()).contains("悬赏任务板");
    }

    @Test
    void shouldUseFallbackWhenApiKeyIsMissing() {
        AssistantAnswerOrchestrator orchestrator = new AssistantAnswerOrchestrator(
                properties(true, "", "demo-model"),
                new AssistantFallbackService(),
                Optional.of(context -> Optional.of(aiAnswer())));

        AssistantAnswerResult result = orchestrator.answer("如何提交成果？", "hall");

        assertThat(result.source()).isEqualTo(AssistantAnswerSource.FAQ);
        assertThat(result.answer()).contains("提交柜台");
    }

    @Test
    void shouldUseAiAnswerWhenConfiguredClientReturnsAnswer() {
        AssistantAnswerOrchestrator orchestrator = new AssistantAnswerOrchestrator(
                properties(true, "test-key", "demo-model"),
                new AssistantFallbackService(),
                Optional.of(context -> Optional.of(aiAnswer())));

        AssistantAnswerResult result = orchestrator.answer("如何接取委托？", "hall");

        assertThat(result.source()).isEqualTo(AssistantAnswerSource.AI);
        assertThat(result.answer()).isEqualTo("AI answer");
    }

    @Test
    void shouldUseFallbackWhenAiClientFails() {
        AssistantAnswerOrchestrator orchestrator = new AssistantAnswerOrchestrator(
                properties(true, "test-key", "demo-model"),
                new AssistantFallbackService(),
                Optional.of(context -> {
                    throw new IllegalStateException("provider unavailable");
                }));

        AssistantAnswerResult result = orchestrator.answer("如何接取委托？", "hall");

        assertThat(result.source()).isEqualTo(AssistantAnswerSource.FAQ);
        assertThat(result.answer()).contains("悬赏任务板");
    }

    private AssistantAiProperties properties(boolean enabled, String apiKey, String model) {
        return new AssistantAiProperties(
                enabled,
                "openai-compatible",
                "http://localhost:65535/v1",
                apiKey,
                model,
                8000,
                4000);
    }

    private AssistantAnswerResult aiAnswer() {
        return new AssistantAnswerResult("AI answer", AssistantAnswerSource.AI, List.of("follow up"));
    }
}
