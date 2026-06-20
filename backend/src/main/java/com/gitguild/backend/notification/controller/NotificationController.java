package com.gitguild.backend.notification.controller;

import com.gitguild.backend.common.ApiResponse;
import com.gitguild.backend.notification.dto.NotificationResponses.NotificationFeed;
import com.gitguild.backend.notification.service.NotificationService;
import com.gitguild.backend.security.SecurityPrincipalUtils;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * 当前用户的站内通知中心（P4-024）。
 *
 * <p>始终以 JWT 解析出的当前用户为作用域，前端无需也无法传入 userId——
 * 与 {@code GrowthController} 的 {@code /users/me} 自我端点风格一致。
 * 标记已读后统一返回刷新后的 Feed，使前端一次请求即可拿到最新红点数与列表。
 */
@RestController
@RequestMapping("/api/v1/users/me/notifications")
public class NotificationController {

    /** 收件箱默认返回的最近条数。 */
    private static final int DEFAULT_LIMIT = 20;

    private final NotificationService notificationService;

    public NotificationController(NotificationService notificationService) {
        this.notificationService = notificationService;
    }

    @GetMapping
    public ApiResponse<NotificationFeed> list(
            @RequestParam(name = "limit", defaultValue = "" + DEFAULT_LIMIT) int limit,
            Authentication authentication) {
        Long userId = SecurityPrincipalUtils.currentUserId(authentication);
        return ApiResponse.success(notificationService.getFeed(userId, limit));
    }

    @PostMapping("/{notificationId}/read")
    public ApiResponse<NotificationFeed> markRead(
            @PathVariable Long notificationId, Authentication authentication) {
        Long userId = SecurityPrincipalUtils.currentUserId(authentication);
        notificationService.markRead(userId, notificationId);
        return ApiResponse.success(notificationService.getFeed(userId, DEFAULT_LIMIT));
    }

    @PostMapping("/read-all")
    public ApiResponse<NotificationFeed> markAllRead(Authentication authentication) {
        Long userId = SecurityPrincipalUtils.currentUserId(authentication);
        notificationService.markAllRead(userId);
        return ApiResponse.success(notificationService.getFeed(userId, DEFAULT_LIMIT));
    }
}
