package com.gitguild.backend.notification.domain;

/**
 * 站内通知的业务类型，对应 {@code notifications.type}（VARCHAR(64)，以枚举名存储）。
 *
 * <p>P4-024 轻量通知只覆盖"关键状态变化"这一条主线，因此类型刻意收敛在
 * 提交-审核闭环上：成果提交触发 {@link #SUBMISSION_RECEIVED}，维护者审核结果触发
 * {@code REVIEW_*} 三态。后续若扩展邮件/超时提醒，可在此追加而不影响既有数据。
 */
public enum NotificationType {

    /** 冒险家提交了成果，提醒任务发布者/维护者前往审核。 */
    SUBMISSION_RECEIVED,

    /** 提交通过审核，提醒提交者已完成并获得奖励。 */
    REVIEW_APPROVED,

    /** 提交被要求修改，提醒提交者修订后重新提交。 */
    REVIEW_CHANGES_REQUESTED,

    /** 提交被驳回，提醒提交者本次成果未通过。 */
    REVIEW_REJECTED
}
