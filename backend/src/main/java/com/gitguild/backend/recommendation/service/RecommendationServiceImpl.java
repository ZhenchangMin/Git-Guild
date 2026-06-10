package com.gitguild.backend.recommendation.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.gitguild.backend.common.BusinessException;
import com.gitguild.backend.growth.domain.ContributionRecord;
import com.gitguild.backend.growth.domain.GrowthProfile;
import com.gitguild.backend.growth.repository.ContributionRecordRepository;
import com.gitguild.backend.growth.repository.GrowthProfileRepository;
import com.gitguild.backend.quest.domain.Difficulty;
import com.gitguild.backend.quest.domain.Quest;
import com.gitguild.backend.quest.domain.QuestStatus;
import com.gitguild.backend.quest.repository.QuestRepository;
import com.gitguild.backend.recommendation.dto.RecommendationResponse;
import com.gitguild.backend.recommendation.dto.RecommendationResponse.RecommendationItem;
import com.gitguild.backend.recommendation.dto.RecommendationResponse.RecommendationItem.QuestBrief;
import com.gitguild.backend.recommendation.dto.RecommendationResponse.UserBrief;
import com.gitguild.backend.user.domain.User;
import com.gitguild.backend.user.repository.UserRepository;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * {@link RecommendationService} 的实现，基于 Adventurer 历史行为画像做规则推荐。
 *
 * <p><b>隐藏的复杂度：</b>
 * <ul>
 *   <li>Adventurer（{@code BEGINNER} 角色）没有自定义技术栈字段，画像从其已完成 Quest 的
 *       {@code techStackJson}（JSON 数组）反推技术栈频率和难度舒适区；</li>
 *   <li>推荐只影响 Quest Board 的排序和理由展示，不减少列表数量；用户已接取或已完成的 Quest
 *       仍参与打分，是否可再次接取由接取接口负责校验；</li>
 *   <li>{@code Difficulty} 枚举序数与打分映射相反（A 难度最高但 ordinal=0），打分数值用显式 switch 映射
 *       （A=4, B=3, C=2, D=1）。</li>
 * </ul>
 *
 * <p><b>业务不变量：</b>推荐覆盖 Quest Board 默认候选集 {@code PUBLISHED} ∪ {@code IN_PROGRESS}
 * （CONTEXT.md §115）。推荐算法只排序该候选集，不过滤候选项，不截断返回数量。
 *
 * <p><b>边界错误模式：</b>未知 strategy 抛 {@code STRATEGY_NOT_AVAILABLE}；
 * 用户不存在抛 {@code USER_NOT_FOUND}；techStackJson 解析失败记 warn 日志后退化为空列表
 * （不中断推荐）；新 Adventurer（无历史画像）techMatch=0，difficultyMatch=0.5，退化为难度中性排序。
 */
@Service
public class RecommendationServiceImpl implements RecommendationService {

    private static final Logger log = LoggerFactory.getLogger(RecommendationServiceImpl.class);
    private static final String DEFAULT_STRATEGY = "tech-difficulty";
    private static final double TECH_WEIGHT = 0.6;
    private static final double DIFFICULTY_WEIGHT = 0.4;
    private static final double STRONG_MATCH_THRESHOLD = 0.7;
    private static final double BEGINNER_BOOST = 0.2;
    private static final int MAX_DIFFICULTY_DELTA = 3;

    private final QuestRepository questRepository;
    private final ContributionRecordRepository contributionRecordRepository;
    private final GrowthProfileRepository growthProfileRepository;
    private final UserRepository userRepository;
    private final ObjectMapper objectMapper;

    public RecommendationServiceImpl(
            QuestRepository questRepository,
            ContributionRecordRepository contributionRecordRepository,
            GrowthProfileRepository growthProfileRepository,
            UserRepository userRepository,
            ObjectMapper objectMapper) {
        this.questRepository = questRepository;
        this.contributionRecordRepository = contributionRecordRepository;
        this.growthProfileRepository = growthProfileRepository;
        this.userRepository = userRepository;
        this.objectMapper = objectMapper;
    }

    @Override
    @Transactional(readOnly = true)
    public RecommendationResponse recommendQuests(Long userId, String strategy, boolean beginnerFriendly,
            boolean excludeAccepted, int limit) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new BusinessException("USER_NOT_FOUND", HttpStatus.NOT_FOUND,
                        "User not found", "userId=" + userId));
        String effectiveStrategy = strategy == null || strategy.isBlank() ? DEFAULT_STRATEGY : strategy;
        if (!DEFAULT_STRATEGY.equals(effectiveStrategy)) {
            throw new BusinessException("STRATEGY_NOT_AVAILABLE", HttpStatus.BAD_REQUEST,
                    "指定的推荐策略不存在或未启用", "strategy=" + effectiveStrategy);
        }
        GrowthProfile profile = growthProfileRepository.findByUserUserId(userId).orElse(null);
        int level = profile != null ? profile.getLevel() : 1;

        // 构建 Adventurer 画像：从已完成 Quest 反推技术栈偏好和难度舒适区
        List<ContributionRecord> records = contributionRecordRepository.findByUserUserId(userId);
        AdventurerProfile adventurerProfile = buildAdventurerProfile(records);

        // 候选集与 Quest Board 默认列表一致；推荐算法只负责打分排序，不减少展示数量。
        List<Quest> candidates = questRepository.findByStatusIn(
                List.of(QuestStatus.PUBLISHED, QuestStatus.IN_PROGRESS));

        // 打分 + 排序
        List<RecommendationItem> items = new ArrayList<>();
        for (Quest quest : candidates) {
            List<String> questTechs = parseTechStack(quest.getTechStackJson());
            int questDiff = difficultyValue(quest.getDifficulty());

            double techMatch = computeTechMatch(adventurerProfile, questTechs);
            double difficultyMatch = computeDifficultyMatch(adventurerProfile, questDiff);
            double beginnerBoost = 0.0;
            if (beginnerFriendly && level <= 2 && quest.getDifficulty() == Difficulty.D) {
                beginnerBoost = BEGINNER_BOOST;
            }
            double score = TECH_WEIGHT * techMatch + DIFFICULTY_WEIGHT * difficultyMatch + beginnerBoost;

            // 生成推荐理由
            List<String> reasons = buildReasons(techMatch, difficultyMatch, beginnerBoost > 0);

            items.add(new RecommendationItem(
                    new QuestBrief(quest.getQuestId(), quest.getTitle(),
                            quest.getDifficulty().name(), questTechs,
                            quest.getRewardXp(), quest.getRepository().getName()),
                    round2(score),
                    score >= STRONG_MATCH_THRESHOLD,
                    reasons));
        }
        items.sort(Comparator.comparingDouble(RecommendationItem::score).reversed());

        return new RecommendationResponse(
                new UserBrief(user.getUserId(), level, profile != null ? profile.getTotalXp() : 0),
                effectiveStrategy,
                OffsetDateTime.now(),
                items);
    }

    // ── 画像构建 ──────────────────────────────────────────────────────────

    /**
     * 从 Adventurer 的贡献记录反推其行为画像。
     *
     * <p><b>画像两维：</b>技术栈频率（{@code techFreq}）用于计算候选 Quest 的技术匹配度；
     * 难度均值（{@code avgDifficulty}）用于计算候选 Quest 的难度偏离度。
     * {@code completedQuestCount = 0} 即冷启动——后续打分函数以此为信号退化为中性排序。
     *
     * <p><b>边界：</b>{@code records} 为空时返回 avgDifficulty=0 的空画像，
     * 调用方以 completedQuestCount 判别冷启动，而非 avgDifficulty 是否为 0。
     *
     * <p><b>实现注：</b>每条 {@code r.getQuest()} 触发一次 LAZY 加载，
     * 当前在 {@code @Transactional(readOnly=true)} 内安全；生产量级可加 {@code @EntityGraph}。
     */
    private AdventurerProfile buildAdventurerProfile(List<ContributionRecord> records) {
        Map<String, Integer> techFreq = new HashMap<>();
        double totalDiff = 0;
        int count = 0;
        for (ContributionRecord r : records) {
            Quest quest = r.getQuest();
            for (String tech : parseTechStack(quest.getTechStackJson())) {
                techFreq.merge(tech, 1, Integer::sum);
            }
            totalDiff += difficultyValue(quest.getDifficulty());
            count++;
        }
        double avgDifficulty = count > 0 ? totalDiff / count : 0;
        return new AdventurerProfile(techFreq, avgDifficulty, count);
    }

    // ── 打分函数 ──────────────────────────────────────────────────────────

    /**
     * 技术栈匹配度（Jaccard 风格交集比）。
     *
     * <p>公式：{@code |画像技术 ∩ Quest 技术| / |Quest 技术|}。
     * 权重 0.6（{@link #TECH_WEIGHT}），是综合分中占比最高的维度，
     * 体现 CONTEXT.md "推荐应体现 Adventurer 已掌握技术栈" 的业务取向。
     *
     * <p><b>不变量：</b>返回值 ∈ [0, 1]。Quest 无技术栈或 Adventurer 冷启动时退化为 0——
     * 即冷启动下推荐排序由 difficultyMatch 主导，这符合"新手先按难度渐进"的产品逻辑。
     */
    private double computeTechMatch(AdventurerProfile profile, List<String> questTechs) {
        if (questTechs.isEmpty() || profile.completedQuestCount == 0) {
            return 0.0;
        }
        long matched = questTechs.stream().filter(profile.techFrequencies::containsKey).count();
        return (double) matched / questTechs.size();
    }

    /**
     * 难度匹配度（距离衰减）。
     *
     * <p>公式：{@code 1 − |questDiff − adventurerAvgDiff| / 3}。
     * 权重 0.4（{@link #DIFFICULTY_WEIGHT}）。A=4, B=3, C=2, D=1。
     *
     * <p><b>冷启动特殊值：</b>无历史画像时返回固定 0.5，使所有候选难度维度中性——
     * 既不偏向也不排斥任何难度，排序退化为 techMatch + beginnerBoost 主导。
     */
    private double computeDifficultyMatch(AdventurerProfile profile, int questDiff) {
        if (profile.completedQuestCount == 0) {
            return 0.5;
        }
        return 1.0 - (double) Math.abs(questDiff - profile.avgDifficulty) / MAX_DIFFICULTY_DELTA;
    }

    // ── 推荐理由 ──────────────────────────────────────────────────────────

    private List<String> buildReasons(double techMatch, double difficultyMatch, boolean beginnerBoost) {
        List<String> reasons = new ArrayList<>();
        if (techMatch >= 0.5) {
            reasons.add("技术栈匹配");
        }
        if (difficultyMatch >= 0.7) {
            reasons.add("难度适合你");
        }
        if (beginnerBoost) {
            reasons.add("新手友好");
        }
        if (reasons.isEmpty()) {
            reasons.add("推荐尝试");
        }
        return reasons;
    }

    // ── JSON 解析 ─────────────────────────────────────────────────────────

    private List<String> parseTechStack(String techStackJson) {
        if (techStackJson == null || techStackJson.isBlank()) {
            return List.of();
        }
        try {
            return objectMapper.readValue(techStackJson,
                    objectMapper.getTypeFactory().constructCollectionType(List.class, String.class));
        } catch (JsonProcessingException e) {
            log.warn("Failed to parse techStackJson: {}", techStackJson, e);
            return List.of();
        }
    }

    // ── Difficulty 数字映射 ──────────────────────────────────────────────

    /** A=4, B=3, C=2, D=1。序数与 {@link Difficulty#ordinal} 相反，此处显式映射。 */
    private static int difficultyValue(Difficulty d) {
        return switch (d) {
            case A -> 4;
            case B -> 3;
            case C -> 2;
            case D -> 1;
        };
    }

    private static double round2(double v) {
        return Math.round(v * 100.0) / 100.0;
    }

    /** Adventurer 画像：技术栈频率 + 难度均值 + 完成数（冷启动判别用）。 */
    private record AdventurerProfile(
            Map<String, Integer> techFrequencies,
            double avgDifficulty,
            int completedQuestCount) {
    }
}
