package com.gitguild.backend.review.repository;

import com.gitguild.backend.quest.domain.Quest;
import com.gitguild.backend.review.domain.Submission;
import com.gitguild.backend.review.domain.SubmissionStatus;
import java.util.Collection;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface SubmissionRepository extends JpaRepository<Submission, Long> {

    boolean existsByQuestAndSubmitterUserIdAndStatusIn(
            Quest quest,
            Long submitterId,
            Collection<SubmissionStatus> statuses);

    List<Submission> findByStatusOrderBySubmittedAtDesc(SubmissionStatus status);

    List<Submission> findAllByOrderBySubmittedAtDesc();
}
