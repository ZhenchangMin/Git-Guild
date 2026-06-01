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
