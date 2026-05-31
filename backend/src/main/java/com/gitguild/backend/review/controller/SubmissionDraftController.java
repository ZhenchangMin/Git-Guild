package com.gitguild.backend.review.controller;

import com.gitguild.backend.common.ApiResponse;
import com.gitguild.backend.review.dto.SubmissionDraftResponse;
import com.gitguild.backend.review.service.SubmissionDraftService;
import com.gitguild.backend.security.SecurityPrincipalUtils;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 提交柜台草稿端点。挂在 {@code /api/v1/quests} 下与契约第 143 行一致，
 * 与 {@link com.gitguild.backend.quest.controller.QuestController} 共用基址但端点不冲突。
 */
@RestController
@RequestMapping("/api/v1/quests")
public class SubmissionDraftController {

    private final SubmissionDraftService submissionDraftService;

    public SubmissionDraftController(SubmissionDraftService submissionDraftService) {
        this.submissionDraftService = submissionDraftService;
    }

    @GetMapping("/{questId}/submission-draft")
    public ApiResponse<SubmissionDraftResponse> getSubmissionDraft(
            @PathVariable Long questId,
            Authentication authentication) {
        return ApiResponse.success(submissionDraftService.getDraft(
                questId,
                SecurityPrincipalUtils.currentUserId(authentication)));
    }
}
