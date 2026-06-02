package com.gitguild.backend.recommendation.service;

import com.gitguild.backend.recommendation.dto.RecommendationResponse;

/**
 * 推荐引擎：根据 Adventurer 的历史行为画像，从悬赏任务板中推荐匹配的任务。
 *
 * <p>当前唯一策略为 {@code "tech-difficulty"}（技术栈 + 难度加权），
 * 其余 strategy 值返回 {@code STRATEGY_NOT_AVAILABLE}（见 P4-025 设计文档延期项 A）。
 * 推荐结果最多 20 条，默认 5 条。
 */
public interface RecommendationService {

    /**
     * 为指定 Adventurer 生成推荐 quest 列表。
     *
     * @param userId           Adventurer 的 Git-Guild 用户 ID
     * @param strategy         策略名，当前仅 {@code "tech-difficulty"}；{@code null} 视为默认
     * @param beginnerFriendly 是否启用新手友好 boost
     * @param excludeAccepted  是否排除已有 ACTIVE 接取的 quest
     * @param limit            返回条数（1–20），默认 5
     * @return 推荐结果，含 score / strongMatch / reasons
     */
    RecommendationResponse recommendQuests(Long userId, String strategy, boolean beginnerFriendly,
            boolean excludeAccepted, int limit);
}
