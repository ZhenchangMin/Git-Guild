package com.gitguild.backend.growth.repository;

import com.gitguild.backend.growth.domain.ContributionRecord;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ContributionRecordRepository extends JpaRepository<ContributionRecord, Long> {

    /**
     * 判断某用户在某任务上是否已有贡献记录——成长激励发放的**幂等键**。
     * 已存在即代表 XP/等级/完成数已发放过，发放服务据此跳过，避免重复加 XP。
     */
    boolean existsByUserUserIdAndQuestQuestId(Long userId, Long questId);

    /**
     * 查 Adventurer 的所有贡献记录，用于推荐算法画像构建（反推技术栈偏好和难度舒适区）。
     */
    List<ContributionRecord> findByUserUserId(Long userId);

    /** 成长档案"贡献历程"：按完成时间倒序列出某用户的全部贡献记录（P4-023）。 */
    List<ContributionRecord> findByUserUserIdOrderByCompletedAtDesc(Long userId);

    Optional<ContributionRecord> findFirstByUserUserIdOrderByCompletedAtAsc(Long userId);

    /** 指定 Quest 集合关联的全部贡献记录，供仓库级联删除清理使用。 */
    List<ContributionRecord> findByQuestQuestIdIn(Collection<Long> questIds);

    /** 指定仓库关联的全部贡献记录，供仓库级联删除兜底清理使用。 */
    List<ContributionRecord> findByRepositoryRepositoryId(Long repositoryId);
}
