package com.gitguild.backend.notification.service;

import com.gitguild.backend.common.BusinessException;
import com.gitguild.backend.notification.domain.Notification;
import com.gitguild.backend.notification.domain.NotificationStatus;
import com.gitguild.backend.notification.domain.NotificationType;
import com.gitguild.backend.notification.dto.NotificationResponses.NotificationFeed;
import com.gitguild.backend.notification.dto.NotificationResponses.NotificationItem;
import com.gitguild.backend.notification.repository.NotificationRepository;
import com.gitguild.backend.user.domain.User;
import java.util.List;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

/**
 * {@link NotificationService} 的实现。
 *
 * <p><b>读端作用域不变量：</b>{@link #markRead} 通过 {@code findByNotificationIdAndReceiverUserId}
 * 同时按 ID 与接收者过滤，使"标记他人通知"在数据层即无法命中，归一化为 NOT_FOUND，避免泄露存在性。
 *
 * <p><b>写端健壮性：</b>{@link #notify} 以 {@code REQUIRES_NEW} 独立事务持久化——这样它的失败
 * 只回滚通知自身、并把异常抛回调用方，而<i>不会</i>把调用方所在的业务事务标记为 rollback-only。
 * 调用方（提交/审核服务）据此用 try/catch 真正吞掉失败，落实"通知不可用绝不阻断主流程"。
 * 反之若用默认 {@code REQUIRED} 传播，失败会污染外层事务，使 try/catch 形同虚设（提交时仍抛
 * {@code UnexpectedRollbackException}）。
 */
@Service
public class NotificationServiceImpl implements NotificationService {

    /** 收件箱单次返回的硬上界，防止 limit 被传入超大值拖垮查询。 */
    private static final int MAX_FEED_LIMIT = 50;

    private final NotificationRepository notificationRepository;

    public NotificationServiceImpl(NotificationRepository notificationRepository) {
        this.notificationRepository = notificationRepository;
    }

    @Override
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void notify(User receiver, NotificationType type, String content, String relatedType, Long relatedId) {
        if (receiver == null) {
            throw new BusinessException("NOTIFICATION_RECEIVER_REQUIRED", HttpStatus.BAD_REQUEST, "通知接收者不能为空");
        }
        notificationRepository.save(
                new Notification(receiver, type, truncate(content), relatedType, relatedId));
    }

    @Override
    @Transactional(readOnly = true)
    public NotificationFeed getFeed(Long userId, int limit) {
        int safeLimit = Math.min(Math.max(limit, 1), MAX_FEED_LIMIT);
        List<NotificationItem> items = notificationRepository
                .findByReceiverUserIdOrderByCreatedAtDesc(userId, PageRequest.of(0, safeLimit))
                .stream()
                .map(this::toItem)
                .toList();
        long unreadCount = notificationRepository.countByReceiverUserIdAndStatus(userId, NotificationStatus.UNREAD);
        return new NotificationFeed(items, unreadCount);
    }

    @Override
    @Transactional
    public long markRead(Long userId, Long notificationId) {
        Notification notification = notificationRepository
                .findByNotificationIdAndReceiverUserId(notificationId, userId)
                .orElseThrow(() -> new BusinessException(
                        "NOTIFICATION_NOT_FOUND", HttpStatus.NOT_FOUND, "通知不存在", "notificationId=" + notificationId));
        notification.markRead();
        notificationRepository.save(notification);
        return notificationRepository.countByReceiverUserIdAndStatus(userId, NotificationStatus.UNREAD);
    }

    @Override
    @Transactional
    public int markReadByRelated(Long userId, String relatedType, Long relatedId) {
        if (relatedType == null || relatedId == null) {
            return 0;
        }
        List<Notification> related = notificationRepository
                .findByReceiverUserIdAndRelatedTypeAndRelatedIdAndStatus(
                        userId, relatedType, relatedId, NotificationStatus.UNREAD);
        related.forEach(Notification::markRead);
        notificationRepository.saveAll(related);
        return related.size();
    }

    @Override
    @Transactional
    public int markAllRead(Long userId) {
        List<Notification> unread =
                notificationRepository.findByReceiverUserIdAndStatus(userId, NotificationStatus.UNREAD);
        unread.forEach(Notification::markRead);
        notificationRepository.saveAll(unread);
        return unread.size();
    }

    private NotificationItem toItem(Notification n) {
        return new NotificationItem(
                n.getNotificationId(),
                n.getType(),
                n.getContent(),
                n.getStatus(),
                n.getRelatedType(),
                n.getRelatedId(),
                n.getCreatedAt(),
                n.getReadAt());
    }

    private String truncate(String content) {
        if (content == null) {
            return "";
        }
        return content.length() <= Notification.MAX_CONTENT_LENGTH
                ? content
                : content.substring(0, Notification.MAX_CONTENT_LENGTH);
    }
}
