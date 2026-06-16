package com.gitguild.backend.review.repository;

import com.gitguild.backend.quest.domain.Quest;
import com.gitguild.backend.review.domain.Submission;
import com.gitguild.backend.review.domain.SubmissionStatus;
import java.util.Collection;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface SubmissionRepository extends JpaRepository<Submission, Long> {

    boolean existsByQuestAndSubmitterUserIdAndStatusIn(
            Quest quest,
            Long submitterId,
            Collection<SubmissionStatus> statuses);

    @Query("""
            select distinct s
            from Submission s
            join fetch s.quest q
            join fetch q.publisher publisher
            join fetch q.repository r
            join fetch r.owner owner
            join fetch s.submitter submitter
            join fetch s.pullRequest pr
            where :admin = true
               or publisher.userId = :reviewerId
               or owner.userId = :reviewerId
            order by s.submittedAt desc
            """)
    List<Submission> findReviewQueueForReviewer(
            @Param("reviewerId") Long reviewerId,
            @Param("admin") boolean admin);

    /** 指定 Quest 集合下的全部提交，供仓库级联删除清理使用。 */
    List<Submission> findByQuestQuestIdIn(Collection<Long> questIds);
}
