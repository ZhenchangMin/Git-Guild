package com.gitguild.backend.codehost.service;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.gitguild.backend.codehost.domain.CodeRepository;
import com.gitguild.backend.codehost.gitea.GiteaAdapter;
import com.gitguild.backend.codehost.gitea.GiteaProperties;
import com.gitguild.backend.codehost.repository.CodeIssueRepository;
import com.gitguild.backend.codehost.repository.CodePullRequestRepository;
import com.gitguild.backend.codehost.repository.CodeRepositoryRepository;
import com.gitguild.backend.growth.repository.ContributionRecordRepository;
import com.gitguild.backend.growth.repository.XpTransactionRepository;
import com.gitguild.backend.quest.domain.Quest;
import com.gitguild.backend.quest.repository.AdminReviewRecordRepository;
import com.gitguild.backend.quest.repository.QuestAssignmentRepository;
import com.gitguild.backend.quest.repository.QuestRepository;
import com.gitguild.backend.review.domain.Submission;
import com.gitguild.backend.review.repository.ReviewRecordRepository;
import com.gitguild.backend.review.repository.SubmissionRepository;
import com.gitguild.backend.user.domain.User;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class RepositoryCascadeDeleterTest {

    @Mock private QuestRepository questRepository;
    @Mock private SubmissionRepository submissionRepository;
    @Mock private ReviewRecordRepository reviewRecordRepository;
    @Mock private QuestAssignmentRepository questAssignmentRepository;
    @Mock private AdminReviewRecordRepository adminReviewRecordRepository;
    @Mock private XpTransactionRepository xpTransactionRepository;
    @Mock private ContributionRecordRepository contributionRecordRepository;
    @Mock private CodeIssueRepository codeIssueRepository;
    @Mock private CodePullRequestRepository codePullRequestRepository;
    @Mock private CodeRepositoryRepository codeRepositoryRepository;
    @Mock private GiteaAdapter giteaAdapter;

    private final GiteaProperties giteaProperties =
            new GiteaProperties("http://localhost:3000", "", "spike-admin", null);

    private RepositoryCascadeDeleter deleter() {
        return new RepositoryCascadeDeleter(questRepository, submissionRepository, reviewRecordRepository,
                questAssignmentRepository, adminReviewRecordRepository, xpTransactionRepository,
                contributionRecordRepository, codeIssueRepository, codePullRequestRepository,
                codeRepositoryRepository, giteaAdapter, giteaProperties);
    }

    private CodeRepository repo(String sourceUrl) {
        return new CodeRepository(org.mockito.Mockito.mock(User.class), "demo-repo", "GITEA", sourceUrl);
    }

    @Test
    void cascadeDeletesAllChildrenAndPlatformGiteaCopy() {
        CodeRepository repo = repo("http://localhost:3000/spike-admin/demo-repo");

        Quest quest = org.mockito.Mockito.mock(Quest.class);
        when(quest.getQuestId()).thenReturn(10L);
        Submission submission = org.mockito.Mockito.mock(Submission.class);
        when(submission.getSubmissionId()).thenReturn(20L);

        when(questRepository.findByRepositoryRepositoryId(any())).thenReturn(List.of(quest));
        when(submissionRepository.findByQuestQuestIdIn(any())).thenReturn(List.of(submission));
        when(reviewRecordRepository.findBySubmissionSubmissionIdIn(any())).thenReturn(List.of());
        when(questAssignmentRepository.findByQuestQuestIdIn(any())).thenReturn(List.of());
        when(adminReviewRecordRepository.findByQuestQuestIdIn(any())).thenReturn(List.of());
        when(xpTransactionRepository.findByQuestQuestIdIn(any())).thenReturn(List.of());
        when(contributionRecordRepository.findByQuestQuestIdIn(any())).thenReturn(List.of());
        when(codeIssueRepository.findByRepositoryRepositoryId(any())).thenReturn(List.of());
        when(codePullRequestRepository.findByRepositoryRepositoryId(any())).thenReturn(List.of());
        when(contributionRecordRepository.findByRepositoryRepositoryId(any())).thenReturn(List.of());

        deleter().deleteCascade(repo);

        verify(submissionRepository).deleteAll(List.of(submission));
        verify(questRepository).deleteAll(List.of(quest));
        verify(codeRepositoryRepository).delete(repo);
        // 平台 host 与 base-url 一致 → 删除 Gitea 副本
        verify(giteaAdapter).deleteRepository(eq("spike-admin"), eq("demo-repo"));
    }

    @Test
    void skipsGiteaCopyWhenSourceHostIsNotPlatform() {
        CodeRepository repo = repo("https://gitea.com/ZhenchangMin/Operating-System.git");

        when(questRepository.findByRepositoryRepositoryId(any())).thenReturn(List.of());
        when(codeIssueRepository.findByRepositoryRepositoryId(any())).thenReturn(List.of());
        when(codePullRequestRepository.findByRepositoryRepositoryId(any())).thenReturn(List.of());
        when(contributionRecordRepository.findByRepositoryRepositoryId(any())).thenReturn(List.of());

        deleter().deleteCascade(repo);

        verify(codeRepositoryRepository).delete(repo);
        // 源 host 非平台 → 绝不调用 Gitea 删除（避免误删第三方仓库）
        verify(giteaAdapter, never()).deleteRepository(any(), any());
    }

    @Test
    void deletesRepoWithoutQuestsSkipsQuestChildLookups() {
        CodeRepository repo = repo("http://localhost:3000/spike-admin/empty-repo");

        when(questRepository.findByRepositoryRepositoryId(any())).thenReturn(List.of());
        when(codeIssueRepository.findByRepositoryRepositoryId(any())).thenReturn(List.of());
        when(codePullRequestRepository.findByRepositoryRepositoryId(any())).thenReturn(List.of());
        when(contributionRecordRepository.findByRepositoryRepositoryId(any())).thenReturn(List.of());

        deleter().deleteCascade(repo);

        // 无委托时不应去查 quest 子表
        verify(submissionRepository, never()).findByQuestQuestIdIn(any());
        verify(codeRepositoryRepository).delete(repo);
        verify(giteaAdapter).deleteRepository(eq("spike-admin"), eq("empty-repo"));
    }
}
