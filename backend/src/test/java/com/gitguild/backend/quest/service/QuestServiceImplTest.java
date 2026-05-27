package com.gitguild.backend.quest.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.gitguild.backend.codehost.domain.CodeIssue;
import com.gitguild.backend.codehost.domain.CodeRepository;
import com.gitguild.backend.codehost.repository.CodeIssueRepository;
import com.gitguild.backend.codehost.repository.CodeRepositoryRepository;
import com.gitguild.backend.common.BusinessException;
import com.gitguild.backend.quest.domain.Difficulty;
import com.gitguild.backend.quest.domain.Quest;
import com.gitguild.backend.quest.domain.QuestAssignment;
import com.gitguild.backend.quest.domain.QuestCategory;
import com.gitguild.backend.quest.domain.QuestStatus;
import com.gitguild.backend.quest.dto.CreateQuestRequest;
import com.gitguild.backend.quest.dto.QuestResponses.AssignmentResponse;
import com.gitguild.backend.quest.dto.QuestResponses.CreateQuestResponse;
import com.gitguild.backend.quest.repository.QuestAssignmentRepository;
import com.gitguild.backend.quest.repository.QuestCategoryRepository;
import com.gitguild.backend.quest.repository.QuestRepository;
import com.gitguild.backend.quest.repository.QuestTagRepository;
import com.gitguild.backend.user.domain.User;
import com.gitguild.backend.user.domain.UserRole;
import com.gitguild.backend.user.repository.UserRepository;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

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
    private UserRepository userRepository;

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
                userRepository,
                new ObjectMapper());
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
