package com.gitguild.backend.assistant.controller;

import com.gitguild.backend.assistant.dto.AssistantAnswerSource;
import com.gitguild.backend.assistant.dto.AssistantChatRequest;
import com.gitguild.backend.assistant.dto.AssistantChatResponse;
import com.gitguild.backend.assistant.dto.AssistantChatResponse.AssistantAction;
import com.gitguild.backend.assistant.service.AssistantActionResolver;
import com.gitguild.backend.assistant.service.AssistantAnswerResult;
import com.gitguild.backend.assistant.service.AssistantAnswerService;
import com.gitguild.backend.assistant.service.AssistantChatContext;
import com.gitguild.backend.assistant.service.AssistantContextAssembler;
import com.gitguild.backend.assistant.service.AssistantRateLimiter;
import com.gitguild.backend.common.ApiResponse;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import java.util.List;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/assistant")
public class AssistantController {

    private final AssistantActionResolver actionResolver;
    private final AssistantAnswerService answerService;
    private final AssistantContextAssembler contextAssembler;
    private final AssistantRateLimiter rateLimiter;

    public AssistantController(
            AssistantActionResolver actionResolver,
            AssistantAnswerService answerService,
            AssistantContextAssembler contextAssembler,
            AssistantRateLimiter rateLimiter) {
        this.actionResolver = actionResolver;
        this.answerService = answerService;
        this.contextAssembler = contextAssembler;
        this.rateLimiter = rateLimiter;
    }

    @PostMapping("/chat")
    public ApiResponse<AssistantChatResponse> chat(
            @Valid @RequestBody AssistantChatRequest request,
            Authentication authentication,
            HttpServletRequest servletRequest) {
        AssistantChatContext context = contextAssembler.assemble(
                request.getMessage(), request.getPage(), authentication);
        rateLimiter.check(context, servletRequest);
        List<AssistantAction> actions = actionResolver.resolveActions(context);
        AssistantAnswerResult answer = answerService.answer(context);
        AssistantChatResponse response = new AssistantChatResponse(
                actions.isEmpty()
                        ? answer.answer()
                        : answer.answer() + " 系统不会自动跳转；如需前往相关页面，请点击回答下方的按钮。",
                answer.source(),
                answer.suggestedQuestions(),
                actions);
        return ApiResponse.success(response);
    }
}
