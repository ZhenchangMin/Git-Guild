package com.gitguild.backend.growth.dto;

/**
 * 成长摘要（契约 §196 {@code GET /users/me/growth}）。
 * 用户尚无成长档案时返回初始值（level=1, totalXp=0, nextLevelXp=XP_PER_LEVEL, completedQuestCount=0）。
 */
public record GrowthSummaryResponse(
        int level,
        int totalXp,
        int nextLevelXp,
        int completedQuestCount) {
}
