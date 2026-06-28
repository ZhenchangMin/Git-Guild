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
import com.gitguild.backend.notification.domain.NotificationType;
import com.gitguild.backend.notification.service.NotificationService;
import com.gitguild.backend.quest.domain.Quest;
import com.gitguild.backend.user.domain.User;
import java.time.DayOfWeek;
import java.time.OffsetDateTime;
import java.time.temporal.TemporalAdjusters;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.function.ToIntFunction;
import java.util.stream.Collectors;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
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

    private static final Logger log = LoggerFactory.getLogger(GrowthServiceImpl.class);

    private static final String REASON_QUEST_COMPLETED = "QUEST_COMPLETED";
    private static final String PERIOD_WEEKLY = "WEEKLY";
    private static final String PERIOD_MONTHLY = "MONTHLY";
    private static final String PERIOD_ALL_TIME = "ALL_TIME";
    private static final int DEFAULT_LEVEL = 1;
    private static final List<TitleRule> TITLE_RULES = List.of(
            new TitleRule(12, "公会大师"),
            new TitleRule(8, "公会精英"),
            new TitleRule(5, "代码工匠"),
            new TitleRule(3, "前端协作学徒"),
            new TitleRule(2, "协作学徒"),
            new TitleRule(1, "见习冒险者"));

    /** 徽章规则的唯一来源：徽章展示（getBadges）与解锁通知（detect-on-completion）共用，避免阈值漂移。 */
    private static final List<BadgeRule> BADGE_RULES = List.of(
            new BadgeRule("FIRST_COMPLETION", "首次贡献", "完成第一个任务后获得",
                    "完成任务数量 >= 1", GrowthSnapshot::completedQuestCount, 1),
            new BadgeRule("XP_APPRENTICE", "XP 学徒", "累计 XP 达到 100 后获得",
                    "累计 XP >= 100", GrowthSnapshot::totalXp, 100),
            new BadgeRule("QUEST_EXPLORER", "任务探索者", "完成 3 个任务后获得",
                    "完成任务数量 >= 3", GrowthSnapshot::completedQuestCount, 3),
            new BadgeRule("LEVEL_RISER", "等级新星", "用户等级达到 3 后获得",
                    "用户等级 >= 3", GrowthSnapshot::level, 3));

    private final GrowthProfileRepository growthProfileRepository;
    private final XpTransactionRepository xpTransactionRepository;
    private final ContributionRecordRepository contributionRecordRepository;
    private final ObjectMapper objectMapper;
    private final NotificationService notificationService;

    public GrowthServiceImpl(
            GrowthProfileRepository growthProfileRepository,
            XpTransactionRepository xpTransactionRepository,
            ContributionRecordRepository contributionRecordRepository,
            ObjectMapper objectMapper,
            NotificationService notificationService) {
        this.growthProfileRepository = growthProfileRepository;
        this.xpTransactionRepository = xpTransactionRepository;
        this.contributionRecordRepository = contributionRecordRepository;
        this.objectMapper = objectMapper;
        this.notificationService = notificationService;
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
        // 本次发放前的快照：用于判断是否升级、是否解锁了新徽章。
        GrowthSnapshot before = snapshotOf(profile);
        profile.recordQuestCompletion(quest.getRewardXp());
        growthProfileRepository.save(profile);

        xpTransactionRepository.save(
                new XpTransaction(submitter, quest, quest.getRewardXp(), REASON_QUEST_COMPLETED));

        contributionRecordRepository.save(new ContributionRecord(
                submitter, quest, quest.getRepository(), quest.getTitle(), OffsetDateTime.now()));

        notifyGrowthMilestones(submitter, quest, before, snapshotOf(profile));
    }

    private GrowthSnapshot snapshotOf(GrowthProfile profile) {
        return new GrowthSnapshot(profile.getLevel(), profile.getTotalXp(), profile.getCompletedQuestCount());
    }

    /**
     * 完成任务后，比较前后快照，向冒险家祝贺「升级」与「解锁新徽章」。
     * best-effort：通知失败仅记日志，绝不回滚成长发放（与上层审核事务解耦）。
     */
    private void notifyGrowthMilestones(User user, Quest quest, GrowthSnapshot before, GrowthSnapshot after) {
        try {
            if (after.level() > before.level()) {
                notificationService.notify(user, NotificationType.LEVEL_UP,
                        String.format("恭喜！你升到了 Lv.%d，继续加油！", after.level()), null, null);
            }
            for (BadgeRule rule : BADGE_RULES) {
                if (!rule.earned(before) && rule.earned(after)) {
                    notificationService.notify(user, NotificationType.BADGE_UNLOCKED,
                            String.format("解锁新徽章「%s」：%s。", rule.name(), rule.description()), null, null);
                }
            }
        } catch (RuntimeException ex) {
            log.warn("发送成长里程碑通知失败 userId={}, questId={}", user.getUserId(), quest.getQuestId(), ex);
        }
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

        if (!PERIOD_ALL_TIME.equals(normalizedPeriod)) {
            return getPeriodLeaderboard(normalizedPeriod, limit);
        }

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
                    titleForLevel(profile.getLevel()),
                    profile.getTotalXp(),
                    profile.getCompletedQuestCount()));
        }

        return new LeaderboardResponse(normalizedPeriod, OffsetDateTime.now(), items);
    }

    private LeaderboardResponse getPeriodLeaderboard(String period, int limit) {
        OffsetDateTime start = periodStart(period);
        List<com.gitguild.backend.growth.repository.XpTransactionRepository.LeaderboardPeriodRow> rows =
                xpTransactionRepository.findLeaderboardSince(start, PageRequest.of(0, limit));
        if (rows.isEmpty()) {
            return new LeaderboardResponse(period, OffsetDateTime.now(), List.of());
        }
        List<Long> userIds = rows.stream()
                .map(com.gitguild.backend.growth.repository.XpTransactionRepository.LeaderboardPeriodRow::getUserId)
                .toList();
        Map<Long, GrowthProfile> profilesByUserId = growthProfileRepository.findByUserUserIdIn(userIds).stream()
                .collect(Collectors.toMap(profile -> profile.getUser().getUserId(), profile -> profile));

        List<LeaderboardResponse.LeaderboardItem> items = new ArrayList<>(rows.size());
        for (int i = 0; i < rows.size(); i++) {
            com.gitguild.backend.growth.repository.XpTransactionRepository.LeaderboardPeriodRow row = rows.get(i);
            GrowthProfile profile = profilesByUserId.get(row.getUserId());
            int level = profile == null ? levelFromXp(row.getTotalXp()) : profile.getLevel();
            items.add(new LeaderboardResponse.LeaderboardItem(
                    i + 1,
                    row.getUserId(),
                    row.getUsername(),
                    level,
                    titleForLevel(level),
                    safeInt(row.getTotalXp()),
                    safeInt(row.getCompletedQuestCount())));
        }
        return new LeaderboardResponse(period, OffsetDateTime.now(), items);
    }

    private OffsetDateTime periodStart(String period) {
        OffsetDateTime now = OffsetDateTime.now();
        if (PERIOD_WEEKLY.equals(period)) {
            return now.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY))
                    .toLocalDate()
                    .atStartOfDay()
                    .atOffset(now.getOffset());
        }
        if (PERIOD_MONTHLY.equals(period)) {
            return now.withDayOfMonth(1)
                    .toLocalDate()
                    .atStartOfDay()
                    .atOffset(now.getOffset());
        }
        return OffsetDateTime.MIN;
    }

    private int levelFromXp(long xp) {
        return (int) (xp / GrowthProfile.XP_PER_LEVEL) + 1;
    }

    private int safeInt(long value) {
        return value > Integer.MAX_VALUE ? Integer.MAX_VALUE : (int) value;
    }

    private String titleForLevel(int level) {
        for (TitleRule rule : TITLE_RULES) {
            if (level >= rule.minLevel()) {
                return rule.title();
            }
        }
        return "见习冒险者";
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

        return new BadgeResponse(BADGE_RULES.stream()
                .map(rule -> {
                    boolean earned = rule.earned(snapshot);
                    return new BadgeResponse.BadgeItem(
                            rule.id(),
                            rule.name(),
                            rule.description(),
                            rule.condition(),
                            earned,
                            earned ? firstContributionAt : null,
                            rule.metric().applyAsInt(snapshot),
                            rule.target());
                })
                .toList());
    }

    private String normalizePeriod(String period) {
        String normalized = period == null || period.isBlank()
                ? PERIOD_ALL_TIME
                : period.trim().toUpperCase(Locale.ROOT);
        if (!PERIOD_WEEKLY.equals(normalized)
                && !PERIOD_MONTHLY.equals(normalized)
                && !PERIOD_ALL_TIME.equals(normalized)) {
            throw new BusinessException(
                    "VALIDATION_FAILED",
                    HttpStatus.BAD_REQUEST,
                    "请求参数不合法",
                    "period only supports WEEKLY, MONTHLY, ALL_TIME");
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

    private record GrowthSnapshot(int level, int totalXp, int completedQuestCount) {
    }

    /** 徽章规则：阈值条件 + 进度取值器；earned() 判断在给定快照下是否达成。 */
    private record TitleRule(int minLevel, String title) {
    }

    private record BadgeRule(
            String id,
            String name,
            String description,
            String condition,
            ToIntFunction<GrowthSnapshot> metric,
            int target) {

        boolean earned(GrowthSnapshot snapshot) {
            return metric.applyAsInt(snapshot) >= target;
        }
    }
}
