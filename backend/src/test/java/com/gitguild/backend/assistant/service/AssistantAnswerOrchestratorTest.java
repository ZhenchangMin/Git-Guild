package com.gitguild.backend.assistant.service;

import static org.assertj.core.api.Assertions.assertThat;

import com.gitguild.backend.assistant.config.AssistantAiProperties;
import com.gitguild.backend.assistant.dto.AssistantAnswerSource;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicInteger;
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
    void shouldPreemptRestrictedMaintainerIntentsBeforeCallingAi() {
        AtomicInteger aiCalls = new AtomicInteger();
        AssistantAnswerOrchestrator orchestrator = new AssistantAnswerOrchestrator(
                properties(true, "test-key", "demo-model"),
                new AssistantFallbackService(),
                Optional.of(context -> {
                    aiCalls.incrementAndGet();
                    return Optional.of(aiAnswer());
                }));

        List.of("我怎么发布委托？", "怎么审核委托？", "我能审核提交吗？", "怎么合并 PR？", "怎么同步仓库？")
                .forEach(message -> {
                    AssistantAnswerResult result = orchestrator.answer(authenticatedContext(message, "ROLE_BEGINNER"));

                    assertThat(result.source()).isEqualTo(AssistantAnswerSource.FAQ);
                    assertThat(result.answer()).contains("登录");
                    assertThat(result.answer()).doesNotContain("AI answer");
                });
        assertThat(aiCalls.get()).isZero();
    }

    @Test
    void shouldAllowMaintainerIntentsToUseAiForMaintainerRole() {
        AssistantAnswerOrchestrator orchestrator = new AssistantAnswerOrchestrator(
                properties(true, "test-key", "demo-model"),
                new AssistantFallbackService(),
                Optional.of(context -> Optional.of(aiAnswer())));

        AssistantAnswerResult result = orchestrator.answer(authenticatedContext("我怎么发布委托？", "ROLE_MAINTAINER"));

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

    private AssistantChatContext authenticatedContext(String message, String role) {
        return new AssistantChatContext(
                message,
                "hall",
                3001L,
                List.of(role),
                null,
                List.of(),
                List.of(),
                List.of());
    }
}
