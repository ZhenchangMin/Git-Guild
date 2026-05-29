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
}
