package com.gitguild.backend.review.repository;

import com.gitguild.backend.review.domain.ReviewRecord;
import java.util.Collection;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ReviewRecordRepository extends JpaRepository<ReviewRecord, Long> {

    List<ReviewRecord> findBySubmissionSubmissionIdOrderByReviewedAtDesc(Long submissionId);

    /** 指定提交集合下的全部评审记录，供仓库级联删除清理使用（删除会级联其 review_items）。 */
    List<ReviewRecord> findBySubmissionSubmissionIdIn(Collection<Long> submissionIds);
}
