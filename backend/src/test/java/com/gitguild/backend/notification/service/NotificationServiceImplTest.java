package com.gitguild.backend.notification.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.gitguild.backend.common.BusinessException;
import com.gitguild.backend.notification.domain.Notification;
import com.gitguild.backend.notification.domain.NotificationStatus;
import com.gitguild.backend.notification.domain.NotificationType;
import com.gitguild.backend.notification.dto.NotificationResponses.NotificationFeed;
import com.gitguild.backend.notification.repository.NotificationRepository;
import com.gitguild.backend.user.domain.User;
import com.gitguild.backend.user.domain.UserRole;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class NotificationServiceImplTest {

    @Mock
    private NotificationRepository notificationRepository;
    @Captor
    private ArgumentCaptor<Notification> notificationCaptor;

    private NotificationServiceImpl notificationService;

    @BeforeEach
    void setUp() {
        notificationService = new NotificationServiceImpl(notificationRepository);
    }

    @Test
    void notifyShouldPersistUnreadNotificationWithGivenFields() {
        User receiver = user(3001L);

        notificationService.notify(
                receiver, NotificationType.SUBMISSION_RECEIVED, "新成果待审核", "SUBMISSION", 9001L);

        verify(notificationRepository).save(notificationCaptor.capture());
        Notification saved = notificationCaptor.getValue();
        assertThat(saved.getReceiver()).isSameAs(receiver);
        assertThat(saved.getType()).isEqualTo(NotificationType.SUBMISSION_RECEIVED);
        assertThat(saved.getContent()).isEqualTo("新成果待审核");
        assertThat(saved.getStatus()).isEqualTo(NotificationStatus.UNREAD);
        assertThat(saved.getRelatedType()).isEqualTo("SUBMISSION");
        assertThat(saved.getRelatedId()).isEqualTo(9001L);
    }

    @Test
    void notifyShouldRejectNullReceiver() {
        assertThatThrownBy(() -> notificationService.notify(
                null, NotificationType.REVIEW_APPROVED, "x", "SUBMISSION", 1L))
                .isInstanceOf(BusinessException.class)
                .extracting("code")
                .isEqualTo("NOTIFICATION_RECEIVER_REQUIRED");
        verify(notificationRepository, never()).save(any());
    }

    @Test
    void notifyShouldTruncateOverlongContent() {
        String overlong = "x".repeat(Notification.MAX_CONTENT_LENGTH + 50);

        notificationService.notify(user(3001L), NotificationType.REVIEW_REJECTED, overlong, null, null);

        verify(notificationRepository).save(notificationCaptor.capture());
        assertThat(notificationCaptor.getValue().getContent()).hasSize(Notification.MAX_CONTENT_LENGTH);
    }

    @Test
    void getFeedShouldReturnMappedItemsAndUnreadCountAndClampLimit() {
        User receiver = user(3001L);
        Notification n = new Notification(receiver, NotificationType.REVIEW_APPROVED, "已通过", "SUBMISSION", 9001L);
        when(notificationRepository.findByReceiverUserIdOrderByCreatedAtDesc(eq(3001L), any()))
                .thenReturn(List.of(n));
        when(notificationRepository.countByReceiverUserIdAndStatus(3001L, NotificationStatus.UNREAD))
                .thenReturn(3L);

        // limit 越界（0）应被收敛到合法下界，不抛异常
        NotificationFeed feed = notificationService.getFeed(3001L, 0);

        assertThat(feed.unreadCount()).isEqualTo(3L);
        assertThat(feed.items()).singleElement()
                .satisfies(item -> {
                    assertThat(item.type()).isEqualTo(NotificationType.REVIEW_APPROVED);
                    assertThat(item.content()).isEqualTo("已通过");
                    assertThat(item.relatedId()).isEqualTo(9001L);
                });
    }

    @Test
    void markReadShouldMarkOwnedNotificationAndReturnUnreadCount() {
        User receiver = user(3001L);
        Notification n = new Notification(receiver, NotificationType.REVIEW_APPROVED, "已通过", "SUBMISSION", 9001L);
        when(notificationRepository.findByNotificationIdAndReceiverUserId(7001L, 3001L))
                .thenReturn(Optional.of(n));
        when(notificationRepository.countByReceiverUserIdAndStatus(3001L, NotificationStatus.UNREAD))
                .thenReturn(2L);

        long unread = notificationService.markRead(3001L, 7001L);

        assertThat(unread).isEqualTo(2L);
        assertThat(n.getStatus()).isEqualTo(NotificationStatus.READ);
        verify(notificationRepository).save(n);
    }

    @Test
    void markReadShouldRejectWhenNotOwnedOrMissing() {
        when(notificationRepository.findByNotificationIdAndReceiverUserId(7001L, 3001L))
                .thenReturn(Optional.empty());

        assertThatThrownBy(() -> notificationService.markRead(3001L, 7001L))
                .isInstanceOf(BusinessException.class)
                .extracting("code")
                .isEqualTo("NOTIFICATION_NOT_FOUND");
    }

    @Test
    void markAllReadShouldMarkEveryUnreadAndReturnCount() {
        User receiver = user(3001L);
        Notification a = new Notification(receiver, NotificationType.REVIEW_APPROVED, "a", "SUBMISSION", 1L);
        Notification b = new Notification(receiver, NotificationType.REVIEW_REJECTED, "b", "SUBMISSION", 2L);
        when(notificationRepository.findByReceiverUserIdAndStatus(3001L, NotificationStatus.UNREAD))
                .thenReturn(List.of(a, b));

        int marked = notificationService.markAllRead(3001L);

        assertThat(marked).isEqualTo(2);
        assertThat(a.getStatus()).isEqualTo(NotificationStatus.READ);
        assertThat(b.getStatus()).isEqualTo(NotificationStatus.READ);
        verify(notificationRepository).saveAll(List.of(a, b));
    }

    private User user(Long id) {
        User user = new User("user" + id, "user" + id + "@example.com", "password-hash", UserRole.BEGINNER);
        user.setUserId(id);
        return user;
    }
}
