package com.gitguild.backend.review.service;

import com.gitguild.backend.review.dto.ReviewSubmissionRequest;
import com.gitguild.backend.review.dto.SubmissionResponses.PullRequestBrief;
import com.gitguild.backend.review.dto.SubmissionResponses.ReviewRecordResponse;

public interface ReviewService {

    ReviewRecordResponse reviewSubmission(Long submissionId, Long reviewerId, ReviewSubmissionRequest request);

    /**
     * 代理合并提交对应的 PR（与「接受提交」解耦的独立动作）。
     *
     * <p>仅发布者 / 仓库属主 / ADMIN 可操作；已合并则幂等返回；冲突抛 {@code PR_MERGE_CONFLICT}(409)。
     */
    PullRequestBrief mergeSubmissionPullRequest(Long submissionId, Long reviewerId);
}
