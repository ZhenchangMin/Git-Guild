package com.gitguild.backend.review.controller;

import com.gitguild.backend.common.ApiResponse;
import com.gitguild.backend.review.dto.CreateSubmissionRequest;
import com.gitguild.backend.review.dto.ReviewSubmissionRequest;
import com.gitguild.backend.review.dto.SubmissionResponses.CreateSubmissionResponse;
import com.gitguild.backend.review.dto.SubmissionResponses.PullRequestBrief;
import com.gitguild.backend.review.dto.SubmissionResponses.ReviewRecordResponse;
import com.gitguild.backend.review.dto.SubmissionResponses.SubmissionDetailResponse;
import com.gitguild.backend.review.dto.SubmissionResponses.SubmissionReviewQueueItemResponse;
import com.gitguild.backend.review.service.ReviewService;
import com.gitguild.backend.review.service.SubmissionService;
import com.gitguild.backend.security.SecurityPrincipalUtils;
import jakarta.validation.Valid;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/submissions")
public class SubmissionController {

    private final SubmissionService submissionService;
    private final ReviewService reviewService;

    public SubmissionController(SubmissionService submissionService, ReviewService reviewService) {
        this.submissionService = submissionService;
        this.reviewService = reviewService;
    }

    @PostMapping
    public ResponseEntity<ApiResponse<CreateSubmissionResponse>> createSubmission(
            Authentication authentication,
            @Valid @RequestBody CreateSubmissionRequest request) {
        CreateSubmissionResponse response = submissionService.createSubmission(
                SecurityPrincipalUtils.currentUserId(authentication),
                request);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success("Submission created", response));
    }

    @GetMapping("/{submissionId}")
    public ApiResponse<SubmissionDetailResponse> getSubmission(
            @PathVariable Long submissionId,
            Authentication authentication) {
        return ApiResponse.success(submissionService.getSubmission(
                submissionId,
                SecurityPrincipalUtils.currentUserId(authentication)));
    }

    @GetMapping("/review-queue")
    public ApiResponse<List<SubmissionReviewQueueItemResponse>> listReviewQueue(Authentication authentication) {
        return ApiResponse.success(submissionService.listReviewQueue(
                SecurityPrincipalUtils.currentUserId(authentication)));
    }

    @PostMapping("/{submissionId}/reviews")
    public ResponseEntity<ApiResponse<ReviewRecordResponse>> reviewSubmission(
            @PathVariable Long submissionId,
            Authentication authentication,
            @Valid @RequestBody ReviewSubmissionRequest request) {
        ReviewRecordResponse response = reviewService.reviewSubmission(
                submissionId,
                SecurityPrincipalUtils.currentUserId(authentication),
                request);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success("Submission review completed", response));
    }

    @PostMapping("/{submissionId}/merge")
    public ApiResponse<PullRequestBrief> mergeSubmissionPullRequest(
            @PathVariable Long submissionId,
            Authentication authentication) {
        PullRequestBrief response = reviewService.mergeSubmissionPullRequest(
                submissionId,
                SecurityPrincipalUtils.currentUserId(authentication));
        return ApiResponse.success("MERGED", response);
    }
}
