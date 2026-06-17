package com.gitguild.backend.quest.repository;

import com.gitguild.backend.quest.domain.AdminReviewRecord;
import java.util.Collection;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface AdminReviewRecordRepository extends JpaRepository<AdminReviewRecord, Long> {

    List<AdminReviewRecord> findByQuestQuestIdOrderByReviewedAtDesc(Long questId);

    /** 指定 Quest 集合下的全部 Admin 审核记录，供仓库级联删除清理使用。 */
    List<AdminReviewRecord> findByQuestQuestIdIn(Collection<Long> questIds);
}
