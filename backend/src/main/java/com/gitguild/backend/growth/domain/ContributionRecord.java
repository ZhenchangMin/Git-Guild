package com.gitguild.backend.growth.domain;

import com.gitguild.backend.codehost.domain.CodeRepository;
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
 * 贡献记录（表 {@code contribution_records}，UNIQUE user_id+quest_id）。
 *
 * <p>每个 Adventurer 在每个 Quest 上至多一条——既是成长档案的明细来源，也是 XP 发放的**幂等键**：
 * {@code GrowthService} 发放前以 (user, quest) 是否已有记录判定"是否已发放过"（见 P4-022 设计文档决策 4）。
 */
@Entity
@Table(name = "contribution_records")
public class ContributionRecord {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "record_id")
    private Long recordId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "quest_id", nullable = false)
    private Quest quest;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "repository_id", nullable = false)
    private CodeRepository repository;

    @Column(nullable = false, length = 500)
    private String summary;

    @Column(name = "completed_at", nullable = false)
    private OffsetDateTime completedAt;

    @Column(name = "created_at", nullable = false)
    private OffsetDateTime createdAt;

    protected ContributionRecord() {
    }

    public ContributionRecord(User user, Quest quest, CodeRepository repository, String summary,
            OffsetDateTime completedAt) {
        this.user = user;
        this.quest = quest;
        this.repository = repository;
        this.summary = summary;
        this.completedAt = completedAt;
    }

    @PrePersist
    void prePersist() {
        this.createdAt = OffsetDateTime.now();
    }

    public Long getRecordId() {
        return recordId;
    }

    public User getUser() {
        return user;
    }

    public Quest getQuest() {
        return quest;
    }

    public CodeRepository getRepository() {
        return repository;
    }

    public String getSummary() {
        return summary;
    }

    public OffsetDateTime getCompletedAt() {
        return completedAt;
    }
}
