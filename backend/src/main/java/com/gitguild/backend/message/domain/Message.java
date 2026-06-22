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
import jakarta.persistence.Table;
import java.time.OffsetDateTime;

@Entity
@Table(name = "messages")
public class Message {

    public static final int MAX_CONTENT_LENGTH = 1000;

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "message_id")
    private Long messageId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "thread_id", nullable = false)
    private MessageThread thread;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "sender_id", nullable = false)
    private User sender;

    @Column(nullable = false, columnDefinition = "TEXT")
    private String content;

    @Column(name = "created_at", nullable = false)
    private OffsetDateTime createdAt;

    protected Message() {
    }

    public Message(MessageThread thread, User sender, String content) {
        this.thread = thread;
        this.sender = sender;
        this.content = content;
    }

    @PrePersist
    void prePersist() {
        this.createdAt = OffsetDateTime.now();
    }

    public Long getMessageId() {
        return messageId;
    }

    public MessageThread getThread() {
        return thread;
    }

    public User getSender() {
        return sender;
    }

    public String getContent() {
        return content;
    }

    public OffsetDateTime getCreatedAt() {
        return createdAt;
    }
}
