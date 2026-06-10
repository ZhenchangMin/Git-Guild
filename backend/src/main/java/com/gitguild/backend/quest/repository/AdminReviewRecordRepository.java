package com.gitguild.backend.quest.repository;

import com.gitguild.backend.quest.domain.AdminReviewRecord;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface AdminReviewRecordRepository extends JpaRepository<AdminReviewRecord, Long> {

    List<AdminReviewRecord> findByQuestQuestIdOrderByReviewedAtDesc(Long questId);
}
