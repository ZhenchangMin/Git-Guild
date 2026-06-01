package com.gitguild.backend.notification.repository;

import com.gitguild.backend.notification.domain.Notification;
import com.gitguild.backend.notification.domain.NotificationStatus;
import java.util.List;
import java.util.Optional;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

/**
 * 通知数据访问。查询一律按 {@code receiverId} 作用域，天然隔离不同用户的收件箱。
 */
public interface NotificationRepository extends JpaRepository<Notification, Long> {

    /** 收件箱按时间倒序分页；{@code Pageable} 充当"最近 N 条"的限流。 */
    List<Notification> findByReceiverUserIdOrderByCreatedAtDesc(Long receiverId, Pageable pageable);

    /** 未读红点数。 */
    long countByReceiverUserIdAndStatus(Long receiverId, NotificationStatus status);

    /** 按状态取该用户的通知，用于"全部标记已读"时遍历未读项。 */
    List<Notification> findByReceiverUserIdAndStatus(Long receiverId, NotificationStatus status);

    /** 带归属校验的单条读取：只有接收者本人能命中，避免越权标记他人通知。 */
    Optional<Notification> findByNotificationIdAndReceiverUserId(Long notificationId, Long receiverId);
}
