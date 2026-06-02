package com.gitguild.backend.growth.repository;

import com.gitguild.backend.growth.domain.GrowthProfile;
import java.util.List;
import java.util.Optional;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

public interface GrowthProfileRepository extends JpaRepository<GrowthProfile, Long> {

    /**
     * 查找用户的成长档案。每人至多一条（DB UNIQUE user_id）。
     * 首次完成任务前不存在，调用方需走"查无则建"。
     */
    Optional<GrowthProfile> findByUserUserId(Long userId);

    List<GrowthProfile> findByOrderByTotalXpDescCompletedQuestCountDescUserUserIdAsc(Pageable pageable);
}
