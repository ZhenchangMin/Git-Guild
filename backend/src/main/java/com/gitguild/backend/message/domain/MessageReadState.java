package com.gitguild.backend.message.domain;

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
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import java.time.OffsetDateTime;

@Entity
@Table(name = "message_read_states")
public class MessageReadState {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "read_state_id")
    private Long readStateId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "thread_id", nullable = false)
    private MessageThread thread;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Column(name = "last_read_at", nullable = false)
    private OffsetDateTime lastReadAt;

    @Column(name = "created_at", nullable = false)
    private OffsetDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private OffsetDateTime updatedAt;

    protected MessageReadState() {
    }

    public MessageReadState(MessageThread thread, User user, OffsetDateTime lastReadAt) {
        this.thread = thread;
        this.user = user;
        this.lastReadAt = lastReadAt;
    }

    @PrePersist
    void prePersist() {
        OffsetDateTime now = OffsetDateTime.now();
        this.createdAt = now;
        this.updatedAt = now;
        if (this.lastReadAt == null) {
            this.lastReadAt = now;
        }
    }

    @PreUpdate
    void preUpdate() {
        this.updatedAt = OffsetDateTime.now();
    }

    public void markRead(OffsetDateTime lastReadAt) {
        this.lastReadAt = lastReadAt;
    }

    public OffsetDateTime getLastReadAt() {
        return lastReadAt;
    }
}
