package com.gitguild.backend.recommendation.dto;

import java.time.OffsetDateTime;
import java.util.List;

/**
 * 推荐任务响应体（契约 §74 {@code GET /recommendations/quests}）。
 *
 * <p>{@code strategy} 标识本次推荐使用的策略名（当前仅有 {@code "tech-difficulty"}），
 * {@code generatedAt} 为本次推荐生成时刻（每次请求均重新计算，不缓存）。
 * {@code items} 与 Quest Board 默认候选集数量一致，并按 {@code score} 降序排列。
 */
public record RecommendationResponse(
        UserBrief user,
        String strategy,
        OffsetDateTime generatedAt,
        List<RecommendationItem> items) {

    /**
     * 推荐受众摘要：Adventurer 的当前等级和累计 XP。
     * 用于前端在推荐卡片上方展示"为你推荐"的身份上下文。
     */
    public record UserBrief(Long userId, int level, int totalXp) {
    }

    /**
     * 单条推荐结果。
     *
     * @param quest       被推荐的 Quest 摘要
     * @param score       综合匹配分（0–1.2），权重见 {@code RecommendationServiceImpl}
     * @param strongMatch {@code score ≥ 0.7} 时为 true，前端可用于高亮展示
     * @param reasons     推荐理由列表，如"技术栈匹配""难度适合你""新手友好"等；至少有一条
     */
    public record RecommendationItem(
            QuestBrief quest,
            double score,
            boolean strongMatch,
            List<String> reasons) {

        /**
         * Quest 摘要——推荐列表所需的 Quest 字段子集，非全量 Quest 详情。
         *
         * @param questId        Quest ID，前端跳转详情页用
         * @param title          Quest 标题
         * @param difficulty     难度枚举值（{@code A/B/C/D}）
         * @param techStack      技术栈标签列表，已从 JSON 解析为字符串数组
         * @param rewardXp       完成此 Quest 可获得的 XP（来自 {@code Quest.rewardXp}）
         * @param repositoryName 所属仓库名，用于展示"来自哪个项目"
         */
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
