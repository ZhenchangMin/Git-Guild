package com.gitguild.backend.growth.repository;

import com.gitguild.backend.growth.domain.XpTransaction;
import org.springframework.data.jpa.repository.JpaRepository;

/**
 * {@code xp_transactions} 的存取。XP 流水只增不改，P4-022 发放路径仅需基础 {@code save}；
 * 分页查询（契约 §198 {@code GET /users/me/xp-transactions}）延后到 P4-023 需要明细时再补。
 */
public interface XpTransactionRepository extends JpaRepository<XpTransaction, Long> {
}
