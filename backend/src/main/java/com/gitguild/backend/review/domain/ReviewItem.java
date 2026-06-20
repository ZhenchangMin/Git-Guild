package com.gitguild.backend.review.domain;

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

@Entity
@Table(name = "review_items")
public class ReviewItem {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "item_id")
    private Long itemId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "review_id", nullable = false)
    private ReviewRecord reviewRecord;

    @Column(nullable = false, length = 128)
    private String checkpoint;

    @Column(length = 500)
    private String comment;

    @Column(nullable = false)
    private boolean passed;

    @Column(name = "created_at", nullable = false)
    private OffsetDateTime createdAt;

    protected ReviewItem() {
    }

    public ReviewItem(String checkpoint, String comment, boolean passed) {
        this.checkpoint = checkpoint;
        this.comment = comment;
        this.passed = passed;
    }

    @PrePersist
    void prePersist() {
        this.createdAt = OffsetDateTime.now();
    }

    void attachTo(ReviewRecord reviewRecord) {
        this.reviewRecord = reviewRecord;
    }

    public Long getItemId() {
        return itemId;
    }

    public String getCheckpoint() {
        return checkpoint;
    }

    public String getComment() {
        return comment;
    }

    public boolean isPassed() {
        return passed;
    }
}
