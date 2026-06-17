package com.gitguild.backend.assistant.service;

import static org.assertj.core.api.Assertions.assertThat;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.gitguild.backend.assistant.config.AssistantAiProperties;
import com.gitguild.backend.assistant.dto.AssistantAnswerSource;
import com.sun.net.httpserver.HttpServer;
import java.io.IOException;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicReference;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

class OpenAiCompatibleAssistantClientTest {

    private final ObjectMapper objectMapper = new ObjectMapper();
    private HttpServer server;

    @AfterEach
    void tearDown() {
        if (server != null) {
            server.stop(0);
        }
    }

    @Test
    void shouldCallChatCompletionsAndParseJsonAnswer() throws Exception {
        AtomicReference<String> authorization = new AtomicReference<>();
        AtomicReference<String> requestBody = new AtomicReference<>();
        startServer(200, openAiResponse("""
                {"answer":"请先进入悬赏任务板查看可接委托。","suggestedQuestions":["如何接取委托？","如何提交成果？"]}
                """), authorization, requestBody);
        OpenAiCompatibleAssistantClient client = new OpenAiCompatibleAssistantClient(
                properties(baseUrl(), "test-key", "demo-model"), objectMapper);

        Optional<AssistantAnswerResult> result = client.tryAnswer(AssistantChatContext.anonymous("我想接任务", "hall"));

        assertThat(result).isPresent();
        assertThat(result.get().source()).isEqualTo(AssistantAnswerSource.AI);
        assertThat(result.get().answer()).isEqualTo("请先进入悬赏任务板查看可接委托。");
        assertThat(result.get().suggestedQuestions()).containsExactly("如何接取委托？", "如何提交成果？");
        assertThat(authorization.get()).isEqualTo("Bearer test-key");
        assertThat(requestBody.get()).contains("\"model\":\"demo-model\"");
        assertThat(requestBody.get()).contains("\"response_format\"");
        assertThat(requestBody.get()).contains("只输出严格 JSON");
        assertThat(requestBody.get()).contains("当前用户身份：游客");
        assertThat(requestBody.get()).contains("已授权可见的委托状态：无");
    }

    @Test
    void shouldIncludeProfileAndQuestCandidatesForRecommendationPrompt() throws Exception {
        AtomicReference<String> requestBody = new AtomicReference<>();
        startServer(200, openAiResponse("""
                {"answer":"推荐 questId=301：修复 Java 接口。","suggestedQuestions":["打开委托板看看？"]}
                """), new AtomicReference<>(), requestBody);
        OpenAiCompatibleAssistantClient client = new OpenAiCompatibleAssistantClient(
                properties(baseUrl(), "test-key", "demo-model"), objectMapper);
        AssistantChatContext context = new AssistantChatContext(
                "给我推荐一个任务",
                "quest-board",
                3001L,
                List.of("ROLE_BEGINNER"),
                new AssistantChatContext.UserProfileSnapshot(
                        2,
                        120,
                        1,
                        List.of("java", "spring"),
                        "C"),
                List.of(new AssistantChatContext.QuestRecommendationSnapshot(
                        301L,
                        "修复 Java 接口",
                        "C",
                        List.of("java", "spring"),
                        100,
                        8,
                        "后端",
                        List.of("bugfix", "spring"),
                        "PUBLISHED",
                        0.93,
                        List.of("技术栈匹配", "难度适合"))),
                List.of(),
                List.of());

        Optional<AssistantAnswerResult> result = client.tryAnswer(context);

        assertThat(result).isPresent();
        assertThat(requestBody.get()).contains("当前用户成长画像");
        assertThat(requestBody.get()).contains("level=2,totalXp=120,completedQuestCount=1");
        assertThat(requestBody.get()).contains("preferredTechStacks=[java, spring]");
        assertThat(requestBody.get()).contains("可推荐的委托板候选");
        assertThat(requestBody.get()).contains("questId=301");
        assertThat(requestBody.get()).contains("修复 Java 接口");
        assertThat(requestBody.get()).contains("estimatedHours=8");
        assertThat(requestBody.get()).contains("category=后端");
        assertThat(requestBody.get()).contains("tags=[bugfix, spring]");
        assertThat(requestBody.get()).contains("matchScore=0.93");
        assertThat(requestBody.get()).contains("只能从");
        assertThat(requestBody.get()).contains("不得基于未提供");
    }

    @Test
    void shouldReturnEmptyWhenProviderReturnsInvalidJsonContent() throws Exception {
        startServer(200, openAiResponse("不是 JSON"), new AtomicReference<>(), new AtomicReference<>());
        OpenAiCompatibleAssistantClient client = new OpenAiCompatibleAssistantClient(
                properties(baseUrl(), "test-key", "demo-model"), objectMapper);

        Optional<AssistantAnswerResult> result = client.tryAnswer(AssistantChatContext.anonymous("我想接任务", "hall"));

        assertThat(result).isEmpty();
    }

    @Test
    void shouldReturnEmptyWhenProviderReturnsHttpError() throws Exception {
        startServer(500, "{\"error\":\"upstream failed\"}", new AtomicReference<>(), new AtomicReference<>());
        OpenAiCompatibleAssistantClient client = new OpenAiCompatibleAssistantClient(
                properties(baseUrl(), "test-key", "demo-model"), objectMapper);

        Optional<AssistantAnswerResult> result = client.tryAnswer(AssistantChatContext.anonymous("我想接任务", "hall"));

        assertThat(result).isEmpty();
    }

    @Test
    void shouldReturnEmptyWhenConfigIsIncomplete() throws Exception {
        OpenAiCompatibleAssistantClient client = new OpenAiCompatibleAssistantClient(
                properties("", "test-key", "demo-model"), objectMapper);

        Optional<AssistantAnswerResult> result = client.tryAnswer(AssistantChatContext.anonymous("我想接任务", "hall"));

        assertThat(result).isEmpty();
    }

    private void startServer(
            int status,
            String response,
            AtomicReference<String> authorization,
            AtomicReference<String> requestBody) throws IOException {
        server = HttpServer.create(new InetSocketAddress("localhost", 0), 0);
        server.createContext("/v1/chat/completions", exchange -> {
            authorization.set(exchange.getRequestHeaders().getFirst("Authorization"));
            requestBody.set(new String(exchange.getRequestBody().readAllBytes(), StandardCharsets.UTF_8));
            byte[] bytes = response.getBytes(StandardCharsets.UTF_8);
            exchange.getResponseHeaders().set("Content-Type", "application/json;charset=UTF-8");
            exchange.sendResponseHeaders(status, bytes.length);
            try (OutputStream output = exchange.getResponseBody()) {
                output.write(bytes);
            }
        });
        server.start();
    }

    private String openAiResponse(String content) throws Exception {
        return objectMapper.writeValueAsString(Map.of(
                "choices", List.of(Map.of(
                        "message", Map.of("content", content)))));
    }

    private String baseUrl() {
        return "http://localhost:" + server.getAddress().getPort() + "/v1";
    }

    private AssistantAiProperties properties(String baseUrl, String apiKey, String model) {
        return new AssistantAiProperties(
                true,
                "openai-compatible",
                baseUrl,
                apiKey,
                model,
                8000,
                4000);
    }
}
