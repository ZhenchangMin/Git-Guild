package com.gitguild.backend.assistant.controller;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.gitguild.backend.assistant.config.AssistantRateLimitProperties;
import com.gitguild.backend.assistant.dto.AssistantAnswerSource;
import com.gitguild.backend.assistant.dto.AssistantChatRequest;
import com.gitguild.backend.assistant.service.AssistantActionResolver;
import com.gitguild.backend.assistant.service.AssistantFallbackService;
import com.gitguild.backend.assistant.service.AssistantChatContext;
import com.gitguild.backend.assistant.service.AssistantContextAssembler;
import com.gitguild.backend.assistant.service.AssistantRateLimiter;
import com.gitguild.backend.common.GlobalExceptionHandler;
import com.gitguild.backend.security.CurrentUserPrincipal;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.security.authentication.TestingAuthenticationToken;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

class AssistantControllerTest {

    private MockMvc mockMvc;
    private ObjectMapper objectMapper;

    @BeforeEach
    void setUp() {
        objectMapper = new ObjectMapper();
        mockMvc = MockMvcBuilders
                .standaloneSetup(new AssistantController(
                        new AssistantActionResolver(),
                        new AssistantFallbackService(),
                        contextAssembler(),
                        disabledRateLimiter()))
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();
    }

    @Test
    void chatShouldReturnContractShapeForValidRequest() throws Exception {
        AssistantChatRequest request = request("我接了任务后下一步该做什么？", "adventurer-workbench");

        mockMvc.perform(post("/api/v1/assistant/chat")
                        .contentType("application/json")
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value("SUCCESS"))
                .andExpect(jsonPath("$.data.answer").isString())
                .andExpect(jsonPath("$.data.source").value(AssistantAnswerSource.FAQ.name()))
                .andExpect(jsonPath("$.data.suggestedQuestions").isArray())
                .andExpect(jsonPath("$.data.actions").isArray());
    }

    @Test
    void chatShouldUseFallbackForOutOfScopeQuestion() throws Exception {
        AssistantChatRequest request = request("今天晚饭吃什么？", "hall");

        mockMvc.perform(post("/api/v1/assistant/chat")
                        .contentType("application/json")
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.source").value(AssistantAnswerSource.FALLBACK.name()))
                .andExpect(jsonPath("$.data.answer").value(org.hamcrest.Matchers.containsString("Git Guild")));
    }

    @Test
    void chatShouldExplainCurrentPageWhenMessageUsesPageContext() throws Exception {
        AssistantChatRequest request = request("这个页面怎么用？", "quest-board");

        mockMvc.perform(post("/api/v1/assistant/chat")
                        .contentType("application/json")
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.source").value(AssistantAnswerSource.FAQ.name()))
                .andExpect(jsonPath("$.data.answer").value(org.hamcrest.Matchers.containsString("悬赏任务板")));
    }

    @Test
    void chatShouldResolveQuestBoardActionFromNaturalLanguage() throws Exception {
        AssistantChatRequest request = request("委托板在哪？我想接任务", "hall");

        mockMvc.perform(post("/api/v1/assistant/chat")
                        .contentType("application/json")
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.actions[0].label").value("打开悬赏任务板"))
                .andExpect(jsonPath("$.data.actions[0].routeName").value("quest-board"));
    }

    @Test
    void chatShouldResolveQuestBoardActionForRecommendationRequest() throws Exception {
        AssistantChatRequest request = request("给我推荐一个任务", "hall");

        mockMvc.perform(post("/api/v1/assistant/chat")
                        .principal(authentication(3001L, "ROLE_BEGINNER"))
                        .contentType("application/json")
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.source").value(AssistantAnswerSource.FAQ.name()))
                .andExpect(jsonPath("$.data.answer").value(org.hamcrest.Matchers.containsString("公开可接委托")))
                .andExpect(jsonPath("$.data.answer").value(org.hamcrest.Matchers.containsString("不会自动跳转")))
                .andExpect(jsonPath("$.data.actions[0].label").value("打开悬赏任务板"))
                .andExpect(jsonPath("$.data.actions[0].routeName").value("quest-board"));
    }

    @Test
    void chatShouldResolveSubmissionCounterActionFromNaturalLanguage() throws Exception {
        AssistantChatRequest request = request("我想交作业，登记成果应该去哪里？", "hall");

        mockMvc.perform(post("/api/v1/assistant/chat")
                        .contentType("application/json")
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.actions[0].label").value("打开提交柜台"))
                .andExpect(jsonPath("$.data.actions[0].routeName").value("submission-counter"));
    }

    @Test
    void chatShouldResolveSubmissionCounterForSubmitQuestWords() throws Exception {
        AssistantChatRequest request = request("提交委托和提交任务应该去哪里？", "hall");

        mockMvc.perform(post("/api/v1/assistant/chat")
                        .contentType("application/json")
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.actions[0].routeName").value("submission-counter"));
    }

    @Test
    void chatShouldResolveMaintainerPublishForPublishTaskWords() throws Exception {
        AssistantChatRequest request = request("我想发任务", "hall");

        mockMvc.perform(post("/api/v1/assistant/chat")
                        .principal(authentication(2001L, "ROLE_MAINTAINER"))
                        .contentType("application/json")
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.actions[0].routeName").value("maintainer-publish"));
    }

    @Test
    void chatShouldResolveMaintainerReviewForReviewVerbAndObject() throws Exception {
        AssistantChatRequest request = request("我想审查成果", "hall");

        mockMvc.perform(post("/api/v1/assistant/chat")
                        .principal(authentication(2001L, "ROLE_MAINTAINER"))
                        .contentType("application/json")
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.actions[0].routeName").value("maintainer-review"));
    }

    @Test
    void chatShouldNotReturnMaintainerActionsForAdventurerRestrictedIntents() throws Exception {
        for (String message : List.of("我怎么发布委托？", "怎么审核委托？", "我能审核提交吗？", "怎么合并 PR？", "怎么同步仓库？")) {
            AssistantChatRequest request = request(message, "hall");

            mockMvc.perform(post("/api/v1/assistant/chat")
                            .principal(authentication(3001L, "ROLE_BEGINNER"))
                            .contentType("application/json")
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.data.source").value(AssistantAnswerSource.FAQ.name()))
                    .andExpect(jsonPath("$.data.answer").value(org.hamcrest.Matchers.containsString("登录")))
                    .andExpect(jsonPath("$.data.actions").value(org.hamcrest.Matchers.empty()));
        }
    }

    @Test
    void chatShouldResolveProfileForPersonalProfileWords() throws Exception {
        AssistantChatRequest request = request("个人档案在哪里？", "hall");

        mockMvc.perform(post("/api/v1/assistant/chat")
                        .contentType("application/json")
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.actions[0].routeName").value("profile"));
    }

    @Test
    void chatShouldResolveAdventurerWorkbenchForBeginnerRole() throws Exception {
        AssistantChatRequest request = request("打开工作台", "hall");

        mockMvc.perform(post("/api/v1/assistant/chat")
                        .principal(authentication(3001L, "ROLE_BEGINNER"))
                        .contentType("application/json")
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.actions[0].label").value("打开冒险家工作台"))
                .andExpect(jsonPath("$.data.actions[0].routeName").value("adventurer-workbench"));
    }

    @Test
    void chatShouldResolveLoginForAnonymousWorkbenchRequest() throws Exception {
        AssistantChatRequest request = request("打开工作台", "hall");

        mockMvc.perform(post("/api/v1/assistant/chat")
                        .contentType("application/json")
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.actions[0].label").value("前往登录后打开工作台"))
                .andExpect(jsonPath("$.data.actions[0].routeName").value("login"));
    }

    @Test
    void chatShouldResolveMaintainerWorkbenchForMaintainerRole() throws Exception {
        AssistantChatRequest request = request("打开工作台", "hall");

        mockMvc.perform(post("/api/v1/assistant/chat")
                        .principal(authentication(2001L, "ROLE_MAINTAINER"))
                        .contentType("application/json")
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.actions[0].label").value("打开委托人工作台"))
                .andExpect(jsonPath("$.data.actions[0].routeName").value("maintainer-workbench"));
    }

    @Test
    void chatShouldRejectBlankMessage() throws Exception {
        AssistantChatRequest request = request("   ", "hall");

        mockMvc.perform(post("/api/v1/assistant/chat")
                        .contentType("application/json")
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("VALIDATION_FAILED"))
                .andExpect(jsonPath("$.details").value("message cannot be blank"));
    }

    @Test
    void chatShouldRejectOverlongMessage() throws Exception {
        AssistantChatRequest request = request("x".repeat(501), "hall");

        mockMvc.perform(post("/api/v1/assistant/chat")
                        .contentType("application/json")
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("VALIDATION_FAILED"))
                .andExpect(jsonPath("$.details").value("message length cannot exceed 500"));
    }

    @Test
    void chatShouldRejectOverlongPage() throws Exception {
        AssistantChatRequest request = request("怎么提交成果？", "x".repeat(65));

        mockMvc.perform(post("/api/v1/assistant/chat")
                        .contentType("application/json")
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("VALIDATION_FAILED"))
                .andExpect(jsonPath("$.details").value("page length cannot exceed 64"));
    }

    @Test
    void chatShouldRejectWhenRateLimitExceeded() throws Exception {
        MockMvc limitedMockMvc = MockMvcBuilders
                .standaloneSetup(new AssistantController(
                        new AssistantActionResolver(),
                        new AssistantFallbackService(),
                        contextAssembler(),
                        new AssistantRateLimiter(new AssistantRateLimitProperties(true, 1, 60))))
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();
        AssistantChatRequest request = request("怎么接任务？", "hall");

        limitedMockMvc.perform(post("/api/v1/assistant/chat")
                        .header("X-Forwarded-For", "203.0.113.10")
                        .contentType("application/json")
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk());

        limitedMockMvc.perform(post("/api/v1/assistant/chat")
                        .header("X-Forwarded-For", "203.0.113.10")
                        .contentType("application/json")
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isTooManyRequests())
                .andExpect(jsonPath("$.code").value("RATE_LIMIT_EXCEEDED"))
                .andExpect(jsonPath("$.message").value("对话过于频繁，请不要频繁对话，稍后再试"))
                .andExpect(jsonPath("$.details").value("maxRequests=1, windowSeconds=60"));
    }

    private AssistantChatRequest request(String message, String page) {
        AssistantChatRequest request = new AssistantChatRequest();
        request.setMessage(message);
        request.setPage(page);
        return request;
    }

    private TestingAuthenticationToken authentication(Long userId, String role) {
        return new TestingAuthenticationToken(new CurrentUserPrincipal(userId, List.of(role), 0), null);
    }

    private AssistantContextAssembler contextAssembler() {
        return new AssistantContextAssembler(null, null, null, null, null, null, objectMapper) {
            @Override
            public AssistantChatContext assemble(String message, String page, org.springframework.security.core.Authentication authentication) {
                return AssistantChatContext.from(message, page, authentication);
            }
        };
    }

    private AssistantRateLimiter disabledRateLimiter() {
        return new AssistantRateLimiter(new AssistantRateLimitProperties(false, 10, 60));
    }
}
