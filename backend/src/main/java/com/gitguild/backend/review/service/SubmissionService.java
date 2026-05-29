package com.gitguild.backend.review.service;

import com.gitguild.backend.review.dto.CreateSubmissionRequest;
import com.gitguild.backend.review.dto.SubmissionResponses.CreateSubmissionResponse;
import com.gitguild.backend.review.dto.SubmissionResponses.SubmissionDetailResponse;

public interface SubmissionService {

    CreateSubmissionResponse createSubmission(Long submitterId, CreateSubmissionRequest request);

    SubmissionDetailResponse getSubmission(Long submissionId, Long currentUserId);
}
