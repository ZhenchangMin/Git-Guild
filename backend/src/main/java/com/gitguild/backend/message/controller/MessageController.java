package com.gitguild.backend.message.controller;

import com.gitguild.backend.common.ApiResponse;
import com.gitguild.backend.message.dto.MessageRequests.SendMessageRequest;
import com.gitguild.backend.message.dto.MessageResponses.MessageThreadDetail;
import com.gitguild.backend.message.dto.MessageResponses.MessageThreadSummary;
import com.gitguild.backend.message.service.MessageService;
import com.gitguild.backend.security.SecurityPrincipalUtils;
import jakarta.validation.Valid;
import java.util.List;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/message-threads")
public class MessageController {

    private final MessageService messageService;

    public MessageController(MessageService messageService) {
        this.messageService = messageService;
    }

    @GetMapping
    public ApiResponse<List<MessageThreadSummary>> list(Authentication authentication) {
        Long userId = SecurityPrincipalUtils.currentUserId(authentication);
        return ApiResponse.success(messageService.listThreads(userId));
    }

    @GetMapping("/{threadId}")
    public ApiResponse<MessageThreadDetail> detail(@PathVariable Long threadId, Authentication authentication) {
        Long userId = SecurityPrincipalUtils.currentUserId(authentication);
        return ApiResponse.success(messageService.getThread(userId, threadId));
    }

    @GetMapping("/by-quest/{questId}")
    public ApiResponse<MessageThreadDetail> openQuestThread(
            @PathVariable Long questId, Authentication authentication) {
        Long userId = SecurityPrincipalUtils.currentUserId(authentication);
        return ApiResponse.success(messageService.openQuestThread(userId, questId));
    }

    @PostMapping("/{threadId}/messages")
    public ApiResponse<MessageThreadDetail> send(
            @PathVariable Long threadId,
            @Valid @RequestBody SendMessageRequest request,
            Authentication authentication) {
        Long userId = SecurityPrincipalUtils.currentUserId(authentication);
        return ApiResponse.success(messageService.sendMessage(userId, threadId, request.content()));
    }

    @PostMapping("/{threadId}/read")
    public ApiResponse<MessageThreadDetail> markRead(@PathVariable Long threadId, Authentication authentication) {
        Long userId = SecurityPrincipalUtils.currentUserId(authentication);
        return ApiResponse.success(messageService.markRead(userId, threadId));
    }
}
