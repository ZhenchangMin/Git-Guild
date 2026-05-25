package com.gitguild.backend.quest.service;

import com.gitguild.backend.quest.dto.AdminReviewRequest;
import com.gitguild.backend.quest.dto.QuestResponses.AdminQuestPageResponse;
import com.gitguild.backend.quest.dto.QuestResponses.AdminReviewResponse;

/**
 * Admin 对 Quest 的审核操作入口，封装上架审核与下架两个核心流程。
 *
 * <p>暴露的功能：
 * <ul>
 *   <li>查询待审核 Quest 分页列表（PENDING_ADMIN_REVIEW）</li>
 *   <li>对单个 Quest 执行审核决策（APPROVE_PUBLISH / REJECT_PUBLISH / TAKE_DOWN）</li>
 * </ul>
 *
 * <p>业务不变量（由实现层强制）：
 * <ul>
 *   <li>调用方必须持有 ADMIN 角色，否则抛出 FORBIDDEN（403）。</li>
 *   <li>APPROVE_PUBLISH 和 REJECT_PUBLISH 仅对 PENDING_ADMIN_REVIEW 状态的 Quest 有效，
 *       状态不符时抛出 QUEST_NOT_REVIEWABLE（409）。</li>
 *   <li>TAKE_DOWN 对任意非 CLOSED 状态有效；已 CLOSED 时抛出 QUEST_ALREADY_CLOSED（409）。</li>
 *   <li>每次审核都会落库一条 {@link com.gitguild.backend.quest.domain.AdminReviewRecord}。</li>
 * </ul>
 */
public interface AdminQuestService {

    /**
     * 返回当前所有 PENDING_ADMIN_REVIEW 状态的 Quest，按创建时间升序分页。
     *
     * @param page 1-based 页码，传入小于 1 的值时自动修正为 1
     * @param size 每页数量，上限 50，超出时自动截断
     */
    AdminQuestPageResponse listPendingQuests(int page, int size);

    /**
     * 对指定 Quest 执行 Admin 审核决策，并生成审核凭证。
     *
     * @param questId Quest 的唯一标识
     * @param adminId 执行审核的 Admin 用户 ID（必须为 ADMIN 角色）
     * @param request 包含决策类型、审核原因及可见性配置
     * @return 包含审核记录 ID、Quest 新状态和决策详情的响应对象
     * @throws com.gitguild.backend.common.BusinessException
     *         QUEST_NOT_FOUND（404）、FORBIDDEN（403）、QUEST_NOT_REVIEWABLE（409）、QUEST_ALREADY_CLOSED（409）
     */
    AdminReviewResponse reviewQuest(Long questId, Long adminId, AdminReviewRequest request);
}
