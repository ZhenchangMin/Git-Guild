package com.gitguild.backend.growth.service;

import com.gitguild.backend.growth.dto.BadgeResponse;
import com.gitguild.backend.growth.dto.ContributionResponse;
import com.gitguild.backend.growth.dto.GrowthSummaryResponse;
import com.gitguild.backend.growth.dto.LeaderboardResponse;
import com.gitguild.backend.quest.domain.Quest;
import com.gitguild.backend.user.domain.User;

/**
 * 成长激励发放与查询。
 *
 * <p>写入侧由审核通过触发（{@code ReviewServiceImpl} 的 APPROVED 分支同事务调用），
 * 读取侧供成长档案前端（P4-023）展示。设计见《P4-022 成长激励后端设计》。
 */
public interface GrowthService {

    /**
     * 为完成任务的 Adventurer 发放成长激励：累加 XP、重算等级、写 XP 流水与贡献记录。
     *
     * <p><b>幂等：</b>若该 (user, quest) 已有贡献记录则整体跳过，重复调用不会二次发放。
     * 应在审核事务内调用，使其与审核结果原子提交。
     *
     * @param submitter 成果提交者（Adventurer）
     * @param quest     已通过审核的任务，XP 取自其 {@code rewardXp}
     */
    void grantQuestCompletion(User submitter, Quest quest);

    /**
     * 查询用户成长摘要。无档案时返回初始值（不抛异常）。
     *
     * @param userId Git-Guild 用户 ID
     */
    GrowthSummaryResponse getSummary(Long userId);

    LeaderboardResponse getXpLeaderboard(String period, int limit);

    BadgeResponse getBadges(Long userId);

    /**
     * 查询用户「贡献历程」明细与贡献仓库数。无贡献时返回空列表与 0（不抛异常、不回退演示数据）。
     *
     * @param userId Git-Guild 用户 ID
     */
    ContributionResponse getContributions(Long userId);
}
