package com.gitguild.backend.review.repository;

import com.gitguild.backend.quest.domain.Quest;
import com.gitguild.backend.review.domain.Submission;
import com.gitguild.backend.review.domain.SubmissionStatus;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
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

    /** 工作台「退回修改」节点：某接取者在某任务下的全部提交，按提交时间升序排列，便于回溯历史。 */
    List<Submission> findByQuestAndSubmitterUserIdOrderBySubmittedAtAsc(Quest quest, Long submitterId);

    /** 委托人「我发布的委托」：某任务下最近一次提交（不限提交者），用于在 Quest 仍为 IN_REVIEW 时区分「已要求修改」。 */
    Optional<Submission> findFirstByQuestQuestIdOrderBySubmittedAtDesc(Long questId);

    @Query("""
            select s
            from Submission s
            join fetch s.quest q
            join fetch s.pullRequest pr
            where s.submitter.userId = :submitterId
            order by s.submittedAt desc
            """)
    List<Submission> findBySubmitterUserIdWithQuestOrderBySubmittedAtDesc(@Param("submitterId") Long submitterId);

    @Query("""
            select s
            from Submission s
            join fetch s.quest q
            join fetch s.pullRequest pr
            where s.submitter.userId = :submitterId
              and (:status is null or s.status = :status)
            order by s.submittedAt desc
            """)
    List<Submission> findBySubmitterUserIdAndOptionalStatusWithQuestOrderBySubmittedAtDesc(
            @Param("submitterId") Long submitterId,
            @Param("status") SubmissionStatus status);
}
