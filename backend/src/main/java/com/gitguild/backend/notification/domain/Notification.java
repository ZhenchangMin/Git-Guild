package com.gitguild.backend.notification.domain;

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
 * 一条站内通知（表 {@code notifications}）。
 *
 * <p><b>语义：</b>面向单个接收者的"关键状态变化"提示。{@code relatedType}/{@code relatedId}
 * 是一个轻量引用（如 {@code SUBMISSION} + submissionId），用于前端点击通知时跳转或定位，
 * 不建外键以免与被引用资源的生命周期强耦合。
 *
 * <p><b>不变量：</b>{@code content} 在创建时即固化为人类可读文案——通知是事件发生那一刻的快照，
 * 即便关联资源后续变化，历史通知文案也不应随之改写。
 */
@Entity
@Table(name = "notifications")
public class Notification {

    /** content 列上限（与 init.sql 的 VARCHAR(1000) 对齐），写入前在服务层截断。 */
    public static final int MAX_CONTENT_LENGTH = 1000;

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "notification_id")
    private Long notificationId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "receiver_id", nullable = false)
    private User receiver;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 64)
    private NotificationType type;

    @Column(nullable = false, length = MAX_CONTENT_LENGTH)
    private String content;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 32)
    private NotificationStatus status;

    @Column(name = "related_type", length = 64)
    private String relatedType;

    @Column(name = "related_id")
    private Long relatedId;

    @Column(name = "created_at", nullable = false)
    private OffsetDateTime createdAt;

    @Column(name = "read_at")
    private OffsetDateTime readAt;

    protected Notification() {
    }

    public Notification(User receiver, NotificationType type, String content, String relatedType, Long relatedId) {
        this.receiver = receiver;
        this.type = type;
        this.content = content;
        this.status = NotificationStatus.UNREAD;
        this.relatedType = relatedType;
        this.relatedId = relatedId;
    }

    @PrePersist
    void prePersist() {
        if (this.createdAt == null) {
            this.createdAt = OffsetDateTime.now();
        }
        if (this.status == null) {
            this.status = NotificationStatus.UNREAD;
        }
    }

    /** 标记为已读：仅在当前未读时落 {@code readAt}，重复调用幂等。 */
    public void markRead() {
        if (this.status == NotificationStatus.UNREAD) {
            this.status = NotificationStatus.READ;
            this.readAt = OffsetDateTime.now();
        }
    }

    public Long getNotificationId() {
        return notificationId;
    }

    public User getReceiver() {
        return receiver;
    }

    public NotificationType getType() {
        return type;
    }

    public String getContent() {
        return content;
    }

    public NotificationStatus getStatus() {
        return status;
    }

    public String getRelatedType() {
        return relatedType;
    }

    public Long getRelatedId() {
        return relatedId;
    }

    public OffsetDateTime getCreatedAt() {
        return createdAt;
    }

    public OffsetDateTime getReadAt() {
        return readAt;
    }
}
