package com.gitguild.backend.notification.service;

import com.gitguild.backend.notification.domain.NotificationType;
import com.gitguild.backend.notification.dto.NotificationResponses.NotificationFeed;
import com.gitguild.backend.user.domain.User;

/**
 * 站内通知应用服务（P4-024）。
 *
 * <p>分两类用途：
 * <ul>
 *   <li><b>写端 {@link #notify}</b>：被业务流程（提交、审核）在其事务内调用，
 *       记录一次"关键状态变化"。调用方负责保证通知失败不阻断主流程（见各调用点说明）。</li>
 *   <li><b>读端 {@link #getFeed}/{@link #markRead}/{@link #markAllRead}</b>：供前端通知中心拉取与清未读。</li>
 * </ul>
 */
public interface NotificationService {

    /**
     * 投递一条通知给指定接收者。
     *
     * @param receiver    接收者（不能为空）
     * @param type        通知类型
     * @param content     人类可读文案；超过 {@code MAX_CONTENT_LENGTH} 会被截断
     * @param relatedType 关联资源类型，可空（如 {@code "SUBMISSION"}）
     * @param relatedId   关联资源 ID，可空
     */
    void notify(User receiver, NotificationType type, String content, String relatedType, Long relatedId);

    /**
     * 拉取某用户的收件箱：最近 {@code limit} 条 + 未读总数。
     *
     * @param userId 接收者用户 ID
     * @param limit  返回的最近条数上限（&gt;0，超出会被收敛到安全上界）
     */
    NotificationFeed getFeed(Long userId, int limit);

    /** 将某条通知标记为已读；非本人或不存在则抛业务异常。返回更新后的未读数。 */
    long markRead(Long userId, Long notificationId);

    /** 将该用户全部未读标记为已读，返回本次标记的条数。 */
    int markAllRead(Long userId);
}
