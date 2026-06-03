package com.gitguild.backend.codehost.service;

import com.gitguild.backend.codehost.domain.CodeIssue;
import com.gitguild.backend.codehost.domain.CodeRepository;
import com.gitguild.backend.codehost.gitea.GiteaAdapter;
import com.gitguild.backend.codehost.gitea.dto.IssueInfo;
import com.gitguild.backend.codehost.repository.CodeIssueRepository;
import com.gitguild.backend.common.BusinessException;
import java.time.OffsetDateTime;
import java.util.Arrays;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * {@link CodeIssueService} 的实现。
 *
 * <p>从 {@code CodeRepository.sourceUrl} 解析 Gitea owner/repo，
 * 调用 {@link GiteaAdapter#createIssue} 创建 Gitea Issue 后 upsert 到本地库。
 * 解析逻辑与 {@link CodePullRequestSyncServiceImpl} 一致，支持
 * {@code http(s)://host/owner/repo(.git)}、scp 风格 {@code git@host:owner/repo}、裸路径三种形态。
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
        OwnerRepo coords = parseOwnerRepo(repository.getSourceUrl());
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

    // ── sourceUrl 解析（与 CodePullRequestSyncServiceImpl 一致） ─────────

    private OwnerRepo parseOwnerRepo(String sourceUrl) {
        if (sourceUrl == null || sourceUrl.isBlank()) {
            throw invalidSourceUrl(sourceUrl);
        }
        String s = stripQueryAndFragment(sourceUrl.trim());
        while (s.endsWith("/")) {
            s = s.substring(0, s.length() - 1);
        }
        if (s.endsWith(".git")) {
            s = s.substring(0, s.length() - 4);
        }

        String path;
        int proto = s.indexOf("://");
        if (proto >= 0) {
            String afterProto = s.substring(proto + 3);
            int slash = afterProto.indexOf('/');
            path = slash >= 0 ? afterProto.substring(slash + 1) : "";
        } else if (s.contains("@") && s.contains(":")) {
            path = s.substring(s.indexOf(':') + 1);
        } else {
            path = s;
        }

        String[] parts = Arrays.stream(path.split("/"))
                .filter(p -> !p.isBlank())
                .toArray(String[]::new);
        if (parts.length < 2) {
            throw invalidSourceUrl(sourceUrl);
        }
        return new OwnerRepo(parts[parts.length - 2], parts[parts.length - 1]);
    }

    private String stripQueryAndFragment(String s) {
        int hash = s.indexOf('#');
        if (hash >= 0) {
            s = s.substring(0, hash);
        }
        int q = s.indexOf('?');
        if (q >= 0) {
            s = s.substring(0, q);
        }
        return s;
    }

    private BusinessException invalidSourceUrl(String sourceUrl) {
        return new BusinessException("REPOSITORY_SOURCE_URL_INVALID", HttpStatus.UNPROCESSABLE_ENTITY,
                "无法从仓库地址解析出 owner/repo", "sourceUrl=" + sourceUrl);
    }

    private record OwnerRepo(String owner, String repo) {
    }
}
