package com.gitguild.backend.codehost.service;

import com.gitguild.backend.codehost.domain.CodeIssue;
import com.gitguild.backend.codehost.domain.CodeRepository;
import com.gitguild.backend.codehost.gitea.GiteaAdapter;
import com.gitguild.backend.codehost.gitea.GiteaRepoCoordinates;
import com.gitguild.backend.codehost.gitea.dto.IssueInfo;
import com.gitguild.backend.codehost.repository.CodeIssueRepository;
import com.gitguild.backend.codehost.repository.CodeRepositoryRepository;
import com.gitguild.backend.common.BusinessException;
import com.gitguild.backend.ops.service.ExceptionRecorder;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

/**
 * {@link RepositorySyncService} 默认实现。
 *
 * <p>失败时调用 {@link ExceptionRecorder} 登记 SYNC 异常，再抛出——异常采集对调用方透明。
 */
@Service
public class RepositorySyncServiceImpl implements RepositorySyncService {

    private final GiteaAdapter giteaAdapter;
    private final CodeRepositoryRepository repositoryRepository;
    private final CodeIssueRepository issueRepository;
    private final ExceptionRecorder exceptionRecorder;

    public RepositorySyncServiceImpl(
            GiteaAdapter giteaAdapter,
            CodeRepositoryRepository repositoryRepository,
            CodeIssueRepository issueRepository,
            ExceptionRecorder exceptionRecorder) {
        this.giteaAdapter = giteaAdapter;
        this.repositoryRepository = repositoryRepository;
        this.issueRepository = issueRepository;
        this.exceptionRecorder = exceptionRecorder;
    }

    // 刻意不加 @Transactional：同步是幂等 upsert，逐条提交即可；失败时还需把
    // 「异常登记 + 标记 FAILED」落库，若包在同一事务里会随 rethrow 一起回滚而丢失。
    @Override
    public CodeRepository syncRepository(Long repositoryId) {
        CodeRepository repository = repositoryRepository.findById(repositoryId)
                .orElseThrow(() -> new BusinessException(
                        "REPOSITORY_NOT_FOUND", HttpStatus.NOT_FOUND, "仓库不存在",
                        "repositoryId=" + repositoryId));
        try {
            syncIssues(repository);
            repository.markSynced();
            return repositoryRepository.save(repository);
        } catch (RuntimeException ex) {
            // 登记真实同步失败到异常处理中心（独立事务），再抛出让前端看到失败。
            exceptionRecorder.recordSyncFailure(repository, ex.getMessage());
            repository.markSyncFailed(ex.getMessage());
            repositoryRepository.save(repository);
            throw ex;
        }
    }

    /** 从 Gitea 拉取仓库 Issue 并 upsert 到本地（按 external_issue_id 幂等）。 */
    private void syncIssues(CodeRepository repository) {
        GiteaRepoCoordinates coords = GiteaRepoCoordinates.parse(repository.getSourceUrl());
        for (IssueInfo remoteIssue : giteaAdapter.listIssues(coords.owner(), coords.repo())) {
            String externalIssueId = String.valueOf(remoteIssue.number());
            String status = remoteIssue.state() == null ? "OPEN" : remoteIssue.state().trim().toUpperCase();
            CodeIssue issue = issueRepository
                    .findByRepositoryRepositoryIdAndExternalIssueId(repository.getRepositoryId(), externalIssueId)
                    .orElseGet(() -> new CodeIssue(repository, externalIssueId, remoteIssue.title(), status));
            issue.updateFromSync(remoteIssue.title(), status, remoteIssue.htmlUrl());
            issueRepository.save(issue);
        }
    }
}
