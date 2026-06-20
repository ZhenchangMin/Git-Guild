package com.gitguild.backend.review.service;

import com.gitguild.backend.review.dto.SubmissionDraftResponse;

/**
 * 提供提交柜台开页所需的草稿数据，并在此刻懒同步该任务仓库的 PR。
 *
 * <p>设计见《P4-018 提交草稿端点与 PR 同步空洞修复设计》：PR 同步刻意内聚在草稿接口里，
 * 使前端"开页即拿到候选 PR、选一个直接提交"为一步操作，避免新建独立仓库同步子系统。
 */
public interface SubmissionDraftService {

    /**
     * 获取指定任务的提交草稿。仅该任务的 ACTIVE Assignee 可取。
     *
     * @param questId       任务 ID
     * @param currentUserId 当前用户（从 JWT 解析）
     * @return 草稿数据，含懒同步后的本地候选 PR 列表
     */
    SubmissionDraftResponse getDraft(Long questId, Long currentUserId);
}
