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

    /**
     * 引用该分类的 Quest 数量（用于管理台分类「引用数」展示及删除前占用校验）。
     * 已下架/已驳回的 Quest 不算「占用」——它们的行还在数据库里，但对分类/标签管理而言
     * 应视为已不再生效，否则管理员永远无法清理一个只剩历史驳回记录的分类。
     */
    long countByCategory_CategoryIdAndStatusNotIn(Long categoryId, Collection<QuestStatus> excludedStatuses);

    /** 引用该标签的 Quest 数量（经 quest_tag_relations 关联表统计），同样排除已下架/已驳回。 */
    @Query("select count(q) from Quest q join q.tags t where t.tagId = :tagId and q.status not in :excludedStatuses")
    long countByTagIdAndStatusNotIn(Long tagId, Collection<QuestStatus> excludedStatuses);

    /**
     * 引用该技术栈名称的 Quest 数量。技术栈没有关联表，原样存在 tech_stack JSON 数组列里，
     * 这里用「转小写后按 JSON 字符串元素子串匹配」代替 MySQL 专属的 JSON_CONTAINS，
     * 以便在 H2(MySQL 模式) 测试库与生产 MySQL 上行为一致。同样排除已下架/已驳回。
     */
    @Query("select count(q) from Quest q where lower(q.techStackJson) like lower(concat('%\"', :name, '\"%'))"
            + " and q.status not in :excludedStatuses")
    long countByTechStackNameAndStatusNotIn(String name, Collection<QuestStatus> excludedStatuses);

    Page<Quest> findByStatus(QuestStatus status, Pageable pageable);

    /**
     * 查询处于指定状态集合的 Quest，用于推荐算法候选集构建（PUBLISHED + IN_PROGRESS）。
     */
    List<Quest> findByStatusIn(Collection<QuestStatus> statuses);

    /** 分页版：管理员控制台「全部」视图按状态集合分页（待审核/已上架/已退回/已下架，排除 DRAFT）。 */
    Page<Quest> findByStatusIn(Collection<QuestStatus> statuses, Pageable pageable);

    /** 当前维护者发布的全部 Quest（含所有状态），供「我发布的委托」视图按创建时间倒序展示。 */
    List<Quest> findByPublisherUserIdOrderByCreatedAtDesc(Long publisherId);

    /** 某仓库下的全部 Quest，供仓库级联删除按 FK 安全顺序清理使用。 */
    List<Quest> findByRepositoryRepositoryId(Long repositoryId);
}
