package com.gitguild.backend.review.domain;

import com.gitguild.backend.codehost.domain.CodePullRequest;
import com.gitguild.backend.quest.domain.Quest;
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
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import java.time.OffsetDateTime;

@Entity
@Table(name = "submissions")
public class Submission {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "submission_id")
    private Long submissionId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "quest_id", nullable = false)
    private Quest quest;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "submitter_id", nullable = false)
    private User submitter;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "pull_request_id", nullable = false)
    private CodePullRequest pullRequest;

    @Column(nullable = false, columnDefinition = "TEXT")
    private String description;

    /**
     * 冒险家提交的佐证材料，存为 JSON 数组：[{name, mimeType, content(base64 data URL)}]。
     * 用 LONGTEXT 承载图片/文档的内联 base64，供委托人在审核台查看其上传的文件。
     */
    @Column(name = "evidence", columnDefinition = "LONGTEXT")
    private String evidence;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 32)
    private SubmissionStatus status;

    @Column(name = "submitted_at", nullable = false)
    private OffsetDateTime submittedAt;

    @Column(name = "created_at", nullable = false)
    private OffsetDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private OffsetDateTime updatedAt;

    protected Submission() {
    }

    public Submission(Quest quest, User submitter, CodePullRequest pullRequest, String description) {
        this.quest = quest;
        this.submitter = submitter;
        this.pullRequest = pullRequest;
        this.description = description;
        this.status = SubmissionStatus.PENDING_REVIEW;
    }

    @PrePersist
    void prePersist() {
        OffsetDateTime now = OffsetDateTime.now();
        this.submittedAt = now;
        this.createdAt = now;
        this.updatedAt = now;
    }

    @PreUpdate
    void preUpdate() {
        this.updatedAt = OffsetDateTime.now();
    }

    public boolean isReviewable() {
        return status == SubmissionStatus.PENDING_REVIEW;
    }

    public void approve() {
        this.status = SubmissionStatus.APPROVED;
    }

    public void requestChanges() {
        this.status = SubmissionStatus.CHANGES_REQUESTED;
    }

    public void reject() {
        this.status = SubmissionStatus.REJECTED;
    }

    public Long getSubmissionId() {
        return submissionId;
    }

    public Quest getQuest() {
        return quest;
    }

    public User getSubmitter() {
        return submitter;
    }

    public CodePullRequest getPullRequest() {
        return pullRequest;
    }

    public String getDescription() {
        return description;
    }

    public String getEvidence() {
        return evidence;
    }

    public void setEvidence(String evidence) {
        this.evidence = evidence;
    }

    public SubmissionStatus getStatus() {
        return status;
    }

    public OffsetDateTime getSubmittedAt() {
        return submittedAt;
    }

    public void setSubmissionId(Long submissionId) {
        this.submissionId = submissionId;
    }
}
