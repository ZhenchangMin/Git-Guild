package com.gitguild.backend.growth.dto;

import java.time.OffsetDateTime;
import java.util.List;

/**
 * 成长档案「贡献历程」（{@code GET /users/me/contributions}）。
 *
 * <p>{@code items} 为按完成时间倒序的真实贡献明细，前端据此渲染贡献时间线与难度曲线；
 * {@code repoCount} 为去重后的贡献仓库数，供成长指标卡展示——均来自 {@code contribution_records}，
 * 用户尚无贡献时返回空列表与 0，不再回退到演示数据。
 */
public record ContributionResponse(
        List<ContributionItem> items,
        int repoCount) {

    public record ContributionItem(
            Long recordId,
            Long questId,
            String questTitle,
            String repository,
            String difficulty,
            int xp,
            OffsetDateTime completedAt,
            String summary,
            List<String> techStack) {
    }
}
