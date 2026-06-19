package com.gitguild.backend.growth.service;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.gitguild.backend.common.BusinessException;
import com.gitguild.backend.growth.domain.ContributionRecord;
import com.gitguild.backend.growth.domain.GrowthProfile;
import com.gitguild.backend.growth.domain.XpTransaction;
import com.gitguild.backend.growth.dto.BadgeResponse;
import com.gitguild.backend.growth.dto.ContributionResponse;
import com.gitguild.backend.growth.dto.GrowthSummaryResponse;
import com.gitguild.backend.growth.dto.LeaderboardResponse;
import com.gitguild.backend.growth.repository.ContributionRecordRepository;
import com.gitguild.backend.growth.repository.GrowthProfileRepository;
import com.gitguild.backend.growth.repository.XpTransactionRepository;
import com.gitguild.backend.quest.domain.Quest;
import com.gitguild.backend.user.domain.User;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.HttpStatus;
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
    private static final String PERIOD_ALL_TIME = "ALL_TIME";
    private static final int DEFAULT_LEVEL = 1;

    private final GrowthProfileRepository growthProfileRepository;
    private final XpTransactionRepository xpTransactionRepository;
    private final ContributionRecordRepository contributionRecordRepository;
    private final ObjectMapper objectMapper;

    public GrowthServiceImpl(
            GrowthProfileRepository growthProfileRepository,
            XpTransactionRepository xpTransactionRepository,
            ContributionRecordRepository contributionRecordRepository,
            ObjectMapper objectMapper) {
        this.growthProfileRepository = growthProfileRepository;
        this.xpTransactionRepository = xpTransactionRepository;
        this.contributionRecordRepository = contributionRecordRepository;
        this.objectMapper = objectMapper;
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

    @Override
    @Transactional(readOnly = true)
    public LeaderboardResponse getXpLeaderboard(String period, int limit) {
        String normalizedPeriod = normalizePeriod(period);
        validateLimit(limit);

        List<GrowthProfile> profiles = growthProfileRepository
                .findByOrderByTotalXpDescCompletedQuestCountDescUserUserIdAsc(PageRequest.of(0, limit));

        List<LeaderboardResponse.LeaderboardItem> items = new ArrayList<>(profiles.size());
        for (int i = 0; i < profiles.size(); i++) {
            GrowthProfile profile = profiles.get(i);
            User user = profile.getUser();
            items.add(new LeaderboardResponse.LeaderboardItem(
                    i + 1,
                    user.getUserId(),
                    user.getUsername(),
                    profile.getLevel(),
                    profile.getTotalXp(),
                    profile.getCompletedQuestCount()));
        }

        return new LeaderboardResponse(normalizedPeriod, OffsetDateTime.now(), items);
    }

    @Override
    @Transactional(readOnly = true)
    public ContributionResponse getContributions(Long userId) {
        List<ContributionRecord> records =
                contributionRecordRepository.findByUserUserIdOrderByCompletedAtDesc(userId);

        List<ContributionResponse.ContributionItem> items = new ArrayList<>(records.size());
        for (ContributionRecord record : records) {
            Quest quest = record.getQuest();
            items.add(new ContributionResponse.ContributionItem(
                    record.getRecordId(),
                    quest.getQuestId(),
                    quest.getTitle(),
                    record.getRepository().getName(),
                    String.valueOf(quest.getDifficulty()),
                    quest.getRewardXp(),
                    record.getCompletedAt(),
                    record.getSummary(),
                    parseTechStack(quest.getTechStackJson())));
        }

        long repoCount = records.stream()
                .map(record -> record.getRepository().getRepositoryId())
                .distinct()
                .count();

        return new ContributionResponse(items, (int) repoCount);
    }

    // 技能图谱要按真实技术栈聚合，不能再用仓库名顶替——解析失败（空/脏数据）时返回空列表，
    // 前端据此跳过该贡献的技能计数，而不是抛出异常影响整页加载。
    private List<String> parseTechStack(String techStackJson) {
        if (techStackJson == null || techStackJson.isBlank()) {
            return List.of();
        }
        try {
            return objectMapper.readValue(techStackJson, new TypeReference<List<String>>() {
            });
        } catch (Exception ex) {
            return List.of();
        }
    }

    @Override
    @Transactional(readOnly = true)
    public BadgeResponse getBadges(Long userId) {
        GrowthSnapshot snapshot = growthProfileRepository.findByUserUserId(userId)
                .map(p -> new GrowthSnapshot(p.getLevel(), p.getTotalXp(), p.getCompletedQuestCount()))
                .orElseGet(() -> new GrowthSnapshot(DEFAULT_LEVEL, 0, 0));
        OffsetDateTime firstContributionAt = contributionRecordRepository
                .findFirstByUserUserIdOrderByCompletedAtAsc(userId)
                .map(ContributionRecord::getCompletedAt)
                .orElse(null);

        return new BadgeResponse(List.of(
                badge("FIRST_COMPLETION", "首次贡献", "完成第一个任务后获得",
                        "完成任务数量 >= 1", snapshot.completedQuestCount(), 1, firstContributionAt),
                badge("XP_APPRENTICE", "XP 学徒", "累计 XP 达到 100 后获得",
                        "累计 XP >= 100", snapshot.totalXp(), 100, firstContributionAt),
                badge("QUEST_EXPLORER", "任务探索者", "完成 3 个任务后获得",
                        "完成任务数量 >= 3", snapshot.completedQuestCount(), 3, firstContributionAt),
                badge("LEVEL_RISER", "等级新星", "用户等级达到 3 后获得",
                        "用户等级 >= 3", snapshot.level(), 3, firstContributionAt)));
    }

    private String normalizePeriod(String period) {
        String normalized = period == null || period.isBlank()
                ? PERIOD_ALL_TIME
                : period.trim().toUpperCase(Locale.ROOT);
        if (!PERIOD_ALL_TIME.equals(normalized)) {
            throw new BusinessException(
                    "VALIDATION_FAILED",
                    HttpStatus.BAD_REQUEST,
                    "请求参数不合法",
                    "period only supports ALL_TIME");
        }
        return normalized;
    }

    private void validateLimit(int limit) {
        if (limit < 1 || limit > 100) {
            throw new BusinessException(
                    "VALIDATION_FAILED",
                    HttpStatus.BAD_REQUEST,
                    "请求参数不合法",
                    "limit must be between 1 and 100");
        }
    }

    private BadgeResponse.BadgeItem badge(
            String badgeId,
            String name,
            String description,
            String condition,
            int progress,
            int target,
            OffsetDateTime firstContributionAt) {
        boolean earned = progress >= target;
        return new BadgeResponse.BadgeItem(
                badgeId,
                name,
                description,
                condition,
                earned,
                earned ? firstContributionAt : null,
                progress,
                target);
    }

    private record GrowthSnapshot(int level, int totalXp, int completedQuestCount) {
    }
}
