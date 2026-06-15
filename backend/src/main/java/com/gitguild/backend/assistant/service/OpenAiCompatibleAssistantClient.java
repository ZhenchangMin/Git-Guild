package com.gitguild.backend.assistant.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.gitguild.backend.assistant.config.AssistantAiProperties;
import com.gitguild.backend.assistant.dto.AssistantAnswerSource;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

@Service
public class OpenAiCompatibleAssistantClient implements AssistantAiClient {

    private static final int MAX_ANSWER_LENGTH = 1000;
    private static final int MAX_SUGGESTION_LENGTH = 80;
    private static final int MAX_SUGGESTIONS = 3;

    private final AssistantAiProperties properties;
    private final ObjectMapper objectMapper;
    private final RestClient restClient;

    @Autowired
    public OpenAiCompatibleAssistantClient(AssistantAiProperties properties, ObjectMapper objectMapper) {
        this(properties, objectMapper, createRestClient(properties));
    }

    OpenAiCompatibleAssistantClient(
            AssistantAiProperties properties,
            ObjectMapper objectMapper,
            RestClient restClient) {
        this.properties = properties;
        this.objectMapper = objectMapper;
        this.restClient = restClient;
    }

    @Override
    public Optional<AssistantAnswerResult> tryAnswer(AssistantChatContext context) {
        if (!properties.isReady()) {
            return Optional.empty();
        }

        try {
            String body = restClient.post()
                    .uri(chatCompletionsEndpoint())
                    .body(requestBody(context))
                    .retrieve()
                    .body(String.class);
            return parseResponse(body);
        } catch (RestClientException | JsonProcessingException | IllegalArgumentException ex) {
            return Optional.empty();
        }
    }

    private static RestClient createRestClient(AssistantAiProperties properties) {
        SimpleClientHttpRequestFactory requestFactory = new SimpleClientHttpRequestFactory();
        requestFactory.setConnectTimeout(properties.timeoutMs());
        requestFactory.setReadTimeout(properties.timeoutMs());
        return RestClient.builder()
                .requestFactory(requestFactory)
                .defaultHeader(HttpHeaders.AUTHORIZATION, "Bearer " + nullToEmpty(properties.apiKey()))
                .defaultHeader(HttpHeaders.ACCEPT, MediaType.APPLICATION_JSON_VALUE)
                .defaultHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                .build();
    }

    private Map<String, Object> requestBody(AssistantChatContext context) {
        Map<String, Object> request = new LinkedHashMap<>();
        request.put("model", properties.model());
        request.put("messages", List.of(
                Map.of("role", "system", "content", systemPrompt()),
                Map.of("role", "user", "content", userPrompt(context))));
        request.put("temperature", 0.2);
        request.put("max_tokens", 700);
        request.put("response_format", Map.of("type", "json_object"));
        return request;
    }

    private String systemPrompt() {
        return """
                你是 Git Guild 的前台 AI 向导，面向游客、冒险家和委托人回答平台使用问题。
                必须遵守：
                1. 只回答 Git Guild 的平台使用、委托流程、提交审核、仓库接入、Git/Gitea 基础操作问题。
                2. 不处理管理员后台问题，不索要或输出密码、Token、密钥等敏感信息。
                3. 不承诺已经替用户提交、审核、发布或跳转；页面跳转由前端按钮处理。
                4. 如果问题超出范围，简短说明你能回答的范围，并给出可继续追问的方向。
                5. 推荐新委托时，只能从“可推荐的委托板候选”中选择，并说明 questId、标题和推荐理由；没有候选时明确说明暂无可推荐委托。
                6. 不得基于未提供的个人画像或状态编造用户偏好、委托状态、提交状态。
                7. 用简洁中文回答，避免编造不存在的状态或数据。
                8. 只输出严格 JSON，不要 Markdown，不要代码块，格式为：
                   {"answer":"回答文本","suggestedQuestions":["后续问题1","后续问题2"]}
                """;
    }

    private String userPrompt(AssistantChatContext context) {
        String prompt = """
                当前页面上下文：%s
                当前用户身份：%s
                当前用户成长画像：%s
                可推荐的委托板候选：%s
                已授权可见的委托状态：%s
                已授权可见的提交状态：%s
                用户问题：
                %s
                """.formatted(
                blankToUnknown(context.page()),
                context.roleLabel(),
                userProfileText(context),
                questRecommendationText(context),
                questStatusText(context),
                submissionStatusText(context),
                nullToEmpty(context.message()));
        return truncate(prompt, properties.maxPromptChars());
    }

    private String userProfileText(AssistantChatContext context) {
        if (context.userProfile() == null) {
            return "无";
        }
        AssistantChatContext.UserProfileSnapshot profile = context.userProfile();
        return "level=%d,totalXp=%d,completedQuestCount=%d,preferredTechStacks=%s,difficultyComfort=%s"
                .formatted(
                        profile.level(),
                        profile.totalXp(),
                        profile.completedQuestCount(),
                        profile.preferredTechStacks(),
                        profile.difficultyComfort());
    }

    private String questRecommendationText(AssistantChatContext context) {
        if (context.questBoardCandidates().isEmpty()) {
            return "无";
        }
        return context.questBoardCandidates().stream()
                .limit(8)
                .map(candidate -> "questId=%s,title=%s,difficulty=%s,techStack=%s,rewardXp=%d,status=%s,matchScore=%.2f,reasons=%s"
                        .formatted(
                                candidate.questId(),
                                clean(candidate.title()),
                                clean(candidate.difficulty()),
                                candidate.techStack(),
                                candidate.rewardXp(),
                                clean(candidate.status()),
                                candidate.matchScore(),
                                candidate.reasons()))
                .toList()
                .toString();
    }

    private String questStatusText(AssistantChatContext context) {
        if (context.questStatuses().isEmpty()) {
            return "无";
        }
        return context.questStatuses().stream()
                .limit(5)
                .map(status -> "questId=%s,title=%s,status=%s".formatted(
                        status.questId(), clean(status.title()), clean(status.status())))
                .toList()
                .toString();
    }

    private String submissionStatusText(AssistantChatContext context) {
        if (context.submissionStatuses().isEmpty()) {
            return "无";
        }
        return context.submissionStatuses().stream()
                .limit(5)
                .map(status -> "submissionId=%s,questId=%s,status=%s".formatted(
                        status.submissionId(), status.questId(), clean(status.status())))
                .toList()
                .toString();
    }

    private Optional<AssistantAnswerResult> parseResponse(String responseBody) throws JsonProcessingException {
        if (responseBody == null || responseBody.isBlank()) {
            return Optional.empty();
        }

        JsonNode root = objectMapper.readTree(responseBody);
        JsonNode contentNode = root.path("choices").path(0).path("message").path("content");
        if (!contentNode.isTextual()) {
            return Optional.empty();
        }

        return parseAssistantContent(contentNode.asText());
    }

    private Optional<AssistantAnswerResult> parseAssistantContent(String content) throws JsonProcessingException {
        String json = extractJsonObject(content);
        JsonNode root = objectMapper.readTree(json);
        String answer = truncate(clean(root.path("answer").asText("")), MAX_ANSWER_LENGTH);
        if (answer.isBlank()) {
            return Optional.empty();
        }

        List<String> suggestions = new ArrayList<>();
        JsonNode suggestionNodes = root.path("suggestedQuestions");
        if (suggestionNodes.isArray()) {
            for (JsonNode node : suggestionNodes) {
                String suggestion = truncate(clean(node.asText("")), MAX_SUGGESTION_LENGTH);
                if (!suggestion.isBlank()) {
                    suggestions.add(suggestion);
                }
                if (suggestions.size() == MAX_SUGGESTIONS) {
                    break;
                }
            }
        }

        return Optional.of(new AssistantAnswerResult(answer, AssistantAnswerSource.AI, List.copyOf(suggestions)));
    }

    private String chatCompletionsEndpoint() {
        return trimTrailingSlash(properties.baseUrl()) + "/chat/completions";
    }

    private String extractJsonObject(String content) {
        String trimmed = nullToEmpty(content).trim();
        int start = trimmed.indexOf('{');
        int end = trimmed.lastIndexOf('}');
        if (start < 0 || end <= start) {
            throw new IllegalArgumentException("Assistant response does not contain a JSON object.");
        }
        return trimmed.substring(start, end + 1);
    }

    private String clean(String value) {
        return nullToEmpty(value).replaceAll("\\s+", " ").trim();
    }

    private String truncate(String value, int maxChars) {
        String text = nullToEmpty(value);
        if (maxChars <= 0 || text.length() <= maxChars) {
            return text;
        }
        return text.substring(0, maxChars);
    }

    private static String trimTrailingSlash(String value) {
        String text = nullToEmpty(value).trim();
        while (text.endsWith("/")) {
            text = text.substring(0, text.length() - 1);
        }
        return text;
    }

    private static String blankToUnknown(String value) {
        return value == null || value.isBlank() ? "unknown" : value.trim();
    }

    private static String nullToEmpty(String value) {
        return value == null ? "" : value;
    }
}
