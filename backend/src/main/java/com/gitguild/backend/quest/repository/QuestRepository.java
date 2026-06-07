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
import org.springframework.data.jpa.repository.Query;

public interface QuestRepository extends JpaRepository<Quest, Long>, JpaSpecificationExecutor<Quest> {

    boolean existsByIssueAndStatusIn(CodeIssue issue, Collection<QuestStatus> statuses);

    /** 引用该分类的 Quest 数量（用于管理台分类「引用数」展示）。 */
    long countByCategory_CategoryId(Long categoryId);

    /** 引用该标签的 Quest 数量（经 quest_tag_relations 关联表统计）。 */
    @Query("select count(q) from Quest q join q.tags t where t.tagId = :tagId")
    long countByTagId(Long tagId);

    Page<Quest> findByStatus(QuestStatus status, Pageable pageable);

    /**
     * 查询处于指定状态集合的 Quest，用于推荐算法候选集构建（PUBLISHED + IN_PROGRESS）。
     */
    List<Quest> findByStatusIn(Collection<QuestStatus> statuses);

    /** 当前维护者发布的全部 Quest（含所有状态），供「我发布的委托」视图按创建时间倒序展示。 */
    List<Quest> findByPublisherUserIdOrderByCreatedAtDesc(Long publisherId);
}
