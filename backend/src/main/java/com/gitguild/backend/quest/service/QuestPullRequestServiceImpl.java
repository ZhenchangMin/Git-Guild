package com.gitguild.backend.quest.service;

import com.gitguild.backend.codehost.domain.CodePullRequest;
import com.gitguild.backend.codehost.domain.CodeRepository;
import com.gitguild.backend.codehost.gitea.GiteaAdapter;
import com.gitguild.backend.codehost.gitea.GiteaRepoCoordinates;
import com.gitguild.backend.codehost.gitea.dto.PrInfo;
import com.gitguild.backend.codehost.repository.CodePullRequestRepository;
import com.gitguild.backend.common.BusinessException;
import java.time.OffsetDateTime;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

/**
 * {@link QuestPullRequestService} 的实现。
 *
 * <p>建/合 PR 的 upsert 逻辑与 {@code CodeHostController} 保持一致（同一套
 * {@code upsertPullRequest}/{@code toLocalPrStatus}）。本类<b>不</b>标注
 * {@code @Transactional}，原因见接口与 {@link QuestTaskBranchServiceImpl} 注释。
 */
@Service
public class QuestPullRequestServiceImpl implements QuestPullRequestService {

    private static final Logger log = LoggerFactory.getLogger(QuestPullRequestServiceImpl.class);

    private final GiteaAdapter giteaAdapter;
    private final CodePullRequestRepository pullRequestRepository;

    public QuestPullRequestServiceImpl(
            GiteaAdapter giteaAdapter,
            CodePullRequestRepository pullRequestRepository) {
        this.giteaAdapter = giteaAdapter;
        this.pullRequestRepository = pullRequestRepository;
    }

    @Override
    public CodePullRequest ensurePullRequestForSubmission(
            CodeRepository repository, String headBranch, String baseBranch, String title, String body) {
        // 空分支预检：head==base 时 Gitea 无 diff，无法建 PR——给出干净的 422 而非 5xx。
        if (headBranch == null || headBranch.isBlank() || headBranch.equals(baseBranch)) {
            throw new BusinessException("TASK_BRANCH_EMPTY", HttpStatus.UNPROCESSABLE_ENTITY,
                    "任务分支没有可提交的改动，请先把提交推送到任务分支后再提交",
                    "head=" + headBranch + ", base=" + baseBranch);
        }
        GiteaRepoCoordinates coords = GiteaRepoCoordinates.parse(repository.getSourceUrl());
        try {
            PrInfo prInfo = giteaAdapter.createPullRequest(
                    coords.owner(), coords.repo(), title, body, headBranch, baseBranch);
            return upsertPullRequest(repository, prInfo, title, headBranch, baseBranch);
        } catch (BusinessException ex) {
            // 同一 head→base 的 PR 已存在（Gitea 409）：复用现有 open PR，保持幂等。
            if ("CODE_HOST_RESOURCE_CONFLICT".equals(ex.getCode())) {
                PrInfo existing = findOpenPullRequest(coords, headBranch, baseBranch);
                if (existing != null) {
                    log.info("PR 已存在，按幂等复用 repo={}/{}, head={}, base={}, number={}",
                            coords.owner(), coords.repo(), headBranch, baseBranch, existing.number());
                    return upsertPullRequest(repository, existing, title, headBranch, baseBranch);
                }
                throw ex;
            }
            // Gitea 对“无差异”分支返回 422，被 execute() 归入 CODE_HOST_UNAVAILABLE（details 以 -> HTTP 422 结尾）。
            if ("CODE_HOST_UNAVAILABLE".equals(ex.getCode())
                    && ex.getDetails() != null && ex.getDetails().contains("HTTP 422")) {
                throw new BusinessException("TASK_BRANCH_EMPTY", HttpStatus.UNPROCESSABLE_ENTITY,
                        "任务分支没有可提交的改动，请先把提交推送到任务分支后再提交",
                        "head=" + headBranch + ", base=" + baseBranch);
            }
            throw ex;
        }
    }

    @Override
    public CodePullRequest mergeForApproval(CodePullRequest pullRequest, CodeRepository repository) {
        if (pullRequest.isMerged()) {
            return pullRequest;
        }
        GiteaRepoCoordinates coords = GiteaRepoCoordinates.parse(repository.getSourceUrl());
        try {
            PrInfo prInfo = giteaAdapter.mergePullRequest(
                    coords.owner(), coords.repo(), parseExternalPrNumber(pullRequest));
            pullRequest.setStatus(toLocalPrStatus(prInfo));
            if (pullRequest.isMerged()) {
                pullRequest.setMergedAt(OffsetDateTime.now());
            }
            return pullRequestRepository.save(pullRequest);
        } catch (BusinessException ex) {
            if ("CODE_HOST_RESOURCE_CONFLICT".equals(ex.getCode())) {
                throw new BusinessException("PR_MERGE_CONFLICT", HttpStatus.CONFLICT,
                        "PR 存在冲突，无法自动合并，请先在 Gitea 解决冲突后再通过",
                        "pullRequestId=" + pullRequest.getPullRequestId());
            }
            throw ex;
        }
    }

    private PrInfo findOpenPullRequest(GiteaRepoCoordinates coords, String headBranch, String baseBranch) {
        return giteaAdapter.listPulls(coords.owner(), coords.repo()).stream()
                .filter(pr -> "open".equalsIgnoreCase(pr.state()))
                .filter(pr -> headBranch.equals(pr.headBranch()) && baseBranch.equals(pr.baseBranch()))
                .findFirst()
                .orElse(null);
    }

    private CodePullRequest upsertPullRequest(
            CodeRepository repository,
            PrInfo prInfo,
            String fallbackTitle,
            String fallbackSourceBranch,
            String fallbackTargetBranch) {
        String externalPrId = String.valueOf(prInfo.number());
        CodePullRequest pullRequest = pullRequestRepository
                .findByRepositoryRepositoryIdAndExternalPrId(repository.getRepositoryId(), externalPrId)
                .orElseGet(() -> new CodePullRequest(
                        repository,
                        externalPrId,
                        blankToDefault(prInfo.title(), fallbackTitle),
                        blankToDefault(prInfo.headBranch(), fallbackSourceBranch),
                        blankToDefault(prInfo.baseBranch(), fallbackTargetBranch),
                        toLocalPrStatus(prInfo),
                        prInfo.htmlUrl()));
        pullRequest.setStatus(toLocalPrStatus(prInfo));
        if (pullRequest.isMerged()) {
            pullRequest.setMergedAt(OffsetDateTime.now());
        }
        return pullRequestRepository.save(pullRequest);
    }

    private String toLocalPrStatus(PrInfo prInfo) {
        if (prInfo.merged()) {
            return "MERGED";
        }
        if ("closed".equalsIgnoreCase(prInfo.state())) {
            return "CLOSED";
        }
        return "OPEN";
    }

    private int parseExternalPrNumber(CodePullRequest pullRequest) {
        try {
            return Integer.parseInt(pullRequest.getExternalPrId());
        } catch (NumberFormatException ex) {
            throw new BusinessException("PR_EXTERNAL_ID_INVALID", HttpStatus.UNPROCESSABLE_ENTITY,
                    "PR 外部编号不是合法的 Gitea PR number",
                    "externalPrId=" + pullRequest.getExternalPrId());
        }
    }

    private String blankToDefault(String value, String defaultValue) {
        return value == null || value.isBlank() ? defaultValue : value;
    }
}
