package com.gitguild.backend.codehost.service;

import com.gitguild.backend.codehost.domain.CodeIssue;
import com.gitguild.backend.codehost.domain.CodeRepository;
import com.gitguild.backend.codehost.gitea.GiteaAdapter;
import com.gitguild.backend.codehost.gitea.GiteaRepoCoordinates;
import com.gitguild.backend.codehost.gitea.dto.IssueInfo;
import com.gitguild.backend.codehost.repository.CodeIssueRepository;
import java.time.OffsetDateTime;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * {@link CodeIssueService} 的实现。
 *
 * <p>从 {@code CodeRepository.sourceUrl} 解析 Gitea owner/repo（见 {@link GiteaRepoCoordinates}），
 * 调用 {@link GiteaAdapter#createIssue} 创建 Gitea Issue 后 upsert 到本地库。
 */
@Service
public class CodeIssueServiceImpl implements CodeIssueService {

    private final GiteaAdapter giteaAdapter;
    private final CodeIssueRepository issueRepository;

    public CodeIssueServiceImpl(GiteaAdapter giteaAdapter, CodeIssueRepository issueRepository) {
        this.giteaAdapter = giteaAdapter;
        this.issueRepository = issueRepository;
    }

    @Override
    @Transactional
    public CodeIssue createFromGitea(CodeRepository repository, String title, String body) {
        GiteaRepoCoordinates coords = GiteaRepoCoordinates.parse(repository.getSourceUrl());
        IssueInfo info = giteaAdapter.createIssue(coords.owner(), coords.repo(), title, body);
        return upsert(repository, info);
    }

    /**
     * 幂等 upsert：按 (repositoryId, externalIssueId) 查找，存在则更新，不存在则新建。
     */
    private CodeIssue upsert(CodeRepository repository, IssueInfo info) {
        String externalIssueId = String.valueOf(info.number());
        CodeIssue local = issueRepository
                .findByRepositoryRepositoryIdAndExternalIssueId(
                        repository.getRepositoryId(), externalIssueId)
                .orElseGet(() -> new CodeIssue(
                        repository, externalIssueId, info.title(), info.state()));
        local.setTitle(info.title());
        local.setBody(info.body());
        local.setStatus(info.state());
        local.setExternalUrl(info.htmlUrl());
        local.setSyncedAt(OffsetDateTime.now());
        return issueRepository.save(local);
    }
}
