package com.gitguild.backend.notification.dto;

import com.gitguild.backend.notification.domain.NotificationStatus;
import com.gitguild.backend.notification.domain.NotificationType;
import java.time.OffsetDateTime;
import java.util.List;

/**
 * 通知模块对外响应 DTO 的聚合容器（与 {@code review} 模块的 {@code SubmissionResponses} 同构）。
 */
public final class NotificationResponses {

    private NotificationResponses() {
    }

    /** 单条通知视图。{@code relatedType}/{@code relatedId} 供前端点击跳转使用。 */
    public record NotificationItem(
            Long notificationId,
            NotificationType type,
            String content,
            NotificationStatus status,
            String relatedType,
            Long relatedId,
            OffsetDateTime createdAt,
            OffsetDateTime readAt) {
    }

    /** 收件箱 Feed：最近若干条 + 未读总数（红点用未读数，不受分页条数限制）。 */
    public record NotificationFeed(List<NotificationItem> items, long unreadCount) {
    }
}
