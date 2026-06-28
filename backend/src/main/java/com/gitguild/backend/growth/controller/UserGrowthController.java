package com.gitguild.backend.growth.controller;

import com.gitguild.backend.common.ApiResponse;
import com.gitguild.backend.growth.dto.BadgeResponse;
import com.gitguild.backend.growth.dto.ContributionResponse;
import com.gitguild.backend.growth.dto.GrowthSummaryResponse;
import com.gitguild.backend.growth.service.GrowthService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 登录成员之间共享的只读成长档案接口。
 */
@RestController
@RequestMapping("/api/v1/users/{userId}")
public class UserGrowthController {

    private final GrowthService growthService;

    public UserGrowthController(GrowthService growthService) {
        this.growthService = growthService;
    }

    @GetMapping("/growth")
    public ApiResponse<GrowthSummaryResponse> getGrowthSummary(@PathVariable Long userId) {
        return ApiResponse.success(growthService.getSummary(userId));
    }

    @GetMapping("/badges")
    public ApiResponse<BadgeResponse> getBadges(@PathVariable Long userId) {
        return ApiResponse.success(growthService.getBadges(userId));
    }

    @GetMapping("/contributions")
    public ApiResponse<ContributionResponse> getContributions(@PathVariable Long userId) {
        return ApiResponse.success(growthService.getContributions(userId));
    }
}
