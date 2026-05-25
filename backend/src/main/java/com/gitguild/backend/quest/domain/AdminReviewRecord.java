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

    @Column(name = "reviewed_at", nullable = false)
    private OffsetDateTime reviewedAt;

    @Column(name = "created_at", nullable = false)
    private OffsetDateTime createdAt;

    protected AdminReviewRecord() {
    }

    public AdminReviewRecord(Quest quest, User admin, AdminDecision decision, String reason, boolean visibleToPublisher) {
        this.quest = quest;
        this.admin = admin;
        this.decision = decision;
        this.reason = reason;
        this.visibleToPublisher = visibleToPublisher;
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
    public OffsetDateTime getReviewedAt() { return reviewedAt; }
}
