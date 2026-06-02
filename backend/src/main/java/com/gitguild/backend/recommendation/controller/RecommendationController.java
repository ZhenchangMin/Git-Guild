package com.gitguild.backend.recommendation.controller;

import com.gitguild.backend.common.ApiResponse;
import com.gitguild.backend.recommendation.dto.RecommendationResponse;
import com.gitguild.backend.recommendation.service.RecommendationService;
import com.gitguild.backend.security.SecurityPrincipalUtils;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * 推荐任务端点（P2，契约 §74）。
 *
 * <p>Adventurer 从悬赏任务板打开推荐视图时调用此端点。Controller 本身无业务逻辑——
 * 身份从 JWT 解析（不信任前端传入的 userId）、参数校验委托 Spring MVC、推荐逻辑全部在
 * {@link RecommendationService} 中。
 *
 * <p><b>暴露的功能：</b>一个 GET 端点，返回按匹配分降序排列的推荐 Quest 列表，
 * 每条含 {@code score} / {@code strongMatch} / {@code reasons[]}，前端可直接渲染为推荐卡片。
 *
 * <p><b>边界错误模式：</b>未登录由 SecurityConfig 拦截返回 401；
 * 未知 strategy 由 Service 层抛 {@code STRATEGY_NOT_AVAILABLE}（HTTP 400）；
 * 用户不存在抛 {@code USER_NOT_FOUND}（HTTP 404）。
 */
@RestController
@RequestMapping("/api/v1/recommendations")
public class RecommendationController {

    private final RecommendationService recommendationService;

    public RecommendationController(RecommendationService recommendationService) {
        this.recommendationService = recommendationService;
    }

    @GetMapping("/quests")
    public ApiResponse<RecommendationResponse> recommendQuests(
            Authentication authentication,
            @RequestParam(defaultValue = "tech-difficulty") String strategy,
            @RequestParam(defaultValue = "true") boolean beginnerFriendly,
            @RequestParam(defaultValue = "true") boolean excludeAccepted,
            @RequestParam(defaultValue = "5") int limit) {
        return ApiResponse.success(recommendationService.recommendQuests(
                SecurityPrincipalUtils.currentUserId(authentication),
                strategy, beginnerFriendly, excludeAccepted, limit));
    }
}
