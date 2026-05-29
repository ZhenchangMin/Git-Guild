package com.gitguild.backend.review.domain;

import com.gitguild.backend.user.domain.User;
import jakarta.persistence.CascadeType;
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
import jakarta.persistence.OneToMany;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "review_records")
public class ReviewRecord {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "review_id")
    private Long reviewId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "submission_id", nullable = false)
    private Submission submission;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "reviewer_id", nullable = false)
    private User reviewer;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 32)
    private ReviewDecision decision;

    @Column(nullable = false, length = 500)
    private String summary;

    @Column(name = "reviewed_at", nullable = false)
    private OffsetDateTime reviewedAt;

    @Column(name = "created_at", nullable = false)
    private OffsetDateTime createdAt;

    @OneToMany(mappedBy = "reviewRecord", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<ReviewItem> items = new ArrayList<>();

    protected ReviewRecord() {
    }

    public ReviewRecord(Submission submission, User reviewer, ReviewDecision decision, String summary) {
        this.submission = submission;
        this.reviewer = reviewer;
        this.decision = decision;
        this.summary = summary;
    }

    @PrePersist
    void prePersist() {
        OffsetDateTime now = OffsetDateTime.now();
        this.reviewedAt = now;
        this.createdAt = now;
    }

    public void addItem(ReviewItem item) {
        items.add(item);
        item.attachTo(this);
    }

    public boolean requiresChanges() {
        return decision == ReviewDecision.CHANGES_REQUESTED;
    }

    public Long getReviewId() {
        return reviewId;
    }

    public Submission getSubmission() {
        return submission;
    }

    public User getReviewer() {
        return reviewer;
    }

    public ReviewDecision getDecision() {
        return decision;
    }

    public String getSummary() {
        return summary;
    }

    public OffsetDateTime getReviewedAt() {
        return reviewedAt;
    }

    public List<ReviewItem> getItems() {
        return items;
    }
}
