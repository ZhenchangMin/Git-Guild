package com.gitguild.backend.growth.domain;

import com.gitguild.backend.quest.domain.Quest;
import com.gitguild.backend.user.domain.User;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import java.time.OffsetDateTime;

/**
 * XP 流水（表 {@code xp_transactions}，只增不改）。每发放一次 XP 追加一行，作为成长档案聚合值的审计来源。
 *
 * <p>{@code quest} 可空——预留非任务来源（如徽章、活动奖励）的 XP；P4-022 仅产生 {@code reason="QUEST_COMPLETED"}。
 */
@Entity
@Table(name = "xp_transactions")
public class XpTransaction {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "transaction_id")
    private Long transactionId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "quest_id")
    private Quest quest;

    @Column(nullable = false)
    private int amount;

    @Column(nullable = false, length = 128)
    private String reason;

    @Column(name = "created_at", nullable = false)
    private OffsetDateTime createdAt;

    protected XpTransaction() {
    }

    public XpTransaction(User user, Quest quest, int amount, String reason) {
        this.user = user;
        this.quest = quest;
        this.amount = amount;
        this.reason = reason;
    }

    @PrePersist
    void prePersist() {
        this.createdAt = OffsetDateTime.now();
    }

    public Long getTransactionId() {
        return transactionId;
    }

    public User getUser() {
        return user;
    }

    public Quest getQuest() {
        return quest;
    }

    public int getAmount() {
        return amount;
    }

    public String getReason() {
        return reason;
    }
}
