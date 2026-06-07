package com.gitguild.backend.quest.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.gitguild.backend.codehost.domain.CodeIssue;
import com.gitguild.backend.codehost.domain.CodeRepository;
import com.gitguild.backend.codehost.repository.CodeIssueRepository;
import com.gitguild.backend.codehost.repository.CodePullRequestRepository;
import com.gitguild.backend.codehost.repository.CodeRepositoryRepository;
import com.gitguild.backend.codehost.service.CodeIssueService;
import com.gitguild.backend.common.BusinessException;
import com.gitguild.backend.quest.domain.AssignmentStatus;
import com.gitguild.backend.quest.domain.Difficulty;
import com.gitguild.backend.quest.domain.Quest;
import com.gitguild.backend.quest.domain.QuestAssignment;
import com.gitguild.backend.quest.domain.QuestCategory;
import com.gitguild.backend.quest.domain.QuestStatus;
import com.gitguild.backend.quest.dto.CreateQuestRequest;
import com.gitguild.backend.quest.dto.QuestResponses.AssignmentResponse;
import com.gitguild.backend.quest.dto.QuestResponses.CreateQuestResponse;
import com.gitguild.backend.quest.dto.QuestResponses.QuestDetailResponse;
import com.gitguild.backend.quest.dto.QuestSearchCriteria;
import com.gitguild.backend.quest.repository.QuestAssignmentRepository;
import com.gitguild.backend.quest.repository.QuestCategoryRepository;
import com.gitguild.backend.quest.repository.QuestRepository;
import com.gitguild.backend.quest.repository.QuestTagRepository;
import com.gitguild.backend.quest.service.QuestTaskBranchService;
import com.gitguild.backend.user.domain.User;
import com.gitguild.backend.user.domain.UserRole;
import com.gitguild.backend.user.repository.UserRepository;
import jakarta.persistence.criteria.CriteriaBuilder;
import jakarta.persistence.criteria.CriteriaQuery;
import jakarta.persistence.criteria.Path;
import jakarta.persistence.criteria.Predicate;
import jakarta.persistence.criteria.Root;
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
import org.springframework.data.jpa.domain.Specification;

@ExtendWith(MockitoExtension.class)
class QuestServiceImplTest {

    @Mock
    private QuestRepository questRepository;
    @Mock
    private QuestAssignmentRepository assignmentRepository;
    @Mock
    private QuestCategoryRepository categoryRepository;
    @Mock
    private QuestTagRepository tagRepository;
    @Mock
    private CodeRepositoryRepository codeRepositoryRepository;
    @Mock
    private CodeIssueRepository issueRepository;
    @Mock
    private CodeIssueService codeIssueService;
    @Mock
    private QuestTaskBranchService taskBranchService;
    @Mock
    private UserRepository userRepository;

    @Mock
    private CodePullRequestRepository pullRequestRepository;

    private QuestServiceImpl questService;

    @BeforeEach
    void setUp() {
        questService = new QuestServiceImpl(
                questRepository,
                assignmentRepository,
                categoryRepository,
                tagRepository,
                codeRepositoryRepository,
                issueRepository,
                codeIssueService,
                taskBranchService,
                pullRequestRepository,
                userRepository,
                new ObjectMapper(),
                new com.gitguild.backend.codehost.gitea.GiteaProperties("http://localhost:3000", "test-token", "spike-admin"));
    }

    @Test
    void createQuestShouldCreatePendingAdminReviewQuest() {
        User maintainer = user(2001L, UserRole.MAINTAINER);
        CodeRepository repository = repository(maintainer);
        CodeIssue issue = issue(repository, "OPEN");
        QuestCategory category = category();
        CreateQuestRequest request = createQuestRequest();

        when(userRepository.findById(2001L)).thenReturn(Optional.of(maintainer));
        when(codeRepositoryRepository.findById(1001L)).thenReturn(Optional.of(repository));
        when(issueRepository.findByIssueIdAndRepositoryRepositoryId(3001L, 1001L)).thenReturn(Optional.of(issue));
        when(questRepository.existsByIssueAndStatusIn(any(), any())).thenReturn(false);
        when(categoryRepository.findById(2L)).thenReturn(Optional.of(category));
        when(questRepository.save(any(Quest.class))).thenAnswer(invocation -> {
            Quest quest = invocation.getArgument(0);
            quest.setQuestId(5001L);
            return quest;
        });

        CreateQuestResponse response = questService.createQuest(2001L, request);

        assertThat(response.questId()).isEqualTo(5001L);
        assertThat(response.status()).isEqualTo(QuestStatus.DRAFT);
        ArgumentCaptor<Quest> questCaptor = ArgumentCaptor.forClass(Quest.class);
        verify(questRepository).save(questCaptor.capture());
        assertThat(questCaptor.getValue().getTitle()).isEqualTo("实现 Issue 同步状态页面");
        assertThat(questCaptor.getValue().getTechStackJson()).contains("Vue", "Spring Boot");
    }

    @Test
    void createQuestShouldCreateGiteaIssueWhenTitleProvided() {
        User maintainer = user(2001L, UserRole.MAINTAINER);
        CodeRepository repository = repository(maintainer);
        CodeIssue createdIssue = issue(repository, "OPEN");
        QuestCategory category = category();

        CreateQuestRequest request = createQuestRequest();
        // 新建 Issue 模式：无 issueId，仅给 giteaIssueTitle。
        request.setIssueId(null);
        request.setGiteaIssueTitle("新建 Issue 标题");
        request.setGiteaIssueBody("Issue 正文");

        when(userRepository.findById(2001L)).thenReturn(Optional.of(maintainer));
        when(codeRepositoryRepository.findById(1001L)).thenReturn(Optional.of(repository));
        when(codeIssueService.createFromGitea(repository, "新建 Issue 标题", "Issue 正文"))
                .thenReturn(createdIssue);
        when(questRepository.existsByIssueAndStatusIn(any(), any())).thenReturn(false);
        when(categoryRepository.findById(2L)).thenReturn(Optional.of(category));
        when(questRepository.save(any(Quest.class))).thenAnswer(invocation -> {
            Quest quest = invocation.getArgument(0);
            quest.setQuestId(5002L);
            return quest;
        });

        CreateQuestResponse response = questService.createQuest(2001L, request);

        assertThat(response.questId()).isEqualTo(5002L);
        // 走的是 Gitea 新建路径，不应再去按 issueId 查本地 Issue。
        verify(codeIssueService).createFromGitea(repository, "新建 Issue 标题", "Issue 正文");
        verify(issueRepository, never()).findByIssueIdAndRepositoryRepositoryId(any(), any());
    }

    @Test
    void createQuestShouldRejectWhenNeitherIssueIdNorGiteaTitleProvided() {
        User maintainer = user(2001L, UserRole.MAINTAINER);
        CodeRepository repository = repository(maintainer);
        CreateQuestRequest request = createQuestRequest();
        request.setIssueId(null);
        request.setGiteaIssueTitle(null);

        when(userRepository.findById(2001L)).thenReturn(Optional.of(maintainer));
        when(codeRepositoryRepository.findById(1001L)).thenReturn(Optional.of(repository));

        assertThatThrownBy(() -> questService.createQuest(2001L, request))
                .isInstanceOf(BusinessException.class)
                .extracting("code")
                .isEqualTo("VALIDATION_FAILED");
    }

    @Test
    void createQuestShouldRejectBeginnerPublisher() {
        when(userRepository.findById(3001L)).thenReturn(Optional.of(user(3001L, UserRole.BEGINNER)));

        assertThatThrownBy(() -> questService.createQuest(3001L, createQuestRequest()))
                .isInstanceOf(BusinessException.class)
                .extracting("code")
                .isEqualTo("FORBIDDEN");
    }

    @Test
    void createQuestShouldRejectUnavailableIssue() {
        User maintainer = user(2001L, UserRole.MAINTAINER);
        CodeRepository repository = repository(maintainer);
        CodeIssue issue = issue(repository, "CLOSED");

        when(userRepository.findById(2001L)).thenReturn(Optional.of(maintainer));
        when(codeRepositoryRepository.findById(1001L)).thenReturn(Optional.of(repository));
        when(issueRepository.findByIssueIdAndRepositoryRepositoryId(3001L, 1001L)).thenReturn(Optional.of(issue));

        assertThatThrownBy(() -> questService.createQuest(2001L, createQuestRequest()))
                .isInstanceOf(BusinessException.class)
                .extracting("code")
                .isEqualTo("ISSUE_NOT_AVAILABLE");
    }

    @Test
    void acceptQuestShouldCreateAssignmentAndMarkQuestInProgress() {
        User maintainer = user(2001L, UserRole.MAINTAINER);
        User assignee = user(3001L, UserRole.BEGINNER);
        Quest quest = publishedQuest(maintainer);

        when(userRepository.findById(3001L)).thenReturn(Optional.of(assignee));
        when(questRepository.findById(5001L)).thenReturn(Optional.of(quest));
        when(assignmentRepository.existsByQuestAndAssigneeUserIdAndStatusIn(any(), any(), any())).thenReturn(false);
        when(assignmentRepository.save(any(QuestAssignment.class))).thenAnswer(invocation -> {
            QuestAssignment assignment = invocation.getArgument(0);
            assignment.setAssignmentId(7001L);
            return assignment;
        });

        AssignmentResponse response = questService.acceptQuest(5001L, 3001L);

        assertThat(response.assignmentId()).isEqualTo(7001L);
        assertThat(response.questStatus()).isEqualTo(QuestStatus.IN_PROGRESS);
        assertThat(quest.getStatus()).isEqualTo(QuestStatus.IN_PROGRESS);
        verify(questRepository).save(quest);
    }

    @Test
    void acceptQuestShouldRejectDuplicateAssignmentBySameUser() {
        User maintainer = user(2001L, UserRole.MAINTAINER);
        User assignee = user(3001L, UserRole.BEGINNER);
        Quest quest = publishedQuest(maintainer);

        when(userRepository.findById(3001L)).thenReturn(Optional.of(assignee));
        when(questRepository.findById(5001L)).thenReturn(Optional.of(quest));
        when(assignmentRepository.existsByQuestAndAssigneeUserIdAndStatusIn(any(), any(), any())).thenReturn(true);

        assertThatThrownBy(() -> questService.acceptQuest(5001L, 3001L))
                .isInstanceOf(BusinessException.class)
                .extracting("code")
                .isEqualTo("DUPLICATE_ASSIGNMENT");
    }

    @Test
    void acceptQuestShouldAllowMultipleAdventurersOnSameQuest() {
        User maintainer = user(2001L, UserRole.MAINTAINER);
        User secondAdventurer = user(3002L, UserRole.BEGINNER);
        Quest quest = inProgressQuest(maintainer);

        when(userRepository.findById(3002L)).thenReturn(Optional.of(secondAdventurer));
        when(questRepository.findById(5001L)).thenReturn(Optional.of(quest));
        when(assignmentRepository.existsByQuestAndAssigneeUserIdAndStatusIn(any(), any(), any())).thenReturn(false);
        when(assignmentRepository.save(any(QuestAssignment.class))).thenAnswer(invocation -> {
            QuestAssignment assignment = invocation.getArgument(0);
            assignment.setAssignmentId(7002L);
            return assignment;
        });

        AssignmentResponse response = questService.acceptQuest(5001L, 3002L);

        assertThat(response.assignmentId()).isEqualTo(7002L);
        assertThat(quest.getStatus()).isEqualTo(QuestStatus.IN_PROGRESS);
    }

    @Test
    void acceptQuestShouldRejectAdminAssigneeBeforeLoadingQuest() {
        when(userRepository.findById(1001L)).thenReturn(Optional.of(user(1001L, UserRole.ADMIN)));

        assertThatThrownBy(() -> questService.acceptQuest(5001L, 1001L))
                .isInstanceOf(BusinessException.class)
                .extracting("code")
                .isEqualTo("FORBIDDEN");

        verify(questRepository, never()).findById(any());
        verify(assignmentRepository, never()).save(any());
    }

    @Test
    void acceptQuestShouldRejectQuestThatIsNotAcceptable() {
        User maintainer = user(2001L, UserRole.MAINTAINER);
        User assignee = user(3001L, UserRole.BEGINNER);
        Quest quest = publishedQuest(maintainer);
        quest.setStatus(QuestStatus.PENDING_ADMIN_REVIEW);

        when(userRepository.findById(3001L)).thenReturn(Optional.of(assignee));
        when(questRepository.findById(5001L)).thenReturn(Optional.of(quest));

        assertThatThrownBy(() -> questService.acceptQuest(5001L, 3001L))
                .isInstanceOf(BusinessException.class)
                .extracting("code")
                .isEqualTo("QUEST_NOT_ACCEPTABLE");

        verify(assignmentRepository, never()).existsByQuestAndAssigneeUserIdAndStatusIn(any(), any(), any());
        verify(assignmentRepository, never()).save(any());
        verify(questRepository, never()).save(any());
    }

    @Test
    void submitQuestShouldMoveDraftQuestToPendingAdminReview() {
        User maintainer = user(2001L, UserRole.MAINTAINER);
        Quest quest = publishedQuest(maintainer);
        quest.setStatus(QuestStatus.DRAFT);

        when(questRepository.findById(5001L)).thenReturn(Optional.of(quest));

        questService.submitQuest(5001L, 2001L);

        assertThat(quest.getStatus()).isEqualTo(QuestStatus.PENDING_ADMIN_REVIEW);
        verify(questRepository).save(quest);
    }

    @Test
    void submitQuestShouldRejectNonPublisher() {
        Quest quest = publishedQuest(user(2001L, UserRole.MAINTAINER));
        quest.setStatus(QuestStatus.DRAFT);

        when(questRepository.findById(5001L)).thenReturn(Optional.of(quest));

        assertThatThrownBy(() -> questService.submitQuest(5001L, 3001L))
                .isInstanceOf(BusinessException.class)
                .extracting("code")
                .isEqualTo("FORBIDDEN");

        verify(questRepository, never()).save(any());
    }

    @Test
    @SuppressWarnings({"unchecked", "rawtypes"})
    void searchQuestsShouldIncludePublishedAndInProgressByDefault() {
        when(questRepository.findAll(any(Specification.class), any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of()));
        QuestSearchCriteria criteria = questSearchCriteria(null);

        questService.searchQuests(criteria);

        ArgumentCaptor<Specification<Quest>> specificationCaptor = ArgumentCaptor.forClass(Specification.class);
        verify(questRepository).findAll(specificationCaptor.capture(), any(Pageable.class));

        Root<Quest> root = mock(Root.class);
        CriteriaQuery<?> query = mock(CriteriaQuery.class);
        CriteriaBuilder cb = mock(CriteriaBuilder.class);
        Path<QuestStatus> statusPath = mock(Path.class);
        Predicate statusPredicate = mock(Predicate.class);
        Predicate finalPredicate = mock(Predicate.class);
        when(root.get("status")).thenReturn((Path) statusPath);
        when(statusPath.in(List.of(QuestStatus.PUBLISHED, QuestStatus.IN_PROGRESS))).thenReturn(statusPredicate);
        when(cb.and(any(Predicate[].class))).thenReturn(finalPredicate);

        specificationCaptor.getValue().toPredicate(root, query, cb);

        verify(statusPath).in(List.of(QuestStatus.PUBLISHED, QuestStatus.IN_PROGRESS));
    }

    @Test
    void getQuestDetailShouldAllowPublicInProgressQuest() {
        Quest quest = inProgressQuest(user(2001L, UserRole.MAINTAINER));
        when(questRepository.findById(5001L)).thenReturn(Optional.of(quest));
        when(assignmentRepository.findFirstByQuestQuestIdAndStatus(5001L, AssignmentStatus.ACTIVE))
                .thenReturn(Optional.empty());

        QuestDetailResponse response = questService.getQuestDetail(5001L, null);

        assertThat(response.questId()).isEqualTo(5001L);
        assertThat(response.status()).isEqualTo(QuestStatus.IN_PROGRESS);
    }

    @Test
    void getQuestDetailShouldAllowPublicCompletedQuest() {
        Quest quest = publishedQuest(user(2001L, UserRole.MAINTAINER));
        quest.setStatus(QuestStatus.COMPLETED);
        when(questRepository.findById(5001L)).thenReturn(Optional.of(quest));
        when(assignmentRepository.findFirstByQuestQuestIdAndStatus(5001L, AssignmentStatus.ACTIVE))
                .thenReturn(Optional.empty());

        QuestDetailResponse response = questService.getQuestDetail(5001L, null);

        assertThat(response.questId()).isEqualTo(5001L);
        assertThat(response.status()).isEqualTo(QuestStatus.COMPLETED);
    }

    @Test
    void getQuestDetailShouldRejectPublicClosedQuest() {
        Quest quest = publishedQuest(user(2001L, UserRole.MAINTAINER));
        quest.setStatus(QuestStatus.CLOSED);
        when(questRepository.findById(5001L)).thenReturn(Optional.of(quest));

        assertThatThrownBy(() -> questService.getQuestDetail(5001L, null))
                .isInstanceOf(BusinessException.class)
                .extracting("code")
                .isEqualTo("FORBIDDEN");
    }

    @Test
    void getQuestDetailShouldAllowAssignedUserToViewClosedQuest() {
        User maintainer = user(2001L, UserRole.MAINTAINER);
        User assignee = user(3001L, UserRole.BEGINNER);
        Quest quest = publishedQuest(maintainer);
        quest.setStatus(QuestStatus.CLOSED);

        when(questRepository.findById(5001L)).thenReturn(Optional.of(quest));
        when(userRepository.findById(3001L)).thenReturn(Optional.of(assignee));
        when(assignmentRepository.existsByQuestAndAssigneeUserIdAndStatusIn(any(), any(), any()))
                .thenReturn(true);
        when(assignmentRepository.findFirstByQuestQuestIdAndStatus(5001L, AssignmentStatus.ACTIVE))
                .thenReturn(Optional.empty());

        QuestDetailResponse response = questService.getQuestDetail(5001L, 3001L);

        assertThat(response.questId()).isEqualTo(5001L);
        assertThat(response.status()).isEqualTo(QuestStatus.CLOSED);
    }

    private CreateQuestRequest createQuestRequest() {
        CreateQuestRequest request = new CreateQuestRequest();
        request.setRepositoryId(1001L);
        request.setIssueId(3001L);
        request.setTitle("实现 Issue 同步状态页面");
        request.setDescription("为仓库管理页补充 Issue 同步状态展示。");
        request.setCompletionCriteria("展示最近同步时间、同步状态和失败原因。");
        request.setDifficulty(Difficulty.C);
        request.setTechStack(List.of("Vue", "Spring Boot"));
        request.setEstimatedHours(6);
        request.setRewardXp(180);
        request.setCategoryId(2L);
        request.setTagIds(List.of());
        return request;
    }

    private QuestSearchCriteria questSearchCriteria(QuestStatus status) {
        return new QuestSearchCriteria(
                null,
                null,
                List.of(),
                null,
                List.of(),
                status,
                "createdAt",
                "desc",
                1,
                10);
    }

    private Quest inProgressQuest(User maintainer) {
        Quest quest = publishedQuest(maintainer);
        quest.setStatus(QuestStatus.IN_PROGRESS);
        return quest;
    }

    private Quest publishedQuest(User maintainer) {
        CodeRepository repository = repository(maintainer);
        Quest quest = new Quest(
                maintainer,
                repository,
                issue(repository, "OPEN"),
                category(),
                "实现 Issue 同步状态页面",
                "任务描述",
                "完成标准",
                Difficulty.C,
                "[\"Vue\"]",
                180,
                6);
        quest.setQuestId(5001L);
        quest.setStatus(QuestStatus.PUBLISHED);
        return quest;
    }

    private QuestCategory category() {
        QuestCategory category = new QuestCategory("前端开发", "围绕 Web 界面与交互的任务");
        category.setCategoryId(2L);
        return category;
    }

    private CodeIssue issue(CodeRepository repository, String status) {
        CodeIssue issue = new CodeIssue(repository, "42", "Issue 同步状态不可见", status);
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
