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
