package com.gitguild.backend.codehost.controller;

import com.gitguild.backend.codehost.domain.CodeIssue;
import com.gitguild.backend.codehost.domain.CodePullRequest;
import com.gitguild.backend.codehost.domain.CodeRepository;
import com.gitguild.backend.codehost.gitea.GiteaAdapter;
import com.gitguild.backend.codehost.gitea.dto.IssueInfo;
import com.gitguild.backend.codehost.repository.CodeIssueRepository;
import com.gitguild.backend.codehost.repository.CodePullRequestRepository;
import com.gitguild.backend.codehost.repository.CodeRepositoryRepository;
import com.gitguild.backend.common.ApiResponse;
import com.gitguild.backend.common.BusinessException;
import com.gitguild.backend.security.SecurityPrincipalUtils;
import com.gitguild.backend.user.domain.User;
import com.gitguild.backend.user.repository.UserRepository;
import java.net.URI;
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

    private final CodeRepositoryRepository repositoryRepository;
    private final CodeIssueRepository issueRepository;
    private final CodePullRequestRepository pullRequestRepository;
    private final UserRepository userRepository;
    private final GiteaAdapter giteaAdapter;

    public CodeHostController(
            CodeRepositoryRepository repositoryRepository,
            CodeIssueRepository issueRepository,
            CodePullRequestRepository pullRequestRepository,
            UserRepository userRepository,
            GiteaAdapter giteaAdapter) {
        this.repositoryRepository = repositoryRepository;
        this.issueRepository = issueRepository;
        this.pullRequestRepository = pullRequestRepository;
        this.userRepository = userRepository;
        this.giteaAdapter = giteaAdapter;
    }

    @PostMapping("/repositories/import")
    public ApiResponse<RepositoryResponse> importRepository(
            @RequestBody ImportRepositoryRequest request,
            Authentication authentication) {
        if (request.sourceUrl() == null || request.sourceUrl().isBlank()) {
            throw validation("sourceUrl is required");
        }
        User owner = currentUser(authentication);
        String sourceUrl = request.sourceUrl().trim();
        String hostType = request.hostType() == null || request.hostType().isBlank() ? "GITEA" : request.hostType().trim();
        CodeRepository repository = repositoryRepository
                .findFirstByHostTypeAndSourceUrlOrderByRepositoryIdAsc(hostType, sourceUrl)
                .orElseGet(() -> repositoryRepository.save(new CodeRepository(
                        owner,
                        request.name() == null || request.name().isBlank() ? inferName(sourceUrl) : request.name().trim(),
                        hostType,
                        sourceUrl)));
        return ApiResponse.success("CREATED", RepositoryResponse.from(repository));
    }

    @GetMapping("/repositories/{repositoryId}")
    public ApiResponse<RepositoryResponse> getRepository(@PathVariable Long repositoryId) {
        return ApiResponse.success(RepositoryResponse.from(findRepository(repositoryId)));
    }

    @PostMapping("/repositories/{repositoryId}/sync")
    public ApiResponse<RepositoryResponse> syncRepository(@PathVariable Long repositoryId) {
        CodeRepository repository = findRepository(repositoryId);
        syncIssues(repository);
        repository.markSynced();
        return ApiResponse.success(RepositoryResponse.from(repositoryRepository.save(repository)));
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
        findRepository(repositoryId);
        if (request.branchName() == null || request.branchName().isBlank()) {
            throw validation("branchName is required");
        }
        return ApiResponse.success("CREATED", new BranchResponse(request.branchName().trim(), request.baseBranch(), "CREATED"));
    }

    @PostMapping("/repositories/{repositoryId}/commits")
    public ApiResponse<CommitResponse> createCommit(
            @PathVariable Long repositoryId,
            @RequestBody CommitRequest request) {
        findRepository(repositoryId);
        if (request.message() == null || request.message().isBlank()) {
            throw validation("message is required");
        }
        return ApiResponse.success("CREATED", new CommitResponse("local-" + System.nanoTime(), request.branch(), request.message()));
    }

    @PostMapping("/repositories/{repositoryId}/pull-requests")
    public ApiResponse<PullRequestResponse> createPullRequest(
            @PathVariable Long repositoryId,
            @RequestBody PullRequestRequest request) {
        CodeRepository repository = findRepository(repositoryId);
        if (request.title() == null || request.title().isBlank()) {
            throw validation("title is required");
        }
        CodePullRequest pullRequest = pullRequestRepository.save(new CodePullRequest(
                repository,
                String.valueOf(System.nanoTime()),
                request.title().trim(),
                blankToDefault(request.sourceBranch(), "feature/local"),
                blankToDefault(request.targetBranch(), repository.getDefaultBranch()),
                "OPEN",
                repository.getSourceUrl()));
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

    @PostMapping("/code-host/webhooks/{hostType}")
    public ApiResponse<WebhookResponse> receiveWebhook(@PathVariable String hostType, @RequestBody(required = false) String payload) {
        return ApiResponse.success(new WebhookResponse(hostType, "ACCEPTED", payload == null ? 0 : payload.length(), OffsetDateTime.now()));
    }

    private User currentUser(Authentication authentication) {
        Long userId = SecurityPrincipalUtils.currentUserId(authentication);
        return userRepository.findById(userId)
                .orElseThrow(() -> notFound("USER_NOT_FOUND", "用户不存在"));
    }

    private CodeRepository findRepository(Long repositoryId) {
        return repositoryRepository.findById(repositoryId)
                .orElseThrow(() -> notFound("REPOSITORY_NOT_FOUND", "仓库不存在"));
    }

    private String inferName(String sourceUrl) {
        try {
            String path = URI.create(sourceUrl).getPath();
            String name = path == null || path.isBlank() ? "repository" : path.substring(path.lastIndexOf('/') + 1);
            return name.endsWith(".git") ? name.substring(0, name.length() - 4) : name;
        } catch (IllegalArgumentException ex) {
            return "repository";
        }
    }

    private void syncIssues(CodeRepository repository) {
        OwnerRepo coords = parseOwnerRepo(repository.getSourceUrl());
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

    private OwnerRepo parseOwnerRepo(String sourceUrl) {
        if (sourceUrl == null || sourceUrl.isBlank()) {
            throw new BusinessException("REPOSITORY_SOURCE_URL_INVALID", HttpStatus.UNPROCESSABLE_ENTITY,
                    "无法从仓库地址解析出 owner/repo", "sourceUrl=" + sourceUrl);
        }
        String value = sourceUrl.trim();
        int hash = value.indexOf('#');
        if (hash >= 0) {
            value = value.substring(0, hash);
        }
        int query = value.indexOf('?');
        if (query >= 0) {
            value = value.substring(0, query);
        }
        while (value.endsWith("/")) {
            value = value.substring(0, value.length() - 1);
        }
        if (value.endsWith(".git")) {
            value = value.substring(0, value.length() - 4);
        }

        String path = value;
        int proto = value.indexOf("://");
        if (proto >= 0) {
            String afterProto = value.substring(proto + 3);
            int slash = afterProto.indexOf('/');
            path = slash >= 0 ? afterProto.substring(slash + 1) : "";
        } else if (value.contains("@") && value.contains(":")) {
            path = value.substring(value.indexOf(':') + 1);
        }

        String[] parts = java.util.Arrays.stream(path.split("/"))
                .filter(part -> !part.isBlank())
                .toArray(String[]::new);
        if (parts.length < 2) {
            throw new BusinessException("REPOSITORY_SOURCE_URL_INVALID", HttpStatus.UNPROCESSABLE_ENTITY,
                    "无法从仓库地址解析出 owner/repo", "sourceUrl=" + sourceUrl);
        }
        return new OwnerRepo(parts[parts.length - 2], parts[parts.length - 1]);
    }

    private String blankToDefault(String value, String defaultValue) {
        return value == null || value.isBlank() ? defaultValue : value.trim();
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

    public record CommitRequest(String branch, String message) {
    }

    public record CommitResponse(String commitId, String branch, String message) {
    }

    public record PullRequestRequest(String title, String sourceBranch, String targetBranch) {
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

    private record OwnerRepo(String owner, String repo) {
    }
}
