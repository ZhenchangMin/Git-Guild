package com.gitguild.backend.quest.repository;

import com.gitguild.backend.quest.domain.AssignmentStatus;
import com.gitguild.backend.quest.domain.Quest;
import com.gitguild.backend.quest.domain.QuestAssignment;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface QuestAssignmentRepository extends JpaRepository<QuestAssignment, Long> {

    boolean existsByQuestAndStatusIn(Quest quest, Collection<AssignmentStatus> statuses);

    boolean existsByQuestAndAssigneeUserIdAndStatusIn(Quest quest, Long assigneeId, Collection<AssignmentStatus> statuses);

    Optional<QuestAssignment> findByQuestAndAssigneeUserIdAndStatus(Quest quest, Long assigneeId, AssignmentStatus status);

    Optional<QuestAssignment> findFirstByQuestQuestIdAndStatus(Long questId, AssignmentStatus status);

    List<QuestAssignment> findByQuestAndStatus(Quest quest, AssignmentStatus status);

    /**
     * 查 Adventurer 的所有 ACTIVE 接取，用于推荐算法候选集过滤——排除已接取的 Quest。
     */
    List<QuestAssignment> findByAssigneeUserIdAndStatus(Long assigneeId, AssignmentStatus status);

    /** 指定 Quest 集合下的全部接取记录，供仓库级联删除清理使用。 */
    List<QuestAssignment> findByQuestQuestIdIn(Collection<Long> questIds);
}
