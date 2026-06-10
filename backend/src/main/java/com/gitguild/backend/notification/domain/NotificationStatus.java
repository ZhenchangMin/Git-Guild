package com.gitguild.backend.notification.domain;

/**
 * 站内通知的已读状态，对应 {@code notifications.status}（VARCHAR(32)，默认 {@code UNREAD}）。
 */
public enum NotificationStatus {

    /** 未读：计入红点数，列表中高亮展示。 */
    UNREAD,

    /** 已读：用户已查看，不再计入未读数。 */
    READ
}
