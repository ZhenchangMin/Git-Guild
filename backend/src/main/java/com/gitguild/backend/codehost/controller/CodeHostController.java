package com.gitguild.backend.codehost.controller;

import com.gitguild.backend.codehost.domain.CodeIssue;
import com.gitguild.backend.codehost.domain.CodePullRequest;
import com.gitguild.backend.codehost.domain.CodeRepository;
import com.gitguild.backend.codehost.gitea.GiteaAdapter;
import com.gitguild.backend.codehost.gitea.GiteaRepoCoordinates;
import com.gitguild.backend.codehost.gitea.dto.BranchInfo;
import com.gitguild.backend.codehost.gitea.dto.FileCommitInfo;
import com.gitguild.backend.codehost.gitea.dto.PrInfo;
import com.gitguild.backend.codehost.repository.CodeIssueRepository;
import com.gitguild.backend.codehost.repository.CodePullRequestRepository;
import com.gitguild.backend.codehost.repository.CodeRepositoryRepository;
import com.gitguild.backend.codehost.service.RepositoryService;
import com.gitguild.backend.codehost.service.RepositorySyncService;
import com.gitguild.backend.common.ApiResponse;
import com.gitguild.backend.common.BusinessException;
import com.gitguild.backend.security.SecurityPrincipalUtils;
import java.time.OffsetDateTime;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1")
public class CodeHostController {

    private final GiteaAdapter giteaAdapter;
    private final RepositoryService repositoryService;
    private final RepositorySyncService repositorySyncService;
    private final CodeRepositoryRepository repositoryRepository;
    private final CodeIssueRepository issueRepository;
    private final CodePullRequestRepository pullRequestRepository;

    public CodeHostController(
            GiteaAdapter giteaAdapter,
            RepositoryService repositoryService,
            RepositorySyncService repositorySyncService,
            CodeRepositoryRepository repositoryRepository,
            CodeIssueRepository issueRepository,
            CodePullRequestRepository pullRequestRepository) {
        this.giteaAdapter = giteaAdapter;
        this.repositoryService = repositoryService;
        this.repositorySyncService = repositorySyncService;
        this.repositoryRepository = repositoryRepository;
        this.issueRepository = issueRepository;
        this.pullRequestRepository = pullRequestRepository;
    }

    @PostMapping("/repositories/import")
    public ApiResponse<RepositoryResponse> importRepository(
            @RequestBody ImportRepositoryRequest request,
            Authentication authentication) {
        // 委派给服务层：内网地址直接登记，外网地址自动迁入平台 Gitea（带 Issue/PR）后登记。
        CodeRepository repository = repositoryService.importRepository(
                SecurityPrincipalUtils.currentUserId(authentication),
                request.sourceUrl(),
                request.name(),
                request.hostType());
        return ApiResponse.success("CREATED", RepositoryResponse.from(repository));
    }

    @GetMapping("/repositories/{repositoryId}")
    public ApiResponse<RepositoryResponse> getRepository(@PathVariable Long repositoryId) {
        return ApiResponse.success(RepositoryResponse.from(findRepository(repositoryId)));
    }

    @PostMapping("/repositories/{repositoryId}/sync")
    public ApiResponse<RepositoryResponse> syncRepository(@PathVariable Long repositoryId) {
        // 同步逻辑下沉到 RepositorySyncService：与异常中心「重试」共用一套实现，失败自动登记异常。
        CodeRepository repository = repositorySyncService.syncRepository(repositoryId);
        return ApiResponse.success(RepositoryResponse.from(repository));
    }

    @GetMapping("/repositories/{repositoryId}/issues")
    public ApiResponse<IssuePageResponse> listIssues(
            @PathVariable Long repositoryId,
            @RequestParam(required = false) String status,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "20") int size) {
        validatePage(page, size);
        findRepository(repositoryId);
        List<IssueResponse> filtered = issueRepository.findAll().stream()
                .filter(issue -> issue.getRepository().getRepositoryId().equals(repositoryId))
                .filter(issue -> status == null || status.isBlank() || issue.getStatus().equalsIgnoreCase(status))
                .map(IssueResponse::from)
                .toList();
        int from = Math.min((page - 1) * size, filtered.size());
        int to = Math.min(from + size, filtered.size());
        return ApiResponse.success(new IssuePageResponse(filtered.subList(from, to), page, size, filtered.size()));
    }

    @PostMapping("/repositories/{repositoryId}/branches")
    public ApiResponse<BranchResponse> createBranch(
            @PathVariable Long repositoryId,
            @RequestBody BranchRequest request) {
        CodeRepository repository = findRepository(repositoryId);
        if (request.branchName() == null || request.branchName().isBlank()) {
            throw validation("branchName is required");
        }
        GiteaRepoCoordinates coords = GiteaRepoCoordinates.parse(repository.getSourceUrl());
        String baseBranch = blankToDefault(request.baseBranch(), repository.getDefaultBranch());
        BranchInfo branch = giteaAdapter.createBranch(coords.owner(), coords.repo(), request.branchName().trim(), baseBranch);
        return ApiResponse.success("CREATED", new BranchResponse(branch.name(), baseBranch, "CREATED"));
    }

    @PostMapping("/repositories/{repositoryId}/commits")
    public ApiResponse<CommitResponse> createCommit(
            @PathVariable Long repositoryId,
            @RequestBody CommitRequest request) {
        CodeRepository repository = findRepository(repositoryId);
        if (request.branch() == null || request.branch().isBlank()) {
            throw validation("branch is required");
        }
        if (request.message() == null || request.message().isBlank()) {
            throw validation("message is required");
        }
        GiteaRepoCoordinates coords = GiteaRepoCoordinates.parse(repository.getSourceUrl());
        String branch = request.branch().trim();
        String path = blankToDefault(request.path(), defaultCommitPath(repositoryId, branch));
        String content = blankToDefault(request.content(), defaultCommitContent(repository, branch));
        FileCommitInfo commit = giteaAdapter.createFile(
                coords.owner(),
                coords.repo(),
                branch,
                path,
                request.message().trim(),
                content);
        return ApiResponse.success("CREATED", new CommitResponse(
                commit.commitSha(),
                commit.branch(),
                request.message().trim(),
                commit.path(),
                commit.htmlUrl()));
    }

    @PostMapping("/repositories/{repositoryId}/pull-requests")
    public ApiResponse<PullRequestResponse> createPullRequest(
            @PathVariable Long repositoryId,
            @RequestBody PullRequestRequest request) {
        CodeRepository repository = findRepository(repositoryId);
        if (request.title() == null || request.title().isBlank()) {
            throw validation("title is required");
        }
        if (request.sourceBranch() == null || request.sourceBranch().isBlank()) {
            throw validation("sourceBranch is required");
        }
        GiteaRepoCoordinates coords = GiteaRepoCoordinates.parse(repository.getSourceUrl());
        String targetBranch = blankToDefault(request.targetBranch(), repository.getDefaultBranch());
        PrInfo prInfo = giteaAdapter.createPullRequest(
                coords.owner(),
                coords.repo(),
                request.title().trim(),
                request.body(),
                request.sourceBranch().trim(),
                targetBranch);
        CodePullRequest pullRequest = upsertPullRequest(repository, prInfo, request.title().trim(), request.sourceBranch().trim(), targetBranch);
        return ApiResponse.success("CREATED", PullRequestResponse.from(pullRequest));
    }

    @GetMapping("/repositories/{repositoryId}/pull-requests/{pullRequestId}")
    public ApiResponse<PullRequestResponse> getPullRequest(
            @PathVariable Long repositoryId,
            @PathVariable Long pullRequestId) {
        CodePullRequest pullRequest = pullRequestRepository
                .findByPullRequestIdAndRepositoryRepositoryId(pullRequestId, repositoryId)
                .orElseThrow(() -> notFound("PULL_REQUEST_NOT_FOUND", "Pull request 不存在"));
        return ApiResponse.success(PullRequestResponse.from(pullRequest));
    }

    @PostMapping("/repositories/{repositoryId}/pull-requests/{pullRequestId}/merge")
    public ApiResponse<PullRequestResponse> mergePullRequest(
            @PathVariable Long repositoryId,
            @PathVariable Long pullRequestId) {
        CodeRepository repository = findRepository(repositoryId);
        CodePullRequest pullRequest = pullRequestRepository
                .findByPullRequestIdAndRepositoryRepositoryId(pullRequestId, repositoryId)
                .orElseThrow(() -> notFound("PULL_REQUEST_NOT_FOUND", "Pull request 不存在"));
        GiteaRepoCoordinates coords = GiteaRepoCoordinates.parse(repository.getSourceUrl());
        PrInfo prInfo = giteaAdapter.mergePullRequest(
                coords.owner(),
                coords.repo(),
                parseExternalPrNumber(pullRequest));
        pullRequest.setStatus(toLocalPrStatus(prInfo));
        if (pullRequest.isMerged()) {
            pullRequest.setMergedAt(OffsetDateTime.now());
        }
        return ApiResponse.success("MERGED", PullRequestResponse.from(pullRequestRepository.save(pullRequest)));
    }

    @PostMapping("/code-host/webhooks/{hostType}")
    public ApiResponse<WebhookResponse> receiveWebhook(@PathVariable String hostType, @RequestBody(required = false) String payload) {
        return ApiResponse.success(new WebhookResponse(hostType, "ACCEPTED", payload == null ? 0 : payload.length(), OffsetDateTime.now()));
    }

    private CodeRepository findRepository(Long repositoryId) {
        return repositoryRepository.findById(repositoryId)
                .orElseThrow(() -> notFound("REPOSITORY_NOT_FOUND", "仓库不存在"));
    }

    private String blankToDefault(String value, String defaultValue) {
        return value == null || value.isBlank() ? defaultValue : value.trim();
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
            throw validation("externalPrId is not a Gitea pull request number: " + pullRequest.getExternalPrId());
        }
    }

    private String defaultCommitPath(Long repositoryId, String branch) {
        String normalizedBranch = branch.replaceAll("[^a-zA-Z0-9._-]", "-");
        return "gitguild-proof-" + repositoryId + "-" + normalizedBranch + "-" + System.nanoTime() + ".md";
    }

    private String defaultCommitContent(CodeRepository repository, String branch) {
        return "# Git-Guild task proof\n\n"
                + "- Repository: " + repository.getName() + "\n"
                + "- Branch: " + branch + "\n"
                + "- Generated by: Git-Guild Workbench\n";
    }

    private void validatePage(int page, int size) {
        if (page < 1 || size < 1 || size > 100) {
            throw validation("page must be >= 1 and size must be between 1 and 100");
        }
    }

    private BusinessException validation(String details) {
        return new BusinessException("VALIDATION_FAILED", HttpStatus.BAD_REQUEST, "请求参数不合法", details);
    }

    private BusinessException notFound(String code, String message) {
        return new BusinessException(code, HttpStatus.NOT_FOUND, message);
    }

    public record ImportRepositoryRequest(String sourceUrl, String name, String hostType) {
    }

    public record RepositoryResponse(Long repositoryId, String name, String hostType, String sourceUrl, String defaultBranch, String syncStatus) {
        static RepositoryResponse from(CodeRepository repository) {
            return new RepositoryResponse(
                    repository.getRepositoryId(),
                    repository.getName(),
                    repository.getHostType(),
                    repository.getSourceUrl(),
                    repository.getDefaultBranch(),
                    repository.getSyncStatus());
        }
    }

    public record IssueResponse(Long issueId, String externalIssueId, String title, String status) {
        static IssueResponse from(CodeIssue issue) {
            return new IssueResponse(issue.getIssueId(), issue.getExternalIssueId(), issue.getTitle(), issue.getStatus());
        }
    }

    public record IssuePageResponse(List<IssueResponse> items, int page, int size, int total) {
    }

    public record BranchRequest(String branchName, String baseBranch) {
    }

    public record BranchResponse(String branchName, String baseBranch, String status) {
    }

    public record CommitRequest(String branch, String message, String path, String content) {
    }

    public record CommitResponse(String commitId, String branch, String message, String path, String externalUrl) {
    }

    public record PullRequestRequest(String title, String body, String sourceBranch, String targetBranch) {
    }

    public record PullRequestResponse(Long pullRequestId, String externalPrId, String title, String sourceBranch, String targetBranch, String status, String externalUrl) {
        static PullRequestResponse from(CodePullRequest pullRequest) {
            return new PullRequestResponse(
                    pullRequest.getPullRequestId(),
                    pullRequest.getExternalPrId(),
                    pullRequest.getTitle(),
                    pullRequest.getSourceBranch(),
                    pullRequest.getTargetBranch(),
                    pullRequest.getStatus(),
                    pullRequest.getExternalUrl());
        }
    }

    public record WebhookResponse(String hostType, String status, int payloadSize, OffsetDateTime receivedAt) {
    }
}
