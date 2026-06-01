package com.gitguild.backend.growth.repository;

import com.gitguild.backend.growth.domain.ContributionRecord;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ContributionRecordRepository extends JpaRepository<ContributionRecord, Long> {

    /**
     * 判断某用户在某任务上是否已有贡献记录——成长激励发放的**幂等键**。
     * 已存在即代表 XP/等级/完成数已发放过，发放服务据此跳过，避免重复加 XP。
     */
    boolean existsByUserUserIdAndQuestQuestId(Long userId, Long questId);
}
