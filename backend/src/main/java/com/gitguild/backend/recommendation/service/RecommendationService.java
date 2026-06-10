package com.gitguild.backend.recommendation.service;

import com.gitguild.backend.recommendation.dto.RecommendationResponse;

/**
 * 推荐引擎：根据 Adventurer 的历史行为画像，从悬赏任务板中推荐匹配的任务。
 *
 * <p>当前唯一策略为 {@code "tech-difficulty"}（技术栈 + 难度加权），
 * 其余 strategy 值返回 {@code STRATEGY_NOT_AVAILABLE}（见 P4-025 设计文档延期项 A）。
 * 推荐结果与 Quest Board 默认候选集数量一致，算法只影响排序、分数和推荐理由。
 */
public interface RecommendationService {

    /**
     * 为指定 Adventurer 生成推荐 quest 列表。
     *
     * @param userId           Adventurer 的 Git-Guild 用户 ID
     * @param strategy         策略名，当前仅 {@code "tech-difficulty"}；{@code null} 视为默认
     * @param beginnerFriendly 是否启用新手友好 boost
     * @param excludeAccepted  兼容旧契约的参数；当前不减少候选列表数量
     * @param limit            兼容旧契约的参数；当前不截断候选列表数量
     * @return 推荐排序结果，含 score / strongMatch / reasons
     */
    RecommendationResponse recommendQuests(Long userId, String strategy, boolean beginnerFriendly,
            boolean excludeAccepted, int limit);
}
