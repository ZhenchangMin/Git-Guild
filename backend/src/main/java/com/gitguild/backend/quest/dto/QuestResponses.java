package com.gitguild.backend.quest.dto;

import com.gitguild.backend.quest.domain.AdminDecision;
import com.gitguild.backend.quest.domain.Difficulty;
import com.gitguild.backend.quest.domain.QuestStatus;
import java.time.OffsetDateTime;
import java.util.List;

public final class QuestResponses {

    private QuestResponses() {
    }

    public record UserBrief(Long userId, String username) {
    }

    public record RepositoryBrief(Long repositoryId, String name, String defaultBranch, String syncStatus, String webUrl) {
    }

    public record IssueBrief(Long issueId, String externalIssueId, String title, String status, String externalUrl) {
    }

    public record CategoryBrief(Long categoryId, String name) {
    }

    public record TagBrief(Long tagId, String name, String color) {
    }

    public record AssignmentBrief(boolean assigned, UserBrief assignee, OffsetDateTime acceptedAt) {
    }

    public record SubmitQuestResponse(Long questId, QuestStatus status, OffsetDateTime updatedAt) {
    }

    public record CreateQuestResponse(
            Long questId,
            String title,
            QuestStatus status,
            Long repositoryId,
            Long issueId,
            String externalIssueId,
            String externalIssueUrl,
            String defaultBranch,
            Difficulty difficulty,
            Integer rewardXp,
            OffsetDateTime createdAt) {
    }

    public record QuestSummaryResponse(
            Long questId,
            String title,
            String descriptionPreview,
            Difficulty difficulty,
            List<String> techStack,
            Integer rewardXp,
            Integer estimatedHours,
            QuestStatus status,
            CategoryBrief category,
            List<TagBrief> tags,
            RepositoryBrief repository,
            OffsetDateTime createdAt) {
    }

    public record QuestDetailResponse(
            Long questId,
            String title,
            String description,
            String completionCriteria,
            Difficulty difficulty,
            List<String> techStack,
            Integer estimatedHours,
            Integer rewardXp,
            QuestStatus status,
            UserBrief publisher,
            RepositoryBrief repository,
            IssueBrief issue,
            CategoryBrief category,
            List<TagBrief> tags,
            AssignmentBrief assignment,
            OffsetDateTime createdAt,
            OffsetDateTime updatedAt) {
    }

    public record QuestPageResponse(
            List<QuestSummaryResponse> items,
            int page,
            int size,
            long totalItems,
            int totalPages) {
    }

    public record AssignmentResponse(
            Long assignmentId,
            Long questId,
            UserBrief assignee,
            QuestStatus questStatus,
            String assignmentStatus,
            String taskBranch,
            OffsetDateTime acceptedAt,
            String cloneUrl) {
    }

    public record MyAssignmentResponse(
            Long assignmentId,
            Long questId,
            String title,
            String completionCriteria,
            Difficulty difficulty,
            List<String> techStack,
            Integer rewardXp,
            QuestStatus questStatus,
            String assignmentStatus,
            String taskBranch,
            OffsetDateTime acceptedAt,
            RepositoryBrief repository,
            IssueBrief issue) {
    }

    /** 单条历史审核记录，供管理台「审核记录」时间线按 Quest 拉取完整历史（而非仅本次会话内的临时记录）。 */
    public record AdminReviewHistoryItem(
            Long adminReviewId,
            AdminDecision decision,
            String reason,
            UserBrief admin,
            boolean visibleToPublisher,
            OffsetDateTime reviewedAt,
            List<ChecklistItemDto> checklist) {
    }

    public record AdminReviewResponse(
            Long adminReviewId,
            Long questId,
            Long adminId,
            AdminDecision decision,
            String reason,
            QuestStatus questStatus,
            OffsetDateTime reviewedAt) {
    }

    public record AdminQuestSummaryResponse(
            Long questId,
            String title,
            String descriptionPreview,
            Difficulty difficulty,
            Integer rewardXp,
            QuestStatus status,
            UserBrief publisher,
            OffsetDateTime createdAt) {
    }

    public record AdminQuestPageResponse(
            List<AdminQuestSummaryResponse> items,
            int page,
            int size,
            long totalItems,
            int totalPages) {
    }

    /**
     * 工作台"我的待办"单条记录——携带 quest、assigment 状态、仓库、PR 等前端一次性排版所需字段。
     */
    public record MyAssignmentItem(
            Long questId,
            String questTitle,
            QuestStatus questStatus,
            String assignmentStatus,
            Difficulty difficulty,
            Integer rewardXp,
            String techStack,
            RepositoryBrief repository,
            IssueBrief issue,
            PullRequestBrief pr) {
    }

    /**
     * GET /quests/me/assignments 响应体（对应工作台"我的待办"区）。
     * {@code stats} 是当前用户维度的计数摘要，{@code items} 是各接取条目。
     */
    public record MyAssignmentsResponse(
            AssignmentStats stats,
            List<MyAssignmentItem> items) {
    }

    public record AssignmentStats(
            int inProgress,
            int inReview,
            int changesRequested,
            int completed) {
    }

    /**
     * 工作台 PR 摘要——仅携带排版必要的字段。
     */
    public record PullRequestBrief(
            Long pullRequestId,
            String externalPrId,
            String title,
            String status,
            String sourceBranch,
            String externalUrl) {
    }
}
