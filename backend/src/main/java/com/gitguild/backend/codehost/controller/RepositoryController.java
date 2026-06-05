package com.gitguild.backend.codehost.controller;

import com.gitguild.backend.codehost.domain.CodeRepository;
import com.gitguild.backend.codehost.dto.CreateRepositoryRequest;
import com.gitguild.backend.codehost.service.RepositoryService;
import com.gitguild.backend.common.ApiResponse;
import com.gitguild.backend.security.SecurityPrincipalUtils;
import jakarta.validation.Valid;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 仓库接入端点（Issue #9）。
 *
 * <p>Guild Master 通过 {@code POST /repositories} 在本地 Gitea 创建新仓库；
 * {@code GET /repositories} 返回当前用户有权限的仓库列表。
 */
@RestController
@RequestMapping("/api/v1/repositories")
public class RepositoryController {

    private final RepositoryService repositoryService;

    public RepositoryController(RepositoryService repositoryService) {
        this.repositoryService = repositoryService;
    }

    @PostMapping
    public ResponseEntity<ApiResponse<RepositoryResponse>> createRepository(
            Authentication authentication,
            @Valid @RequestBody CreateRepositoryRequest request) {
        CodeRepository repo = repositoryService.createRepository(
                SecurityPrincipalUtils.currentUserId(authentication), request);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success("仓库已创建",
                        new RepositoryResponse(
                                repo.getRepositoryId(),
                                repo.getName(),
                                repo.getSourceUrl(),
                                repo.getDefaultBranch(),
                                repo.getDescription())));
    }

    @GetMapping
    public ApiResponse<List<RepositoryResponse>> listRepositories(Authentication authentication) {
        List<RepositoryResponse> items = repositoryService
                .listRepositories(SecurityPrincipalUtils.currentUserId(authentication))
                .stream()
                .map(r -> new RepositoryResponse(
                        r.getRepositoryId(), r.getName(), r.getSourceUrl(),
                        r.getDefaultBranch(), r.getDescription()))
                .toList();
        return ApiResponse.success(items);
    }

    /**
     * 仓库摘要响应体——列表和创建接口共享。
     */
    public record RepositoryResponse(
            Long repositoryId,
            String name,
            String sourceUrl,
            String defaultBranch,
            String description) {
    }
}
