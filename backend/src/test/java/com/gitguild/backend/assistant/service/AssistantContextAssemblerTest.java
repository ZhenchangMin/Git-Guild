package com.gitguild.backend.assistant.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.mockito.ArgumentMatchers.any;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.gitguild.backend.growth.domain.ContributionRecord;
import com.gitguild.backend.growth.domain.GrowthProfile;
import com.gitguild.backend.growth.repository.ContributionRecordRepository;
import com.gitguild.backend.growth.repository.GrowthProfileRepository;
import com.gitguild.backend.quest.domain.Difficulty;
import com.gitguild.backend.quest.domain.AssignmentStatus;
import com.gitguild.backend.quest.domain.Quest;
import com.gitguild.backend.quest.domain.QuestAssignment;
import com.gitguild.backend.quest.domain.QuestStatus;
import com.gitguild.backend.quest.repository.QuestAssignmentRepository;
import com.gitguild.backend.quest.repository.QuestRepository;
import com.gitguild.backend.review.domain.Submission;
import com.gitguild.backend.review.domain.SubmissionStatus;
import com.gitguild.backend.review.repository.SubmissionRepository;
import com.gitguild.backend.security.CurrentUserPrincipal;
import com.gitguild.backend.user.domain.User;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.security.authentication.TestingAuthenticationToken;

class AssistantContextAssemblerTest {

    private QuestRepository questRepository;
    private QuestAssignmentRepository assignmentRepository;
    private SubmissionRepository submissionRepository;
    private GrowthProfileRepository growthProfileRepository;
    private ContributionRecordRepository contributionRecordRepository;
    private AssistantContextAssembler assembler;

    @BeforeEach
    void setUp() {
        questRepository = mock(QuestRepository.class);
        assignmentRepository = mock(QuestAssignmentRepository.class);
        submissionRepository = mock(SubmissionRepository.class);
        growthProfileRepository = mock(GrowthProfileRepository.class);
        contributionRecordRepository = mock(ContributionRecordRepository.class);
        when(questRepository.findByStatusIn(any())).thenReturn(List.of());
        assembler = new AssistantContextAssembler(
                questRepository,
                assignmentRepository,
                submissionRepository,
                growthProfileRepository,
                contributionRecordRepository,
                new ObjectMapper());
    }

    @Test
    void shouldAttachPublicQuestBoardCandidatesForAnonymousVisitor() {
        Quest quest = quest(301L, "整理 README", QuestStatus.PUBLISHED, Difficulty.D, "[\"docs\"]", 40);
        when(questRepository.findByStatusIn(any())).thenReturn(List.of(quest));

        AssistantChatContext context = assembler.assemble("工作台在哪？", "hall", null);

        assertThat(context.authenticated()).isFalse();
        assertThat(context.userProfile()).isNull();
        assertThat(context.questBoardCandidates()).hasSize(1);
        assertThat(context.questBoardCandidates().get(0).questId()).isEqualTo(301L);
        assertThat(context.questStatuses()).isEmpty();
        assertThat(context.submissionStatuses()).isEmpty();
        verifyNoInteractions(assignmentRepository, submissionRepository, growthProfileRepository, contributionRecordRepository);
    }

    @Test
    void shouldAttachAdventurerAssignmentsAndOwnSubmissions() {
        Quest activeQuest = quest(101L, "修复登录 bug", QuestStatus.IN_PROGRESS, Difficulty.C, "[\"java\"]", 80);
        QuestAssignment assignment = mock(QuestAssignment.class);
        when(assignment.getQuest()).thenReturn(activeQuest);
        when(assignment.getStatus()).thenReturn(AssignmentStatus.ACTIVE);
        Submission submission = submission(501L, activeQuest, SubmissionStatus.PENDING_REVIEW);
        GrowthProfile profile = growthProfile(2, 120, 1);
        ContributionRecord record = contributionRecord(activeQuest);
        Quest javaCandidate = quest(301L, "修复 Java 接口", QuestStatus.PUBLISHED, Difficulty.C, "[\"java\"]", 100);
        Quest cssCandidate = quest(302L, "编写 CSS 样式", QuestStatus.PUBLISHED, Difficulty.D, "[\"css\"]", 40);
        when(assignmentRepository.findByAssigneeUserIdAndStatusWithQuestOrderByAcceptedAtDesc(3001L, AssignmentStatus.ACTIVE))
                .thenReturn(List.of(assignment));
        when(submissionRepository.findBySubmitterUserIdWithQuestOrderBySubmittedAtDesc(3001L))
                .thenReturn(List.of(submission));
        when(growthProfileRepository.findByUserUserId(3001L)).thenReturn(java.util.Optional.of(profile));
        when(contributionRecordRepository.findByUserUserId(3001L)).thenReturn(List.of(record));
        when(questRepository.findByStatusIn(any())).thenReturn(List.of(javaCandidate, cssCandidate));

        AssistantChatContext context = assembler.assemble(
                "我的任务到哪一步了？",
                "adventurer-workbench",
                authentication(3001L, "ROLE_BEGINNER"));

        assertThat(context.roleLabel()).isEqualTo("冒险家");
        assertThat(context.userProfile()).isNotNull();
        assertThat(context.userProfile().level()).isEqualTo(2);
        assertThat(context.userProfile().preferredTechStacks()).containsExactly("java");
        assertThat(context.questBoardCandidates()).hasSize(2);
        assertThat(context.questBoardCandidates().get(0).questId()).isEqualTo(301L);
        assertThat(context.questBoardCandidates().get(0).reasons()).contains("技术栈匹配");
        assertThat(context.questStatuses()).hasSize(1);
        assertThat(context.questStatuses().get(0).questId()).isEqualTo(101L);
        assertThat(context.questStatuses().get(0).status()).isEqualTo("quest=IN_PROGRESS,assignment=ACTIVE");
        assertThat(context.submissionStatuses()).hasSize(1);
        assertThat(context.submissionStatuses().get(0).submissionId()).isEqualTo(501L);
    }

    @Test
    void shouldAttachMaintainerPublishedQuestsAndReviewableSubmissions() {
        Quest publishedQuest = quest(201L, "发布暗色主题", QuestStatus.PUBLISHED, Difficulty.B, "[\"vue\"]", 120);
        Submission submission = submission(601L, publishedQuest, SubmissionStatus.CHANGES_REQUESTED);
        when(questRepository.findByPublisherUserIdOrderByCreatedAtDesc(2001L)).thenReturn(List.of(publishedQuest));
        when(submissionRepository.findReviewQueueForReviewer(2001L, false)).thenReturn(List.of(submission));

        AssistantChatContext context = assembler.assemble(
                "我有哪些待处理任务？",
                "maintainer-workbench",
                authentication(2001L, "ROLE_MAINTAINER"));

        assertThat(context.roleLabel()).isEqualTo("委托人");
        assertThat(context.questStatuses()).hasSize(1);
        assertThat(context.questStatuses().get(0).status()).isEqualTo("PUBLISHED");
        assertThat(context.submissionStatuses()).hasSize(1);
        assertThat(context.submissionStatuses().get(0).status()).isEqualTo("CHANGES_REQUESTED");
        verifyNoInteractions(assignmentRepository);
    }

    @Test
    void shouldNotAttachAdminWideDataForFrontendAssistant() {
        AssistantChatContext context = assembler.assemble(
                "查看所有任务",
                "hall",
                authentication(1L, "ROLE_ADMIN"));

        assertThat(context.roleLabel()).isEqualTo("管理员");
        assertThat(context.questBoardCandidates()).isEmpty();
        assertThat(context.questStatuses()).isEmpty();
        assertThat(context.submissionStatuses()).isEmpty();
        verifyNoInteractions(questRepository, assignmentRepository, submissionRepository, growthProfileRepository, contributionRecordRepository);
    }

    private Quest quest(Long questId, String title, QuestStatus status, Difficulty difficulty, String techStackJson, int rewardXp) {
        Quest quest = mock(Quest.class);
        when(quest.getQuestId()).thenReturn(questId);
        when(quest.getTitle()).thenReturn(title);
        when(quest.getStatus()).thenReturn(status);
        when(quest.getDifficulty()).thenReturn(difficulty);
        when(quest.getTechStackJson()).thenReturn(techStackJson);
        when(quest.getRewardXp()).thenReturn(rewardXp);
        return quest;
    }

    private Submission submission(Long submissionId, Quest quest, SubmissionStatus status) {
        Submission submission = mock(Submission.class);
        when(submission.getSubmissionId()).thenReturn(submissionId);
        when(submission.getQuest()).thenReturn(quest);
        when(submission.getStatus()).thenReturn(status);
        return submission;
    }

    private GrowthProfile growthProfile(int level, int totalXp, int completedQuestCount) {
        GrowthProfile profile = mock(GrowthProfile.class);
        when(profile.getLevel()).thenReturn(level);
        when(profile.getTotalXp()).thenReturn(totalXp);
        when(profile.getCompletedQuestCount()).thenReturn(completedQuestCount);
        return profile;
    }

    private ContributionRecord contributionRecord(Quest quest) {
        ContributionRecord record = mock(ContributionRecord.class);
        when(record.getQuest()).thenReturn(quest);
        return record;
    }

    private TestingAuthenticationToken authentication(Long userId, String role) {
        return new TestingAuthenticationToken(new CurrentUserPrincipal(userId, List.of(role), 0), null);
    }
}
