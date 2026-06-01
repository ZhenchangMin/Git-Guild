package com.gitguild.backend.growth.controller;

import com.gitguild.backend.common.ApiResponse;
import com.gitguild.backend.growth.dto.GrowthSummaryResponse;
import com.gitguild.backend.growth.service.GrowthService;
import com.gitguild.backend.security.SecurityPrincipalUtils;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 成长摘要读端点，供成长档案前端（P4-023）展示身份卡。
 */
@RestController
@RequestMapping("/api/v1/users/me")
public class GrowthController {

    private final GrowthService growthService;

    public GrowthController(GrowthService growthService) {
        this.growthService = growthService;
    }

    @GetMapping("/growth")
    public ApiResponse<GrowthSummaryResponse> getGrowthSummary(Authentication authentication) {
        return ApiResponse.success(growthService.getSummary(
                SecurityPrincipalUtils.currentUserId(authentication)));
    }
}
