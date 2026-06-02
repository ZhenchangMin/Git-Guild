package com.gitguild.backend.recommendation.dto;

import java.time.OffsetDateTime;
import java.util.List;

/**
 * 推荐任务响应体（契约 §74 {@code GET /recommendations/quests}）。
 */
public record RecommendationResponse(
        UserBrief user,
        String strategy,
        OffsetDateTime generatedAt,
        List<RecommendationItem> items) {

    public record UserBrief(Long userId, int level, int totalXp) {
    }

    public record RecommendationItem(
            QuestBrief quest,
            double score,
            boolean strongMatch,
            List<String> reasons) {

        public record QuestBrief(
                Long questId,
                String title,
                String difficulty,
                List<String> techStack,
                int rewardXp,
                String repositoryName) {
        }
    }
}
