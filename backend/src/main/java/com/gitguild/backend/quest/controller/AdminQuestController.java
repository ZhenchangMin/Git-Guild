package com.gitguild.backend.quest.controller;

import com.gitguild.backend.common.ApiResponse;
import com.gitguild.backend.quest.dto.AdminReviewRequest;
import com.gitguild.backend.quest.dto.QuestResponses.AdminQuestPageResponse;
import com.gitguild.backend.quest.dto.QuestResponses.AdminReviewResponse;
import com.gitguild.backend.quest.service.AdminQuestService;
import com.gitguild.backend.security.SecurityPrincipalUtils;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
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
 * Admin 审核操作的 HTTP 入口，全部端点均要求 ADMIN 角色（由 {@code @PreAuthorize} 在方法级强制）。
 * 角色校验失败时由 Spring Security 统一返回 403，不进入业务层。
 *
 * <p>端点：
 * <ul>
 *   <li>{@code GET /api/v1/admin/quests} — 查询 PENDING_ADMIN_REVIEW 的 Quest 分页列表</li>
 *   <li>{@code POST /api/v1/quests/{questId}/admin-reviews} — 对 Quest 执行审核决策，成功返回 201</li>
 * </ul>
 */
@RestController
@RequestMapping("/api/v1")
@PreAuthorize("hasRole('ADMIN')")
public class AdminQuestController {

    private final AdminQuestService adminQuestService;

    public AdminQuestController(AdminQuestService adminQuestService) {
        this.adminQuestService = adminQuestService;
    }

    @GetMapping("/admin/quests")
    public ApiResponse<AdminQuestPageResponse> listPendingQuests(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "10") int size) {
        return ApiResponse.success(adminQuestService.listPendingQuests(page, size));
    }

    @PostMapping("/quests/{questId}/admin-reviews")
    public ResponseEntity<ApiResponse<AdminReviewResponse>> reviewQuest(
            @PathVariable Long questId,
            Authentication authentication,
            @Valid @RequestBody AdminReviewRequest request) {
        AdminReviewResponse response = adminQuestService.reviewQuest(
                questId, SecurityPrincipalUtils.currentUserId(authentication), request);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success("审核完成", response));
    }
}
