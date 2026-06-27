package com.gitguild.backend.growth.repository;

import com.gitguild.backend.growth.domain.XpTransaction;
import java.time.OffsetDateTime;
import java.util.Collection;
import java.util.List;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

/**
 * {@code xp_transactions} 的存取。XP 流水只增不改，P4-022 发放路径仅需基础 {@code save}；
 * 分页查询（契约 §198 {@code GET /users/me/xp-transactions}）延后到 P4-023 需要明细时再补。
 */
public interface XpTransactionRepository extends JpaRepository<XpTransaction, Long> {

    /** 指定 Quest 集合关联的全部 XP 流水，供仓库级联删除清理使用。 */
    List<XpTransaction> findByQuestQuestIdIn(Collection<Long> questIds);

    @Query("""
            select t.user.userId as userId,
                   t.user.username as username,
                   coalesce(sum(t.amount), 0) as totalXp,
                   count(distinct t.quest.questId) as completedQuestCount
            from XpTransaction t
            where t.createdAt >= :start
            group by t.user.userId, t.user.username
            order by coalesce(sum(t.amount), 0) desc,
                     count(distinct t.quest.questId) desc,
                     t.user.userId asc
            """)
    List<LeaderboardPeriodRow> findLeaderboardSince(@Param("start") OffsetDateTime start, Pageable pageable);

    interface LeaderboardPeriodRow {
        Long getUserId();

        String getUsername();

        long getTotalXp();

        long getCompletedQuestCount();
    }
}
