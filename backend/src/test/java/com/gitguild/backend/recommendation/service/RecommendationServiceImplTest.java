package com.gitguild.backend.recommendation.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.when;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.gitguild.backend.codehost.domain.CodeRepository;
import com.gitguild.backend.common.BusinessException;
import com.gitguild.backend.growth.domain.ContributionRecord;
import com.gitguild.backend.growth.domain.GrowthProfile;
import com.gitguild.backend.growth.repository.ContributionRecordRepository;
import com.gitguild.backend.growth.repository.GrowthProfileRepository;
import com.gitguild.backend.quest.domain.AssignmentStatus;
import com.gitguild.backend.quest.domain.Difficulty;
import com.gitguild.backend.quest.domain.Quest;
import com.gitguild.backend.quest.domain.QuestAssignment;
import com.gitguild.backend.quest.repository.QuestAssignmentRepository;
import com.gitguild.backend.quest.repository.QuestRepository;
import com.gitguild.backend.recommendation.dto.RecommendationResponse;
import com.gitguild.backend.user.domain.User;
import com.gitguild.backend.user.repository.UserRepository;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

/**
 * 推荐引擎聚焦单测：策略校验、候选过滤、新手 boost、strongMatch 阈值、limit 截断。
 */
@ExtendWith(MockitoExtension.class)
class RecommendationServiceImplTest {

    @Mock private QuestRepository questRepository;
    @Mock private QuestAssignmentRepository assignmentRepository;
    @Mock private ContributionRecordRepository contributionRecordRepository;
    @Mock private GrowthProfileRepository growthProfileRepository;
    @Mock private UserRepository userRepository;
    private final ObjectMapper objectMapper = new ObjectMapper();

    private RecommendationServiceImpl service;

    @BeforeEach
    void setUp() {
        service = new RecommendationServiceImpl(questRepository, assignmentRepository,
                contributionRecordRepository, growthProfileRepository, userRepository, objectMapper);
    }

    @Test
    void rejectsUnknownStrategy() {
        User u = mockUser(3001L);
        when(userRepository.findById(3001L)).thenReturn(Optional.of(u));

        assertThatThrownBy(() -> service.recommendQuests(3001L, "unknown-strategy", true, true, 5))
                .isInstanceOf(BusinessException.class)
                .extracting("code")
                .isEqualTo("STRATEGY_NOT_AVAILABLE");
    }

    @Test
    void defaultsToTechDifficultyWhenStrategyIsNull() {
        User u = mockUser(3001L);
        when(userRepository.findById(3001L)).thenReturn(Optional.of(u));
        when(growthProfileRepository.findByUserUserId(3001L)).thenReturn(Optional.empty());
        when(contributionRecordRepository.findByUserUserId(3001L)).thenReturn(List.of());
        when(questRepository.findByStatusIn(any())).thenReturn(List.of());
        when(assignmentRepository.findByAssigneeUserIdAndStatus(3001L, AssignmentStatus.ACTIVE))
                .thenReturn(List.of());

        RecommendationResponse result = service.recommendQuests(3001L, null, true, true, 5);

        assertThat(result.strategy()).isEqualTo("tech-difficulty");
        assertThat(result.items()).isEmpty();
    }

    @Test
    void filtersOutActivelyAssignedAndCompletedQuests() {
        User u = mockUser(3001L);
        CodeRepository repo = mockRepo();
        Quest q1 = mockQuest(7001L, Difficulty.B, "[\"Java\"]", repo);
        Quest q2 = mockQuest(7002L, Difficulty.C, "[\"Python\"]", repo);
        Quest q3 = mockQuest(7003L, Difficulty.A, "[\"Go\"]", repo);
        // q3 is the only candidate that makes it to the result — its title is read
        lenient().when(q3.getTitle()).thenReturn("Go Task");

        QuestAssignment active = new QuestAssignment(q1, u);
        ContributionRecord completed = new ContributionRecord(u, q2, repo, "done", OffsetDateTime.now());

        when(userRepository.findById(3001L)).thenReturn(Optional.of(u));
        when(growthProfileRepository.findByUserUserId(3001L)).thenReturn(Optional.empty());
        when(contributionRecordRepository.findByUserUserId(3001L)).thenReturn(List.of(completed));
        when(questRepository.findByStatusIn(any())).thenReturn(List.of(q1, q2, q3));
        when(assignmentRepository.findByAssigneeUserIdAndStatus(3001L, AssignmentStatus.ACTIVE))
                .thenReturn(List.of(active));

        RecommendationResponse result = service.recommendQuests(3001L, "tech-difficulty", true, true, 5);

        assertThat(result.items()).hasSize(1);
        assertThat(result.items().get(0).quest().questId()).isEqualTo(7003L);
    }

    @Test
    void appliesBeginnerBoostForLowLevelOnDifficultyD() {
        User u = mockUser(3001L);
        CodeRepository repo = mockRepo();
        Quest q = mockQuest(7001L, Difficulty.D, "[\"Markdown\"]", repo);
        when(q.getTitle()).thenReturn("Easy Task");
        GrowthProfile profile = new GrowthProfile(u);

        when(userRepository.findById(3001L)).thenReturn(Optional.of(u));
        when(growthProfileRepository.findByUserUserId(3001L)).thenReturn(Optional.of(profile));
        when(contributionRecordRepository.findByUserUserId(3001L)).thenReturn(List.of());
        when(questRepository.findByStatusIn(any())).thenReturn(List.of(q));
        when(assignmentRepository.findByAssigneeUserIdAndStatus(3001L, AssignmentStatus.ACTIVE))
                .thenReturn(List.of());

        RecommendationResponse result = service.recommendQuests(3001L, "tech-difficulty", true, true, 5);

        assertThat(result.items()).hasSize(1);
        assertThat(result.items().get(0).reasons()).contains("新手友好");
    }

    @Test
    void marksStrongMatchWhenScoreAboveThreshold() {
        User u = mockUser(3001L);
        CodeRepository repo = mockRepo();
        Quest candidate = mockQuest(7001L, Difficulty.A, "[\"Java\",\"Spring\"]", repo);
        when(candidate.getTitle()).thenReturn("Advanced Java");
        Quest historyQuest = mockQuest(8001L, Difficulty.A, "[\"Java\",\"Spring\",\"Vue\"]", repo);
        ContributionRecord record = new ContributionRecord(u, historyQuest, repo, "old", OffsetDateTime.now());

        when(userRepository.findById(3001L)).thenReturn(Optional.of(u));
        when(growthProfileRepository.findByUserUserId(3001L)).thenReturn(Optional.empty());
        when(contributionRecordRepository.findByUserUserId(3001L)).thenReturn(List.of(record));
        when(questRepository.findByStatusIn(any())).thenReturn(List.of(candidate));
        when(assignmentRepository.findByAssigneeUserIdAndStatus(3001L, AssignmentStatus.ACTIVE))
                .thenReturn(List.of());

        RecommendationResponse result = service.recommendQuests(3001L, "tech-difficulty", true, true, 5);

        assertThat(result.items()).hasSize(1);
        assertThat(result.items().get(0).strongMatch()).isTrue();
        assertThat(result.items().get(0).score()).isEqualTo(1.0);
    }

    @Test
    void respectsLimit() {
        User u = mockUser(3001L);
        CodeRepository repo = mockRepo();
        List<Quest> quests = java.util.stream.IntStream.rangeClosed(1, 10)
                .mapToObj(i -> {
                    Quest q = mockQuest((long) (7000 + i), Difficulty.B, "[\"Java\"]", repo);
                    lenient().when(q.getTitle()).thenReturn("Q" + i);
                    return q;
                })
                .toList();

        when(userRepository.findById(3001L)).thenReturn(Optional.of(u));
        when(growthProfileRepository.findByUserUserId(3001L)).thenReturn(Optional.empty());
        when(contributionRecordRepository.findByUserUserId(3001L)).thenReturn(List.of());
        when(questRepository.findByStatusIn(any())).thenReturn(quests);
        when(assignmentRepository.findByAssigneeUserIdAndStatus(3001L, AssignmentStatus.ACTIVE))
                .thenReturn(List.of());

        RecommendationResponse result = service.recommendQuests(3001L, "tech-difficulty", true, true, 3);

        assertThat(result.items()).hasSize(3);
    }

    // ── helpers ──────────────────────────────────────────────────────────

    private User mockUser(Long id) {
        User u = org.mockito.Mockito.mock(User.class);
        lenient().when(u.getUserId()).thenReturn(id);
        return u;
    }

    private CodeRepository mockRepo() {
        CodeRepository r = org.mockito.Mockito.mock(CodeRepository.class);
        when(r.getName()).thenReturn("demo-repo");
        return r;
    }

    /** 最小 Quest mock：只桩推荐引擎必调的 getter。title 由调用方按需桩（lenient 或显式）。 */
    private Quest mockQuest(Long id, Difficulty difficulty, String techStackJson, CodeRepository repo) {
        Quest q = org.mockito.Mockito.mock(Quest.class);
        when(q.getQuestId()).thenReturn(id);
        lenient().when(q.getDifficulty()).thenReturn(difficulty);
        lenient().when(q.getTechStackJson()).thenReturn(techStackJson);
        lenient().when(q.getRewardXp()).thenReturn(100);
        lenient().when(q.getRepository()).thenReturn(repo);
        return q;
    }
}
