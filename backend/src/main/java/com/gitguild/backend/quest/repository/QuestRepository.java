package com.gitguild.backend.quest.repository;

import com.gitguild.backend.codehost.domain.CodeIssue;
import com.gitguild.backend.quest.domain.Quest;
import com.gitguild.backend.quest.domain.QuestStatus;
import java.util.Collection;
import java.util.List;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

public interface QuestRepository extends JpaRepository<Quest, Long>, JpaSpecificationExecutor<Quest> {

    boolean existsByIssueAndStatusIn(CodeIssue issue, Collection<QuestStatus> statuses);

    Page<Quest> findByStatus(QuestStatus status, Pageable pageable);

    /**
     * 查询处于指定状态集合的 Quest，用于推荐算法候选集构建（PUBLISHED + IN_PROGRESS）。
     */
    List<Quest> findByStatusIn(Collection<QuestStatus> statuses);
}
