package com.gitguild.backend.quest.domain;

import com.gitguild.backend.user.domain.User;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
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
 * Admin 每次审核动作的不可变凭证，映射 {@code admin_review_records} 表。
 *
 * <p>隐藏的复杂度：{@code reviewedAt} 在构造时由业务层赋值（代表决策发生时刻），
 * 与 {@code createdAt}（持久化时刻）刻意分离，以便未来支持离线审批场景。
 *
 * <p>不变量：
 * <ul>
 *   <li>每条记录与一个 Quest 和一个 Admin 强关联，且创建后不可修改。</li>
 *   <li>{@code reason} 不允许为空，确保每次决策都有可追溯的说明。</li>
 *   <li>{@code visibleToPublisher} 控制审核意见是否向 Guild Master 公开，默认为 {@code true}。</li>
 * </ul>
 */
@Entity
@Table(name = "admin_review_records")
public class AdminReviewRecord {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "admin_review_id")
    private Long adminReviewId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "quest_id", nullable = false)
    private Quest quest;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "admin_id", nullable = false)
    private User admin;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 32)
    private AdminDecision decision;

    @Column(nullable = false, length = 500)
    private String reason;

    @Column(name = "visible_to_publisher", nullable = false)
    private boolean visibleToPublisher = true;

    /** APPROVE_PUBLISH 时落库的 6 项提示性检查清单（JSON 序列化的 ChecklistItemDto 列表）；其余决策可为 null。 */
    @Column(name = "checklist_json", columnDefinition = "TEXT")
    private String checklistJson;

    @Column(name = "reviewed_at", nullable = false)
    private OffsetDateTime reviewedAt;

    @Column(name = "created_at", nullable = false)
    private OffsetDateTime createdAt;

    protected AdminReviewRecord() {
    }

    public AdminReviewRecord(Quest quest, User admin, AdminDecision decision, String reason, boolean visibleToPublisher) {
        this(quest, admin, decision, reason, visibleToPublisher, null);
    }

    public AdminReviewRecord(
            Quest quest, User admin, AdminDecision decision, String reason, boolean visibleToPublisher, String checklistJson) {
        this.quest = quest;
        this.admin = admin;
        this.decision = decision;
        this.reason = reason;
        this.visibleToPublisher = visibleToPublisher;
        this.checklistJson = checklistJson;
        this.reviewedAt = OffsetDateTime.now();
    }

    @PrePersist
    void prePersist() {
        this.createdAt = OffsetDateTime.now();
        if (this.reviewedAt == null) {
            this.reviewedAt = this.createdAt;
        }
    }

    public Long getAdminReviewId() { return adminReviewId; }
    public Quest getQuest() { return quest; }
    public User getAdmin() { return admin; }
    public AdminDecision getDecision() { return decision; }
    public String getReason() { return reason; }
    public boolean isVisibleToPublisher() { return visibleToPublisher; }
    public String getChecklistJson() { return checklistJson; }
    public OffsetDateTime getReviewedAt() { return reviewedAt; }
}
