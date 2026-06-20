package com.gitguild.backend.quest.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.gitguild.backend.codehost.domain.CodePullRequest;
import com.gitguild.backend.codehost.domain.CodeRepository;
import com.gitguild.backend.codehost.gitea.GiteaAdapter;
import com.gitguild.backend.codehost.gitea.dto.PrInfo;
import com.gitguild.backend.codehost.repository.CodePullRequestRepository;
import com.gitguild.backend.common.BusinessException;
import com.gitguild.backend.user.domain.User;
import com.gitguild.backend.user.domain.UserRole;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;

@ExtendWith(MockitoExtension.class)
class QuestPullRequestServiceImplTest {

    @Mock
    private GiteaAdapter giteaAdapter;
    @Mock
    private CodePullRequestRepository pullRequestRepository;

    private QuestPullRequestServiceImpl service;

    @BeforeEach
    void setUp() {
        service = new QuestPullRequestServiceImpl(giteaAdapter, pullRequestRepository);
    }

    // ── ensurePullRequestForSubmission ──────────────────────────────────

    @Test
    void ensureShouldCreateAndUpsertPullRequest() {
        CodeRepository repository = repository();
        when(giteaAdapter.createPullRequest(eq("spike-admin"), eq("hello"), any(), any(), eq("task/x"), eq("main")))
                .thenReturn(new PrInfo(7, "PR", "open", false, "task/x", "main", "http://localhost:3000/pulls/7", "advent"));
        when(pullRequestRepository.findByRepositoryRepositoryIdAndExternalPrId(1001L, "7")).thenReturn(Optional.empty());
        when(pullRequestRepository.save(any(CodePullRequest.class))).thenAnswer(inv -> inv.getArgument(0));

        CodePullRequest pr = service.ensurePullRequestForSubmission(repository, "task/x", "main", "title", "body");

        assertThat(pr.getExternalPrId()).isEqualTo("7");
        assertThat(pr.getStatus()).isEqualTo("OPEN");
        assertThat(pr.getSourceBranch()).isEqualTo("task/x");
        assertThat(pr.getTargetBranch()).isEqualTo("main");
        assertThat(pr.getExternalUrl()).isEqualTo("http://localhost:3000/pulls/7");
    }

    @Test
    void ensureShouldReuseExistingOpenPullRequestOnConflict() {
        CodeRepository repository = repository();
        when(giteaAdapter.createPullRequest(any(), any(), any(), any(), any(), any()))
                .thenThrow(new BusinessException("CODE_HOST_RESOURCE_CONFLICT", HttpStatus.CONFLICT, "代码托管资源已存在", null));
        when(giteaAdapter.listPulls("spike-admin", "hello")).thenReturn(List.of(
                new PrInfo(3, "Other", "open", false, "task/other", "main", "u3", "advent"),
                new PrInfo(9, "Existing", "open", false, "task/x", "main", "u9", "advent")));
        when(pullRequestRepository.findByRepositoryRepositoryIdAndExternalPrId(1001L, "9")).thenReturn(Optional.empty());
        when(pullRequestRepository.save(any(CodePullRequest.class))).thenAnswer(inv -> inv.getArgument(0));

        CodePullRequest pr = service.ensurePullRequestForSubmission(repository, "task/x", "main", "title", "body");

        assertThat(pr.getExternalPrId()).isEqualTo("9");
        assertThat(pr.getStatus()).isEqualTo("OPEN");
    }

    @Test
    void ensureShouldRethrowConflictWhenNoMatchingOpenPullRequest() {
        CodeRepository repository = repository();
        when(giteaAdapter.createPullRequest(any(), any(), any(), any(), any(), any()))
                .thenThrow(new BusinessException("CODE_HOST_RESOURCE_CONFLICT", HttpStatus.CONFLICT, "代码托管资源已存在", null));
        when(giteaAdapter.listPulls("spike-admin", "hello")).thenReturn(List.of(
                new PrInfo(3, "Other", "open", false, "task/other", "main", "u3", "advent")));

        assertThatThrownBy(() -> service.ensurePullRequestForSubmission(repository, "task/x", "main", "t", "b"))
                .isInstanceOf(BusinessException.class)
                .extracting("code")
                .isEqualTo("CODE_HOST_RESOURCE_CONFLICT");
        verify(pullRequestRepository, never()).save(any());
    }

    @Test
    void ensureShouldRejectEmptyTaskBranchBeforeCallingGitea() {
        CodeRepository repository = repository();

        assertThatThrownBy(() -> service.ensurePullRequestForSubmission(repository, "main", "main", "t", "b"))
                .isInstanceOf(BusinessException.class)
                .extracting("code")
                .isEqualTo("TASK_BRANCH_EMPTY");
        verify(giteaAdapter, never()).createPullRequest(any(), any(), any(), any(), any(), any());
    }

    @Test
    void ensureShouldTranslateGitea422ToTaskBranchEmpty() {
        CodeRepository repository = repository();
        when(giteaAdapter.createPullRequest(any(), any(), any(), any(), any(), any()))
                .thenThrow(new BusinessException("CODE_HOST_UNAVAILABLE", HttpStatus.BAD_GATEWAY,
                        "代码托管平台请求失败", "repo=spike-admin/hello -> HTTP 422"));

        assertThatThrownBy(() -> service.ensurePullRequestForSubmission(repository, "task/x", "main", "t", "b"))
                .isInstanceOf(BusinessException.class)
                .extracting("code")
                .isEqualTo("TASK_BRANCH_EMPTY");
    }

    // ── mergeForApproval ────────────────────────────────────────────────

    @Test
    void mergeShouldMarkPullRequestMerged() {
        CodeRepository repository = repository();
        CodePullRequest pr = pullRequest(repository, "OPEN");
        when(giteaAdapter.mergePullRequest("spike-admin", "hello", 7))
                .thenReturn(new PrInfo(7, "PR", "closed", true, "task/x", "main", "u7", "advent"));
        when(pullRequestRepository.save(any(CodePullRequest.class))).thenAnswer(inv -> inv.getArgument(0));

        CodePullRequest merged = service.mergeForApproval(pr, repository);

        assertThat(merged.getStatus()).isEqualTo("MERGED");
        assertThat(merged.isMerged()).isTrue();
    }

    @Test
    void mergeShouldTranslateConflictToPrMergeConflictAndLeaveEntityUntouched() {
        CodeRepository repository = repository();
        CodePullRequest pr = pullRequest(repository, "OPEN");
        when(giteaAdapter.mergePullRequest("spike-admin", "hello", 7))
                .thenThrow(new BusinessException("CODE_HOST_RESOURCE_CONFLICT", HttpStatus.CONFLICT, "代码托管资源已存在", null));

        assertThatThrownBy(() -> service.mergeForApproval(pr, repository))
                .isInstanceOf(BusinessException.class)
                .extracting("code")
                .isEqualTo("PR_MERGE_CONFLICT");
        assertThat(pr.getStatus()).isEqualTo("OPEN");
        verify(pullRequestRepository, never()).save(any());
    }

    @Test
    void mergeShouldShortCircuitWhenAlreadyMerged() {
        CodeRepository repository = repository();
        CodePullRequest pr = pullRequest(repository, "MERGED");

        CodePullRequest result = service.mergeForApproval(pr, repository);

        assertThat(result).isSameAs(pr);
        verify(giteaAdapter, never()).mergePullRequest(any(), any(), org.mockito.ArgumentMatchers.anyInt());
    }

    // ── fixtures ────────────────────────────────────────────────────────

    private CodeRepository repository() {
        User owner = new User("guild", "guild@gg.local", "hash", UserRole.MAINTAINER);
        owner.setUserId(5L);
        CodeRepository repository = new CodeRepository(owner, "hello", "GITEA", "http://localhost:3000/spike-admin/hello");
        repository.setRepositoryId(1001L);
        repository.setDefaultBranch("main");
        repository.setSyncStatus("SYNCED");
        return repository;
    }

    private CodePullRequest pullRequest(CodeRepository repository, String status) {
        CodePullRequest pr = new CodePullRequest(
                repository, "7", "PR", "task/x", "main", status, "http://localhost:3000/pulls/7");
        pr.setPullRequestId(8001L);
        return pr;
    }
}
