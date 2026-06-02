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
 * 推荐任务端点（P2），供悬赏任务板展示 Adventurer 专属推荐。
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
