package com.gitguild.backend.message.domain;

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
@Table(name = "message_threads")
public class MessageThread {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "thread_id")
    private Long threadId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "quest_id", nullable = false, unique = true)
    private Quest quest;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "publisher_id", nullable = false)
    private User publisher;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "assignee_id", nullable = false)
    private User assignee;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 32)
    private MessageThreadStatus status;

    @Column(name = "last_message_at", nullable = false)
    private OffsetDateTime lastMessageAt;

    @Column(name = "created_at", nullable = false)
    private OffsetDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private OffsetDateTime updatedAt;

    protected MessageThread() {
    }

    public MessageThread(Quest quest, User publisher, User assignee) {
        this.quest = quest;
        this.publisher = publisher;
        this.assignee = assignee;
        this.status = MessageThreadStatus.ACTIVE;
    }

    @PrePersist
    void prePersist() {
        OffsetDateTime now = OffsetDateTime.now();
        this.createdAt = now;
        this.updatedAt = now;
        if (this.lastMessageAt == null) {
            this.lastMessageAt = now;
        }
        if (this.status == null) {
            this.status = MessageThreadStatus.ACTIVE;
        }
    }

    @PreUpdate
    void preUpdate() {
        this.updatedAt = OffsetDateTime.now();
    }

    public boolean hasParticipant(Long userId) {
        return userId != null
                && (publisher.getUserId().equals(userId) || assignee.getUserId().equals(userId));
    }

    public User counterpartOf(Long userId) {
        return publisher.getUserId().equals(userId) ? assignee : publisher;
    }

    public void touch(OffsetDateTime lastMessageAt) {
        this.lastMessageAt = lastMessageAt;
    }

    public Long getThreadId() {
        return threadId;
    }

    public Quest getQuest() {
        return quest;
    }

    public User getPublisher() {
        return publisher;
    }

    public User getAssignee() {
        return assignee;
    }

    public MessageThreadStatus getStatus() {
        return status;
    }

    public OffsetDateTime getLastMessageAt() {
        return lastMessageAt;
    }

    public OffsetDateTime getCreatedAt() {
        return createdAt;
    }

    public OffsetDateTime getUpdatedAt() {
        return updatedAt;
    }
}
