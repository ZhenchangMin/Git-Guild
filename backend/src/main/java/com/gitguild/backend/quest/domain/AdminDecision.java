package com.gitguild.backend.quest.domain;

/**
 * Admin 对 Quest 的三种决策动作，对应 Quest 上架审核流程的完整操作集合。
 *
 * <ul>
 *   <li>{@link #APPROVE_PUBLISH} — 仅对 PENDING_ADMIN_REVIEW 状态的 Quest 有效，通过后 Quest 进入 PUBLISHED。</li>
 *   <li>{@link #REJECT_PUBLISH} — 仅对 PENDING_ADMIN_REVIEW 状态的 Quest 有效，驳回后 Quest 退回 REJECTED，Guild Master 需修改后重新提交。</li>
 *   <li>{@link #TAKE_DOWN} — 可对任意非 CLOSED 状态的 Quest 执行，Quest 进入 CLOSED，同时所有 ACTIVE 接取记录自动置为 CANCELLED。</li>
 * </ul>
 *
 * 不合法的决策与状态组合由 {@link com.gitguild.backend.quest.domain.Quest} 的守卫方法拦截，
 * 并以 QUEST_NOT_REVIEWABLE（409）或 QUEST_ALREADY_CLOSED（409）响应。
 */
public enum AdminDecision {
    APPROVE_PUBLISH,
    REJECT_PUBLISH,
    TAKE_DOWN
}
