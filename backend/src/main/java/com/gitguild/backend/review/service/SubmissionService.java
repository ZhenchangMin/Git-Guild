package com.gitguild.backend.review.service;

import com.gitguild.backend.review.dto.CreateSubmissionRequest;
import com.gitguild.backend.review.dto.SubmissionResponses.CreateSubmissionResponse;
import com.gitguild.backend.review.dto.SubmissionResponses.SubmissionDetailResponse;
import com.gitguild.backend.review.dto.SubmissionResponses.SubmissionReviewQueueItemResponse;
import java.util.List;

public interface SubmissionService {

    CreateSubmissionResponse createSubmission(Long submitterId, CreateSubmissionRequest request);

    SubmissionDetailResponse getSubmission(Long submissionId, Long currentUserId);

    List<SubmissionReviewQueueItemResponse> listReviewQueue(Long currentUserId);
}
