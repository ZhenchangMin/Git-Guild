package com.gitguild.backend.review.dto;

import com.gitguild.backend.review.domain.SubmissionStatus;
import java.util.List;

/**
 * 提交柜台开页的草稿数据（契约第 143 行 {@code GET /quests/{questId}/submission-draft}）。
 *
 * <p>{@code pullRequests} 是经 PR 同步落库后的本地候选 PR，每项带本地 {@code pullRequestId}，
 * 供 Adventurer 选定后填入 {@code POST /submissions} 的 {@code pullRequestId}。
 */
public record SubmissionDraftResponse(
        Long questId,
        RepositoryBrief repository,
        String branch,
        List<PullRequestOption> pullRequests,
        String completionCriteria,
        // 当前接取者在该任务下最近一次提交的审核状态：用于提交柜台判断是否为「退回后重新提交」，
        // 从而清除旧回执的锁定态，允许重新提交修改后的分支。无历史提交时为 null。
        SubmissionStatus latestSubmissionStatus) {

    public record RepositoryBrief(Long repositoryId, String name, String sourceUrl) {
    }

    public record PullRequestOption(
            Long pullRequestId,
            String externalPrId,
            String title,
            String status,
            String sourceBranch,
            String targetBranch,
            String externalUrl) {
    }
}
