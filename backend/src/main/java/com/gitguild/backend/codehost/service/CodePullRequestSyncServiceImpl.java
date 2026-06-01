package com.gitguild.backend.codehost.service;

import com.gitguild.backend.codehost.domain.CodePullRequest;
import com.gitguild.backend.codehost.domain.CodeRepository;
import com.gitguild.backend.codehost.gitea.GiteaAdapter;
import com.gitguild.backend.codehost.gitea.dto.PrInfo;
import com.gitguild.backend.codehost.repository.CodePullRequestRepository;
import com.gitguild.backend.common.BusinessException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * {@link CodePullRequestSyncService} 的实现，隐藏 Gitea→本地 PR 落库的三处复杂度：
 * <ul>
 *   <li><b>仓库坐标解析</b>：从 {@code CodeRepository.sourceUrl} 还原 Gitea owner/repo，
 *       兼容 {@code http(s)://host/owner/repo(.git)}、scp 风格 {@code git@host:owner/repo}、裸路径三种形态；</li>
 *   <li><b>PR 状态映射</b>：把 Gitea 的 (state, merged) 归一为本地三态
 *       {@code OPEN}/{@code MERGED}/{@code CLOSED}（契约 §40 的工作台扩展态不在此产出）；</li>
 *   <li><b>幂等 upsert</b>：按唯一键 {@code (repository_id, external_pr_id)} 查找-或-新建，
 *       使重复同步同一 PR 不致触发唯一约束冲突（兑现 P4-016 已知问题清单第 5 条）。</li>
 * </ul>
 *
 * <p><b>业务不变量：</b>同一 PR 在本地至多一条记录；同步只更新 {@code status}，标题/分支/链接视为创建即固定。
 *
 * <p><b>边界错误模式：</b>{@code sourceUrl} 解析不出 owner/repo 抛 {@code REPOSITORY_SOURCE_URL_INVALID}；
 * Gitea 仓库不存在 / 不可达的 4xx 由 {@link com.gitguild.backend.codehost.gitea.GiteaAdapter}
 * 统一转 {@code CODE_HOST_RESOURCE_NOT_FOUND} / {@code CODE_HOST_UNAVAILABLE}，本类不再二次包装。
 */
@Service
public class CodePullRequestSyncServiceImpl implements CodePullRequestSyncService {

    private final GiteaAdapter giteaAdapter;
    private final CodePullRequestRepository pullRequestRepository;

    public CodePullRequestSyncServiceImpl(
            GiteaAdapter giteaAdapter,
            CodePullRequestRepository pullRequestRepository) {
        this.giteaAdapter = giteaAdapter;
        this.pullRequestRepository = pullRequestRepository;
    }

    @Override
    @Transactional
    public List<CodePullRequest> syncRepositoryPullRequests(CodeRepository repository) {
        OwnerRepo coords = parseOwnerRepo(repository.getSourceUrl());
        List<PrInfo> remotePulls = giteaAdapter.listPulls(coords.owner(), coords.repo());

        List<CodePullRequest> result = new ArrayList<>();
        for (PrInfo pr : remotePulls) {
            result.add(upsert(repository, pr));
        }
        return result;
    }

    /**
     * 单个 PR 的 upsert：先按 (repositoryId, externalPrId) 查找，存在则更新状态，不存在则新建。
     * 仅状态会随同步变化，标题/分支/链接视为创建即固定。
     */
    private CodePullRequest upsert(CodeRepository repository, PrInfo pr) {
        String externalPrId = String.valueOf(pr.number());
        String status = mapStatus(pr);
        String defaultBranch = repository.getDefaultBranch();

        CodePullRequest local = pullRequestRepository
                .findByRepositoryRepositoryIdAndExternalPrId(repository.getRepositoryId(), externalPrId)
                .orElseGet(() -> new CodePullRequest(
                        repository,
                        externalPrId,
                        firstNonBlank(pr.title(), "(untitled PR #" + externalPrId + ")"),
                        firstNonBlank(pr.headBranch(), defaultBranch),
                        firstNonBlank(pr.baseBranch(), defaultBranch),
                        status,
                        pr.htmlUrl()));
        local.setStatus(status);
        return pullRequestRepository.save(local);
    }

    /**
     * Gitea (state, merged) → 本地 status 映射（契约第 40 行）：
     * merged=true → MERGED；state=closed 且未合并 → CLOSED；其余 → OPEN。
     */
    private String mapStatus(PrInfo pr) {
        if (pr.merged()) {
            return "MERGED";
        }
        if ("closed".equalsIgnoreCase(pr.state())) {
            return "CLOSED";
        }
        return "OPEN";
    }

    /**
     * 从仓库 {@code sourceUrl} 解析 Gitea owner/repo。
     * 支持 http(s)://host/owner/repo(.git)、scp 风格 git@host:owner/repo(.git)、裸路径 owner/repo。
     */
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

    private String firstNonBlank(String value, String fallback) {
        return (value == null || value.isBlank()) ? fallback : value;
    }

    private BusinessException invalidSourceUrl(String sourceUrl) {
        return new BusinessException("REPOSITORY_SOURCE_URL_INVALID", HttpStatus.UNPROCESSABLE_ENTITY,
                "无法从仓库地址解析出 owner/repo", "sourceUrl=" + sourceUrl);
    }

    private record OwnerRepo(String owner, String repo) {
    }
}
