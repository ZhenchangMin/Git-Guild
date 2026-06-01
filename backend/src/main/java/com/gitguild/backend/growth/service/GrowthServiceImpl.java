package com.gitguild.backend.growth.service;

import com.gitguild.backend.growth.domain.ContributionRecord;
import com.gitguild.backend.growth.domain.GrowthProfile;
import com.gitguild.backend.growth.domain.XpTransaction;
import com.gitguild.backend.growth.dto.GrowthSummaryResponse;
import com.gitguild.backend.growth.repository.ContributionRecordRepository;
import com.gitguild.backend.growth.repository.GrowthProfileRepository;
import com.gitguild.backend.growth.repository.XpTransactionRepository;
import com.gitguild.backend.quest.domain.Quest;
import com.gitguild.backend.user.domain.User;
import java.time.OffsetDateTime;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * {@link GrowthService} 的实现，隐藏成长激励发放的"聚合 + 流水 + 幂等"三件事：
 * <ul>
 *   <li>对 {@code growth_profiles} 走查找-或-新建，累加 XP 并由 {@code total_xp} 线性重算等级；</li>
 *   <li>向只增的 {@code xp_transactions} 追加一条审计流水；</li>
 *   <li>向 {@code contribution_records} 写入贡献记录，其唯一键 {@code (user, quest)} 即发放幂等键。</li>
 * </ul>
 *
 * <p><b>业务不变量：</b>每个 Adventurer 在每个 Quest 上至多发放一次——既由"Quest 通过审核即转
 * {@code COMPLETED}、阻断后续成果提交"的状态机天然保证，又以贡献记录是否已存在做兜底跳过。
 * {@code grantQuestCompletion} 须在 Maintainer 审核（{@code ReviewServiceImpl} 的 APPROVED 分支）的
 * 事务内被调用，使成长发放与审核结果原子提交，审核回滚则发放一并回滚。
 *
 * <p><b>边界错误模式：</b>正常路径不抛业务异常（重复发放静默跳过）；若幂等键被并发绕过，
 * DB 唯一约束兜底回滚，绝不二次加 XP。
 */
@Service
public class GrowthServiceImpl implements GrowthService {

    private static final String REASON_QUEST_COMPLETED = "QUEST_COMPLETED";

    private final GrowthProfileRepository growthProfileRepository;
    private final XpTransactionRepository xpTransactionRepository;
    private final ContributionRecordRepository contributionRecordRepository;

    public GrowthServiceImpl(
            GrowthProfileRepository growthProfileRepository,
            XpTransactionRepository xpTransactionRepository,
            ContributionRecordRepository contributionRecordRepository) {
        this.growthProfileRepository = growthProfileRepository;
        this.xpTransactionRepository = xpTransactionRepository;
        this.contributionRecordRepository = contributionRecordRepository;
    }

    @Override
    @Transactional
    public void grantQuestCompletion(User submitter, Quest quest) {
        Long userId = submitter.getUserId();
        // 幂等键：贡献记录已存在 = 已发放过，直接跳过（亦对应 DB UNIQUE user_id+quest_id）
        if (contributionRecordRepository.existsByUserUserIdAndQuestQuestId(userId, quest.getQuestId())) {
            return;
        }

        GrowthProfile profile = growthProfileRepository.findByUserUserId(userId)
                .orElseGet(() -> new GrowthProfile(submitter));
        profile.recordQuestCompletion(quest.getRewardXp());
        growthProfileRepository.save(profile);

        xpTransactionRepository.save(
                new XpTransaction(submitter, quest, quest.getRewardXp(), REASON_QUEST_COMPLETED));

        contributionRecordRepository.save(new ContributionRecord(
                submitter, quest, quest.getRepository(), quest.getTitle(), OffsetDateTime.now()));
    }

    @Override
    @Transactional(readOnly = true)
    public GrowthSummaryResponse getSummary(Long userId) {
        return growthProfileRepository.findByUserUserId(userId)
                .map(p -> new GrowthSummaryResponse(
                        p.getLevel(), p.getTotalXp(), p.nextLevelXp(), p.getCompletedQuestCount()))
                .orElseGet(() -> new GrowthSummaryResponse(1, 0, GrowthProfile.XP_PER_LEVEL, 0));
    }
}
