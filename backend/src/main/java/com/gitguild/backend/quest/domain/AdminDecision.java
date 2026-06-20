package com.gitguild.backend.quest.domain;

/**
 * Admin 对 Quest 的四种决策动作，对应 Quest 上架审核流程的完整操作集合。
 *
 * <ul>
 *   <li>{@link #APPROVE_PUBLISH} — 仅对 PENDING_ADMIN_REVIEW 状态的 Quest 有效，通过后 Quest 进入 PUBLISHED；
 *       必须随请求提交全部 6 项提示性检查清单（仅做结构校验，不要求逐项「通过」），否则以 CHECKLIST_INCOMPLETE（422）拒绝。</li>
 *   <li>{@link #REJECT_PUBLISH} — 仅对 PENDING_ADMIN_REVIEW 状态的 Quest 有效，驳回后 Quest 退回 REJECTED，Guild Master 需修改后重新提交。</li>
 *   <li>{@link #TAKE_DOWN} — 可对任意非 CLOSED 状态的 Quest 执行，Quest 进入 CLOSED，同时所有 ACTIVE 接取记录自动置为 CANCELLED。</li>
 *   <li>{@link #REOPEN} — 仅对 CLOSED 状态的 Quest 有效，重新进入 PUBLISHED；被取消的接取记录不会恢复。</li>
 * </ul>
 *
 * 不合法的决策与状态组合由 {@link com.gitguild.backend.quest.domain.Quest} 的守卫方法拦截，
 * 并以 QUEST_NOT_REVIEWABLE（409）、QUEST_ALREADY_CLOSED（409）或 QUEST_NOT_REOPENABLE（409）响应。
 */
public enum AdminDecision {
    APPROVE_PUBLISH,
    REJECT_PUBLISH,
    TAKE_DOWN,
    REOPEN
}
