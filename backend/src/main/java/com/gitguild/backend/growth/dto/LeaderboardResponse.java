package com.gitguild.backend.growth.dto;

import java.time.OffsetDateTime;
import java.util.List;

public record LeaderboardResponse(
        String period,
        OffsetDateTime generatedAt,
        List<LeaderboardItem> items) {

    public record LeaderboardItem(
            int rank,
            Long userId,
            String username,
            int level,
            String title,
            int totalXp,
            int completedQuestCount) {
    }
}
