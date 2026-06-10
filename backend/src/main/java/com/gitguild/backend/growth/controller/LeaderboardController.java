package com.gitguild.backend.growth.controller;

import com.gitguild.backend.common.ApiResponse;
import com.gitguild.backend.growth.dto.LeaderboardResponse;
import com.gitguild.backend.growth.service.GrowthService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/leaderboards")
public class LeaderboardController {

    private final GrowthService growthService;

    public LeaderboardController(GrowthService growthService) {
        this.growthService = growthService;
    }

    @GetMapping("/xp")
    public ApiResponse<LeaderboardResponse> getXpLeaderboard(
            @RequestParam(defaultValue = "ALL_TIME") String period,
            @RequestParam(defaultValue = "10") int limit) {
        return ApiResponse.success(growthService.getXpLeaderboard(period, limit));
    }
}
