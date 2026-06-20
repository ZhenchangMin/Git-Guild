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
    REVIEW_REJECTED,

    /** 委托通过管理员上架审核，提醒发布者（委托人）委托已上架。 */
    QUEST_APPROVED,

    /** 委托被管理员驳回上架，提醒发布者修改后重新提交。 */
    QUEST_REJECTED,

    /** 委托被管理员下架，提醒发布者委托已下线。 */
    QUEST_TAKEN_DOWN,

    /** 委托被管理员重新上架，提醒发布者委托恢复可见。 */
    QUEST_REOPENED,

    /** 委托被冒险家接取，提醒发布者有人开始做这个任务。 */
    QUEST_ACCEPTED,

    /** 提交对应的 PR 被合并，提醒提交者代码已并入目标分支。 */
    PR_MERGED,

    /** 冒险家等级提升，祝贺其升级。 */
    LEVEL_UP,

    /** 冒险家解锁新徽章/成就，祝贺其达成。 */
    BADGE_UNLOCKED
}
