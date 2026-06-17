package com.gitguild.backend.ops.controller;

import com.gitguild.backend.common.ApiResponse;
import com.gitguild.backend.ops.dto.ExceptionResponses.ExceptionListResponse;
import com.gitguild.backend.ops.dto.ExceptionResponses.ExceptionView;
import com.gitguild.backend.ops.dto.ResolveExceptionRequest;
import com.gitguild.backend.ops.service.AdminExceptionService;
import com.gitguild.backend.security.SecurityPrincipalUtils;
import jakarta.validation.Valid;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * 异常处理中心的 HTTP 入口，全部端点要求 ADMIN 角色（方法级 {@code @PreAuthorize} 强制）。
 *
 * <p>端点：
 * <ul>
 *   <li>{@code GET  /api/v1/admin/exceptions} — 按分类/状态筛选异常列表</li>
 *   <li>{@code GET  /api/v1/admin/exceptions/{id}} — 异常详情</li>
 *   <li>{@code POST /api/v1/admin/exceptions/{id}/resolve} — 处置（action + comment）</li>
 *   <li>{@code POST /api/v1/admin/exceptions/{id}/retry} — 对可重试的 SYNC 异常重新发起同步</li>
 * </ul>
 */
@RestController
@RequestMapping("/api/v1/admin/exceptions")
@PreAuthorize("hasRole('ADMIN')")
public class AdminExceptionController {

    private final AdminExceptionService adminExceptionService;

    public AdminExceptionController(AdminExceptionService adminExceptionService) {
        this.adminExceptionService = adminExceptionService;
    }

    @GetMapping
    public ApiResponse<ExceptionListResponse> list(
            @RequestParam(required = false) String category,
            @RequestParam(required = false) String status) {
        return ApiResponse.success(
                ExceptionListResponse.of(adminExceptionService.listExceptions(category, status)));
    }

    @GetMapping("/{exceptionId}")
    public ApiResponse<ExceptionView> get(@PathVariable Long exceptionId) {
        return ApiResponse.success(ExceptionView.from(adminExceptionService.getException(exceptionId)));
    }

    @PostMapping("/{exceptionId}/resolve")
    public ApiResponse<ExceptionView> resolve(
            @PathVariable Long exceptionId,
            @Valid @RequestBody ResolveExceptionRequest request,
            Authentication authentication) {
        Long adminId = SecurityPrincipalUtils.currentUserId(authentication);
        return ApiResponse.success("已处理",
                ExceptionView.from(adminExceptionService.resolve(adminId, exceptionId, request)));
    }

    @PostMapping("/{exceptionId}/retry")
    public ApiResponse<ExceptionView> retry(
            @PathVariable Long exceptionId,
            Authentication authentication) {
        Long adminId = SecurityPrincipalUtils.currentUserId(authentication);
        return ApiResponse.success("已发起重试",
                ExceptionView.from(adminExceptionService.retry(adminId, exceptionId)));
    }
}
