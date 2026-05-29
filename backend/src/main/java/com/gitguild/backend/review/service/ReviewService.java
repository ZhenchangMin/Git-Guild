package com.gitguild.backend.review.service;

import com.gitguild.backend.review.dto.ReviewSubmissionRequest;
import com.gitguild.backend.review.dto.SubmissionResponses.ReviewRecordResponse;

public interface ReviewService {

    ReviewRecordResponse reviewSubmission(Long submissionId, Long reviewerId, ReviewSubmissionRequest request);
}
