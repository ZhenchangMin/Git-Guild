package com.gitguild.backend.recommendation.controller;

import com.gitguild.backend.common.ApiResponse;
import com.gitguild.backend.common.BusinessException;
import com.gitguild.backend.growth.domain.GrowthProfile;
import com.gitguild.backend.growth.repository.GrowthProfileRepository;
import com.gitguild.backend.quest.domain.Quest;
import com.gitguild.backend.quest.repository.QuestRepository;
import com.gitguild.backend.recommendation.dto.RecommendationResponse;
import com.gitguild.backend.recommendation.service.RecommendationService;
import com.gitguild.backend.security.SecurityPrincipalUtils;
import com.gitguild.backend.user.domain.User;
import com.gitguild.backend.user.repository.UserRepository;
import java.util.Comparator;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * 推荐任务端点（P2，契约 §74）。
 *
 * <p>Adventurer 从悬赏任务板打开推荐视图时调用此端点。Controller 本身无业务逻辑——
 * 身份从 JWT 解析（不信任前端传入的 userId）、参数校验委托 Spring MVC、推荐逻辑全部在
 * {@link RecommendationService} 中。
 *
 * <p><b>暴露的功能：</b>一个 GET 端点，返回按匹配分降序排列的推荐 Quest 列表，
 * 每条含 {@code score} / {@code strongMatch} / {@code reasons[]}，前端可直接渲染为推荐卡片。
 *
 * <p><b>边界错误模式：</b>未登录由 SecurityConfig 拦截返回 401；
 * 未知 strategy 由 Service 层抛 {@code STRATEGY_NOT_AVAILABLE}（HTTP 400）；
 * 用户不存在抛 {@code USER_NOT_FOUND}（HTTP 404）。
 */
@RestController
@RequestMapping("/api/v1/recommendations")
public class RecommendationController {

    private final RecommendationService recommendationService;
    private final QuestRepository questRepository;
    private final UserRepository userRepository;
    private final GrowthProfileRepository growthProfileRepository;

    public RecommendationController(
            RecommendationService recommendationService,
            QuestRepository questRepository,
            UserRepository userRepository,
            GrowthProfileRepository growthProfileRepository) {
        this.recommendationService = recommendationService;
        this.questRepository = questRepository;
        this.userRepository = userRepository;
        this.growthProfileRepository = growthProfileRepository;
    }

    @GetMapping("/quests")
    public ApiResponse<RecommendationResponse> recommendQuests(
            Authentication authentication,
            @RequestParam(defaultValue = "tech-difficulty") String strategy,
            @RequestParam(defaultValue = "true") boolean beginnerFriendly,
            @RequestParam(defaultValue = "true") boolean excludeAccepted,
            @RequestParam(defaultValue = "5") int limit) {
        return ApiResponse.success(recommendationService.recommendQuests(
                SecurityPrincipalUtils.currentUserId(authentication),
                strategy, beginnerFriendly, excludeAccepted, limit));
    }

    @GetMapping("/quests/{questId}/contributors")
    public ApiResponse<ContributorRecommendationResponse> recommendContributors(
            @PathVariable Long questId,
            @RequestParam(defaultValue = "5") int limit) {
        if (limit < 1 || limit > 100) {
            throw validation("limit must be between 1 and 100");
        }
        Quest quest = questRepository.findById(questId)
                .orElseThrow(() -> new BusinessException("QUEST_NOT_FOUND", HttpStatus.NOT_FOUND, "任务不存在"));
        List<ContributorCandidate> items = growthProfileRepository.findAll().stream()
                .sorted(Comparator.comparing(GrowthProfile::getTotalXp).reversed())
                .limit(limit)
                .map(profile -> new ContributorCandidate(
                        profile.getUser().getUserId(),
                        profile.getUser().getUsername(),
                        profile.getLevel(),
                        profile.getTotalXp(),
                        List.of("历史 XP 较高", "适合任务：" + quest.getTitle())))
                .toList();
        return ApiResponse.success(new ContributorRecommendationResponse(questId, items));
    }

    @GetMapping("/reasons")
    public ApiResponse<RecommendationReasonResponse> getReasons(
            Authentication authentication,
            @RequestParam Long questId) {
        Long userId = SecurityPrincipalUtils.currentUserId(authentication);
        Quest quest = questRepository.findById(questId)
                .orElseThrow(() -> new BusinessException("QUEST_NOT_FOUND", HttpStatus.NOT_FOUND, "任务不存在"));
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new BusinessException("USER_NOT_FOUND", HttpStatus.NOT_FOUND, "用户不存在"));
        GrowthProfile profile = growthProfileRepository.findByUserUserId(userId).orElse(null);
        int totalXp = profile == null ? 0 : profile.getTotalXp();
        List<String> reasons = totalXp == 0
                ? List.of("你还没有完成记录，可以从该任务开始积累贡献")
                : List.of("你的累计 XP 为 " + totalXp, "该任务可继续提升你的成长档案");
        return ApiResponse.success(new RecommendationReasonResponse(quest.getQuestId(), user.getUserId(), reasons));
    }

    private BusinessException validation(String details) {
        return new BusinessException("VALIDATION_FAILED", HttpStatus.BAD_REQUEST, "请求参数不合法", details);
    }

    public record ContributorRecommendationResponse(Long questId, List<ContributorCandidate> items) {
    }

    public record ContributorCandidate(Long userId, String username, int level, int totalXp, List<String> reasons) {
    }

    public record RecommendationReasonResponse(Long questId, Long userId, List<String> reasons) {
    }
}
