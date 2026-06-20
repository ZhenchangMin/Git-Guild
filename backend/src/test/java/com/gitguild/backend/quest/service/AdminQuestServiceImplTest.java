package com.gitguild.backend.quest.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.gitguild.backend.codehost.domain.CodeIssue;
import com.gitguild.backend.codehost.domain.CodeRepository;
import com.gitguild.backend.common.BusinessException;
import com.gitguild.backend.quest.domain.AdminDecision;
import com.gitguild.backend.quest.domain.AdminReviewRecord;
import com.gitguild.backend.quest.domain.AssignmentStatus;
import com.gitguild.backend.quest.domain.Difficulty;
import com.gitguild.backend.quest.domain.Quest;
import com.gitguild.backend.quest.domain.QuestAssignment;
import com.gitguild.backend.quest.domain.QuestCategory;
import com.gitguild.backend.quest.domain.QuestStatus;
import com.gitguild.backend.quest.domain.QuestTechStack;
import com.gitguild.backend.quest.dto.AdminReviewRequest;
import com.gitguild.backend.quest.dto.ChecklistItemDto;
import com.gitguild.backend.quest.dto.QuestResponses.AdminQuestPageResponse;
import com.gitguild.backend.quest.dto.QuestResponses.AdminReviewResponse;
import com.gitguild.backend.quest.repository.AdminReviewRecordRepository;
import com.gitguild.backend.quest.repository.QuestAssignmentRepository;
import com.gitguild.backend.quest.repository.QuestRepository;
import com.gitguild.backend.quest.repository.QuestTechStackRepository;
import com.gitguild.backend.user.domain.User;
import com.gitguild.backend.user.domain.UserRole;
import com.gitguild.backend.user.repository.UserRepository;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;

@ExtendWith(MockitoExtension.class)
class AdminQuestServiceImplTest {

    @Mock
    private QuestRepository questRepository;
    @Mock
    private AdminReviewRecordRepository reviewRecordRepository;
    @Mock
    private QuestAssignmentRepository assignmentRepository;
    @Mock
    private UserRepository userRepository;
    @Mock
    private QuestTechStackRepository techStackRepository;
    @Mock
    private com.gitguild.backend.notification.service.NotificationService notificationService;

    private AdminQuestServiceImpl adminQuestService;

    @BeforeEach
    void setUp() {
        adminQuestService = new AdminQuestServiceImpl(
                questRepository,
                reviewRecordRepository,
                assignmentRepository,
                userRepository,
                techStackRepository,
                new ObjectMapper(),
                notificationService);
    }

    /** APPROVE_PUBLISH 测试默认前提：委托技术栈词表里的所有项均已登记（标准写法与委托一致）。 */
    private void stubAllTechStacksRegistered(Quest quest) {
        for (String name : techStackOf(quest)) {
            when(techStackRepository.findByNameIgnoreCase(name))
                    .thenReturn(Optional.of(new QuestTechStack(name)));
        }
    }

    private List<String> techStackOf(Quest quest) {
        try {
            return new ObjectMapper().readValue(quest.getTechStackJson(), new com.fasterxml.jackson.core.type.TypeReference<List<String>>() { });
        } catch (Exception ex) {
            throw new RuntimeException(ex);
        }
    }

    private List<ChecklistItemDto> fullPassingChecklist() {
        List<ChecklistItemDto> checklist = new ArrayList<>();
        for (int i = 0; i < 6; i++) {
            checklist.add(new ChecklistItemDto("check-" + i, true));
        }
        return checklist;
    }

    @Test
    void listQuestsWithSpecificStatusShouldQueryThatStatusWithSafePagination() {
        User maintainer = user(2001L, UserRole.MAINTAINER);
        Quest quest = quest(maintainer, QuestStatus.PENDING_ADMIN_REVIEW);
        when(questRepository.findByStatus(any(), any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of(quest)));

        AdminQuestPageResponse response = adminQuestService.listQuests("PENDING_ADMIN_REVIEW", 0, 100);

        assertThat(response.items()).hasSize(1);
        assertThat(response.items().get(0).questId()).isEqualTo(5001L);
        assertThat(response.items().get(0).status()).isEqualTo(QuestStatus.PENDING_ADMIN_REVIEW);
        assertThat(response.page()).isEqualTo(1);
        assertThat(response.size()).isEqualTo(50);

        ArgumentCaptor<Pageable> pageableCaptor = ArgumentCaptor.forClass(Pageable.class);
        verify(questRepository).findByStatus(eq(QuestStatus.PENDING_ADMIN_REVIEW), pageableCaptor.capture());
        assertThat(pageableCaptor.getValue().getPageNumber()).isZero();
        assertThat(pageableCaptor.getValue().getPageSize()).isEqualTo(50);
    }

    @Test
    void listQuestsWithoutStatusShouldQueryPipelineStatusesExcludingDraft() {
        User maintainer = user(2001L, UserRole.MAINTAINER);
        Quest quest = quest(maintainer, QuestStatus.PUBLISHED);
        when(questRepository.findByStatusIn(any(), any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of(quest)));

        AdminQuestPageResponse response = adminQuestService.listQuests(null, 1, 20);

        assertThat(response.items()).hasSize(1);
        // 「全部」视图覆盖待审核/已上架/已接取/进行中/审核中/已完成/已退回/已下架，但不含 DRAFT
        @SuppressWarnings("unchecked")
        ArgumentCaptor<java.util.Collection<QuestStatus>> statusesCaptor =
                ArgumentCaptor.forClass(java.util.Collection.class);
        verify(questRepository).findByStatusIn(statusesCaptor.capture(), any(Pageable.class));
        assertThat(statusesCaptor.getValue())
                .containsExactlyInAnyOrder(
                        QuestStatus.PENDING_ADMIN_REVIEW, QuestStatus.PUBLISHED,
                        QuestStatus.IN_PROGRESS, QuestStatus.IN_REVIEW, QuestStatus.COMPLETED,
                        QuestStatus.REJECTED, QuestStatus.CLOSED)
                .doesNotContain(QuestStatus.DRAFT);
    }

    @Test
    void reviewQuestShouldApprovePendingQuestAndSaveAuditRecord() {
        User admin = user(1001L, UserRole.ADMIN);
        Quest quest = quest(user(2001L, UserRole.MAINTAINER), QuestStatus.PENDING_ADMIN_REVIEW);

        when(userRepository.findById(1001L)).thenReturn(Optional.of(admin));
        when(questRepository.findById(5001L)).thenReturn(Optional.of(quest));
        when(reviewRecordRepository.save(any(AdminReviewRecord.class))).thenAnswer(invocation -> invocation.getArgument(0));
        stubAllTechStacksRegistered(quest);

        AdminReviewRequest approveRequest = request(AdminDecision.APPROVE_PUBLISH, "Ready to publish", true);
        approveRequest.setChecklist(fullPassingChecklist());

        AdminReviewResponse response = adminQuestService.reviewQuest(
                5001L,
                1001L,
                approveRequest);

        assertThat(response.questId()).isEqualTo(5001L);
        assertThat(response.adminId()).isEqualTo(1001L);
        assertThat(response.decision()).isEqualTo(AdminDecision.APPROVE_PUBLISH);
        assertThat(response.questStatus()).isEqualTo(QuestStatus.PUBLISHED);
        assertThat(quest.getStatus()).isEqualTo(QuestStatus.PUBLISHED);

        ArgumentCaptor<AdminReviewRecord> recordCaptor = ArgumentCaptor.forClass(AdminReviewRecord.class);
        verify(reviewRecordRepository).save(recordCaptor.capture());
        assertThat(recordCaptor.getValue().getQuest()).isSameAs(quest);
        assertThat(recordCaptor.getValue().getAdmin()).isSameAs(admin);
        assertThat(recordCaptor.getValue().getDecision()).isEqualTo(AdminDecision.APPROVE_PUBLISH);
        assertThat(recordCaptor.getValue().getReason()).isEqualTo("Ready to publish");
        assertThat(recordCaptor.getValue().isVisibleToPublisher()).isTrue();
        verify(questRepository).save(quest);
    }

    @Test
    void reviewQuestShouldRejectPendingQuestAndKeepReasonPrivateWhenRequested() {
        User admin = user(1001L, UserRole.ADMIN);
        Quest quest = quest(user(2001L, UserRole.MAINTAINER), QuestStatus.PENDING_ADMIN_REVIEW);

        when(userRepository.findById(1001L)).thenReturn(Optional.of(admin));
        when(questRepository.findById(5001L)).thenReturn(Optional.of(quest));
        when(reviewRecordRepository.save(any(AdminReviewRecord.class))).thenAnswer(invocation -> invocation.getArgument(0));

        AdminReviewResponse response = adminQuestService.reviewQuest(
                5001L,
                1001L,
                request(AdminDecision.REJECT_PUBLISH, "Completion criteria are unclear", false));

        assertThat(response.decision()).isEqualTo(AdminDecision.REJECT_PUBLISH);
        assertThat(response.questStatus()).isEqualTo(QuestStatus.REJECTED);
        assertThat(quest.getStatus()).isEqualTo(QuestStatus.REJECTED);

        ArgumentCaptor<AdminReviewRecord> recordCaptor = ArgumentCaptor.forClass(AdminReviewRecord.class);
        verify(reviewRecordRepository).save(recordCaptor.capture());
        assertThat(recordCaptor.getValue().getReason()).isEqualTo("Completion criteria are unclear");
        assertThat(recordCaptor.getValue().isVisibleToPublisher()).isFalse();
        verify(questRepository).save(quest);
    }

    @Test
    void reviewQuestShouldRejectNonAdminReviewer() {
        when(userRepository.findById(2001L)).thenReturn(Optional.of(user(2001L, UserRole.MAINTAINER)));

        assertThatThrownBy(() -> adminQuestService.reviewQuest(
                5001L,
                2001L,
                request(AdminDecision.APPROVE_PUBLISH, "Not allowed", true)))
                .isInstanceOf(BusinessException.class)
                .extracting("code")
                .isEqualTo("FORBIDDEN");

        verify(questRepository, never()).findById(any());
        verify(reviewRecordRepository, never()).save(any());
        verify(questRepository, never()).save(any());
    }

    @Test
    void reviewQuestShouldRejectApprovalWhenQuestIsNotPendingReview() {
        User admin = user(1001L, UserRole.ADMIN);
        Quest quest = quest(user(2001L, UserRole.MAINTAINER), QuestStatus.PUBLISHED);

        when(userRepository.findById(1001L)).thenReturn(Optional.of(admin));
        when(questRepository.findById(5001L)).thenReturn(Optional.of(quest));
        stubAllTechStacksRegistered(quest);

        AdminReviewRequest approveRequest = request(AdminDecision.APPROVE_PUBLISH, "Already published", true);
        approveRequest.setChecklist(fullPassingChecklist());

        assertThatThrownBy(() -> adminQuestService.reviewQuest(5001L, 1001L, approveRequest))
                .isInstanceOf(BusinessException.class)
                .extracting("code")
                .isEqualTo("QUEST_NOT_REVIEWABLE");

        verify(reviewRecordRepository, never()).save(any());
        verify(questRepository, never()).save(any());
    }

    @Test
    void reviewQuestShouldTakeDownQuestAndCancelActiveAssignments() {
        User admin = user(1001L, UserRole.ADMIN);
        User maintainer = user(2001L, UserRole.MAINTAINER);
        User assignee = user(3001L, UserRole.BEGINNER);
        Quest quest = quest(maintainer, QuestStatus.IN_PROGRESS);
        QuestAssignment activeAssignment = new QuestAssignment(quest, assignee);

        when(userRepository.findById(1001L)).thenReturn(Optional.of(admin));
        when(questRepository.findById(5001L)).thenReturn(Optional.of(quest));
        when(assignmentRepository.findByQuestAndStatus(quest, AssignmentStatus.ACTIVE))
                .thenReturn(List.of(activeAssignment));
        when(reviewRecordRepository.save(any(AdminReviewRecord.class))).thenAnswer(invocation -> invocation.getArgument(0));

        AdminReviewResponse response = adminQuestService.reviewQuest(
                5001L,
                1001L,
                request(AdminDecision.TAKE_DOWN, "Contains invalid content", true));

        assertThat(response.decision()).isEqualTo(AdminDecision.TAKE_DOWN);
        assertThat(response.questStatus()).isEqualTo(QuestStatus.CLOSED);
        assertThat(quest.getStatus()).isEqualTo(QuestStatus.CLOSED);
        assertThat(activeAssignment.getStatus()).isEqualTo(AssignmentStatus.CANCELLED);
        verify(assignmentRepository).save(activeAssignment);
        verify(questRepository).save(quest);
    }

    @Test
    void reviewQuestShouldRejectTakeDownWhenQuestIsAlreadyClosed() {
        User admin = user(1001L, UserRole.ADMIN);
        Quest quest = quest(user(2001L, UserRole.MAINTAINER), QuestStatus.CLOSED);

        when(userRepository.findById(1001L)).thenReturn(Optional.of(admin));
        when(questRepository.findById(5001L)).thenReturn(Optional.of(quest));

        assertThatThrownBy(() -> adminQuestService.reviewQuest(
                5001L,
                1001L,
                request(AdminDecision.TAKE_DOWN, "Already closed", true)))
                .isInstanceOf(BusinessException.class)
                .extracting("code")
                .isEqualTo("QUEST_ALREADY_CLOSED");

        verify(assignmentRepository, never()).findByQuestAndStatus(any(), any());
        verify(reviewRecordRepository, never()).save(any());
        verify(questRepository, never()).save(any());
    }

    @Test
    void reviewQuestShouldRejectApprovalWhenChecklistIsIncomplete() {
        User admin = user(1001L, UserRole.ADMIN);
        Quest quest = quest(user(2001L, UserRole.MAINTAINER), QuestStatus.PENDING_ADMIN_REVIEW);

        when(userRepository.findById(1001L)).thenReturn(Optional.of(admin));
        when(questRepository.findById(5001L)).thenReturn(Optional.of(quest));

        AdminReviewRequest approveRequest = request(AdminDecision.APPROVE_PUBLISH, "Ready to publish", true);
        approveRequest.setChecklist(List.of(new ChecklistItemDto("only-one", true)));

        assertThatThrownBy(() -> adminQuestService.reviewQuest(5001L, 1001L, approveRequest))
                .isInstanceOf(BusinessException.class)
                .extracting("code")
                .isEqualTo("CHECKLIST_INCOMPLETE");

        assertThat(quest.getStatus()).isEqualTo(QuestStatus.PENDING_ADMIN_REVIEW);
        verify(reviewRecordRepository, never()).save(any());
        verify(questRepository, never()).save(any());
    }

    @Test
    void reviewQuestShouldRejectApprovalWhenTechStackIsNotRegistered() {
        User admin = user(1001L, UserRole.ADMIN);
        Quest quest = quest(user(2001L, UserRole.MAINTAINER), QuestStatus.PENDING_ADMIN_REVIEW);

        when(userRepository.findById(1001L)).thenReturn(Optional.of(admin));
        when(questRepository.findById(5001L)).thenReturn(Optional.of(quest));
        when(techStackRepository.findByNameIgnoreCase("Spring Boot")).thenReturn(Optional.empty());

        AdminReviewRequest approveRequest = request(AdminDecision.APPROVE_PUBLISH, "Ready to publish", true);
        approveRequest.setChecklist(fullPassingChecklist());

        assertThatThrownBy(() -> adminQuestService.reviewQuest(5001L, 1001L, approveRequest))
                .isInstanceOf(BusinessException.class)
                .extracting("code")
                .isEqualTo("TECH_STACK_NOT_REGISTERED");

        assertThat(quest.getStatus()).isEqualTo(QuestStatus.PENDING_ADMIN_REVIEW);
        verify(reviewRecordRepository, never()).save(any());
        verify(questRepository, never()).save(any());
    }

    @Test
    void reviewQuestShouldNormalizeTechStackCasingOnApproval() {
        User admin = user(1001L, UserRole.ADMIN);
        Quest quest = quest(user(2001L, UserRole.MAINTAINER), QuestStatus.PENDING_ADMIN_REVIEW);

        when(userRepository.findById(1001L)).thenReturn(Optional.of(admin));
        when(questRepository.findById(5001L)).thenReturn(Optional.of(quest));
        when(reviewRecordRepository.save(any(AdminReviewRecord.class))).thenAnswer(invocation -> invocation.getArgument(0));
        // 委托人填写的是 "Spring Boot"，平台登记的标准写法大小写不同，验证最终落库会被规范化为登记的标准写法。
        when(techStackRepository.findByNameIgnoreCase("Spring Boot"))
                .thenReturn(Optional.of(new QuestTechStack("spring boot")));

        AdminReviewRequest approveRequest = request(AdminDecision.APPROVE_PUBLISH, "Ready to publish", true);
        approveRequest.setChecklist(fullPassingChecklist());

        adminQuestService.reviewQuest(5001L, 1001L, approveRequest);

        assertThat(quest.getTechStackJson()).isEqualTo("[\"spring boot\"]");
    }

    @Test
    void reviewQuestShouldReopenClosedQuest() {
        User admin = user(1001L, UserRole.ADMIN);
        Quest quest = quest(user(2001L, UserRole.MAINTAINER), QuestStatus.CLOSED);

        when(userRepository.findById(1001L)).thenReturn(Optional.of(admin));
        when(questRepository.findById(5001L)).thenReturn(Optional.of(quest));
        when(reviewRecordRepository.save(any(AdminReviewRecord.class))).thenAnswer(invocation -> invocation.getArgument(0));

        AdminReviewResponse response = adminQuestService.reviewQuest(
                5001L,
                1001L,
                request(AdminDecision.REOPEN, "Reopening after fix", true));

        assertThat(response.decision()).isEqualTo(AdminDecision.REOPEN);
        assertThat(response.questStatus()).isEqualTo(QuestStatus.PUBLISHED);
        assertThat(quest.getStatus()).isEqualTo(QuestStatus.PUBLISHED);
        verify(questRepository).save(quest);
    }

    @Test
    void reviewQuestShouldRejectReopenWhenQuestIsNotClosed() {
        User admin = user(1001L, UserRole.ADMIN);
        Quest quest = quest(user(2001L, UserRole.MAINTAINER), QuestStatus.PUBLISHED);

        when(userRepository.findById(1001L)).thenReturn(Optional.of(admin));
        when(questRepository.findById(5001L)).thenReturn(Optional.of(quest));

        assertThatThrownBy(() -> adminQuestService.reviewQuest(
                5001L,
                1001L,
                request(AdminDecision.REOPEN, "Not closed", true)))
                .isInstanceOf(BusinessException.class)
                .extracting("code")
                .isEqualTo("QUEST_NOT_REOPENABLE");

        verify(reviewRecordRepository, never()).save(any());
        verify(questRepository, never()).save(any());
    }

    private AdminReviewRequest request(AdminDecision decision, String reason, boolean visibleToPublisher) {
        AdminReviewRequest request = new AdminReviewRequest();
        request.setDecision(decision);
        request.setReason(reason);
        request.setVisibleToPublisher(visibleToPublisher);
        return request;
    }

    private Quest quest(User maintainer, QuestStatus status) {
        CodeRepository repository = repository(maintainer);
        Quest quest = new Quest(
                maintainer,
                repository,
                issue(repository),
                category(),
                "Review admin quest",
                "Task description",
                "Completion criteria",
                Difficulty.C,
                "[\"Spring Boot\"]",
                180,
                6);
        quest.setQuestId(5001L);
        quest.setStatus(status);
        return quest;
    }

    private QuestCategory category() {
        QuestCategory category = new QuestCategory("Backend", "Backend tasks");
        category.setCategoryId(2L);
        return category;
    }

    private CodeIssue issue(CodeRepository repository) {
        CodeIssue issue = new CodeIssue(repository, "42", "Admin review", "OPEN");
        issue.setIssueId(3001L);
        return issue;
    }

    private CodeRepository repository(User owner) {
        CodeRepository repository = new CodeRepository(owner, "git-guild", "GITEA", "http://localhost:3000/git-guild");
        repository.setRepositoryId(1001L);
        repository.setSyncStatus("SYNCED");
        return repository;
    }

    private User user(Long id, UserRole role) {
        User user = new User("user" + id, "user" + id + "@example.com", "password-hash", role);
        user.setUserId(id);
        return user;
    }
}
