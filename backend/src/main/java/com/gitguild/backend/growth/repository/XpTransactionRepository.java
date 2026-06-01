package com.gitguild.backend.growth.repository;

import com.gitguild.backend.growth.domain.XpTransaction;
import org.springframework.data.jpa.repository.JpaRepository;

public interface XpTransactionRepository extends JpaRepository<XpTransaction, Long> {
}
