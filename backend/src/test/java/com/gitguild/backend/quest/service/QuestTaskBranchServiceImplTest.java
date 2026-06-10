package com.gitguild.backend.quest.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.gitguild.backend.codehost.domain.CodeIssue;
import com.gitguild.backend.codehost.domain.CodeRepository;
import com.gitguild.backend.codehost.gitea.GiteaAdapter;
import com.gitguild.backend.codehost.gitea.dto.BranchInfo;
import com.gitguild.backend.common.BusinessException;
import com.gitguild.backend.quest.domain.Difficulty;
import com.gitguild.backend.quest.domain.Quest;
import com.gitguild.backend.quest.domain.QuestAssignment;
import com.gitguild.backend.quest.domain.QuestCategory;
import com.gitguild.backend.quest.domain.QuestStatus;
import com.gitguild.backend.quest.repository.QuestAssignmentRepository;
import com.gitguild.backend.user.domain.User;
import com.gitguild.backend.user.domain.UserRole;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;

@ExtendWith(MockitoExtension.class)
class QuestTaskBranchServiceImplTest {

    @Mock
    private GiteaAdapter giteaAdapter;
    @Mock
    private QuestAssignmentRepository assignmentRepository;

    private QuestTaskBranchServiceImpl service;

    @BeforeEach
    void setUp() {
        service = new QuestTaskBranchServiceImpl(giteaAdapter, assignmentRepository);
    }

    @Test
    void ensureTaskBranchShouldCreateBranchAndPersist() {
        QuestAssignment assignment = assignment(null);
        when(giteaAdapter.createBranch(eq("git-guild"), eq("repo"), any(), eq("main")))
                .thenReturn(new BranchInfo("task/quest-5001-assignment-7001-adventurer", "abc123"));

        String branch = service.ensureTaskBranch(assignment);

        assertThat(branch).isEqualTo("task/quest-5001-assignment-7001-adventurer");
        assertThat(assignment.getTaskBranch()).isEqualTo(branch);
        verify(assignmentRepository).save(assignment);
    }

    @Test
    void ensureTaskBranchShouldBeIdempotentWhenBranchAlreadyRecorded() {
        QuestAssignment assignment = assignment("task/existing-branch");

        String branch = service.ensureTaskBranch(assignment);

        assertThat(branch).isEqualTo("task/existing-branch");
        verify(giteaAdapter, never()).createBranch(any(), any(), any(), any());
        verify(assignmentRepository, never()).save(any());
    }

    @Test
    void ensureTaskBranchShouldTreatGiteaConflictAsSuccess() {
        QuestAssignment assignment = assignment(null);
        when(giteaAdapter.createBranch(any(), any(), any(), any()))
                .thenThrow(new BusinessException("CODE_HOST_RESOURCE_CONFLICT", HttpStatus.CONFLICT, "代码托管资源已存在", null));

        String branch = service.ensureTaskBranch(assignment);

        assertThat(branch).isEqualTo("task/quest-5001-assignment-7001-adventurer");
        assertThat(assignment.getTaskBranch()).isEqualTo(branch);
        verify(assignmentRepository).save(assignment);
    }

    @Test
    void ensureTaskBranchShouldPropagateGiteaUnavailable() {
        QuestAssignment assignment = assignment(null);
        when(giteaAdapter.createBranch(any(), any(), any(), any()))
                .thenThrow(new BusinessException("CODE_HOST_UNAVAILABLE", HttpStatus.BAD_GATEWAY, "代码托管平台不可达", null));

        assertThatThrownBy(() -> service.ensureTaskBranch(assignment))
                .isInstanceOf(BusinessException.class)
                .extracting("code")
                .isEqualTo("CODE_HOST_UNAVAILABLE");
        verify(assignmentRepository, never()).save(any());
    }

    @Test
    void ensureTaskBranchShouldSanitizeUsernameInBranchName() {
        QuestAssignment assignment = assignment(null, "Zoe Reeves@dev");
        when(giteaAdapter.createBranch(any(), any(), any(), any()))
                .thenReturn(new BranchInfo("x", "y"));

        String branch = service.ensureTaskBranch(assignment);

        assertThat(branch).isEqualTo("task/quest-5001-assignment-7001-Zoe-Reeves-dev");
    }

    private QuestAssignment assignment(String taskBranch) {
        return assignment(taskBranch, "adventurer");
    }

    private QuestAssignment assignment(String taskBranch, String username) {
        User maintainer = user(2001L, "maintainer", UserRole.MAINTAINER);
        User assignee = user(3001L, username, UserRole.BEGINNER);
        CodeRepository repository = repository(maintainer);
        Quest quest = quest(maintainer, repository);
        QuestAssignment assignment = new QuestAssignment(quest, assignee);
        assignment.setAssignmentId(7001L);
        if (taskBranch != null) {
            assignment.setTaskBranch(taskBranch);
        }
        return assignment;
    }

    private Quest quest(User maintainer, CodeRepository repository) {
        Quest quest = new Quest(
                maintainer,
                repository,
                new CodeIssue(repository, "42", "Issue", "OPEN"),
                category(),
                "标题",
                "描述",
                "完成标准",
                Difficulty.C,
                "[\"Spring Boot\"]",
                180,
                6);
        quest.setQuestId(5001L);
        quest.setStatus(QuestStatus.IN_PROGRESS);
        return quest;
    }

    private QuestCategory category() {
        QuestCategory category = new QuestCategory("Backend", "Backend tasks");
        category.setCategoryId(2L);
        return category;
    }

    private CodeRepository repository(User owner) {
        CodeRepository repository = new CodeRepository(owner, "repo", "GITEA", "http://localhost:3000/git-guild/repo");
        repository.setRepositoryId(1001L);
        repository.setDefaultBranch("main");
        repository.setSyncStatus("SYNCED");
        return repository;
    }

    private User user(Long id, String username, UserRole role) {
        User user = new User(username, "user" + id + "@example.com", "hash", role);
        user.setUserId(id);
        return user;
    }
}
