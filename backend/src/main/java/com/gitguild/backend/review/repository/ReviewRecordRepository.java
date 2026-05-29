package com.gitguild.backend.review.repository;

import com.gitguild.backend.review.domain.ReviewRecord;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ReviewRecordRepository extends JpaRepository<ReviewRecord, Long> {

    List<ReviewRecord> findBySubmissionSubmissionIdOrderByReviewedAtDesc(Long submissionId);
}
