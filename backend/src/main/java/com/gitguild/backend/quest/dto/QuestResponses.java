package com.gitguild.backend.quest.dto;

import com.gitguild.backend.quest.domain.Difficulty;
import com.gitguild.backend.quest.domain.QuestStatus;
import java.time.OffsetDateTime;
import java.util.List;

public final class QuestResponses {

    private QuestResponses() {
    }

    public record UserBrief(Long userId, String username) {
    }

    public record RepositoryBrief(Long repositoryId, String name, String syncStatus) {
    }

    public record IssueBrief(Long issueId, String externalIssueId, String title, String status) {
    }

    public record CategoryBrief(Long categoryId, String name) {
    }

    public record TagBrief(Long tagId, String name, String color) {
    }

    public record AssignmentBrief(boolean assigned, UserBrief assignee, OffsetDateTime acceptedAt) {
    }

    public record CreateQuestResponse(
            Long questId,
            String title,
            QuestStatus status,
            Long repositoryId,
            Long issueId,
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
            OffsetDateTime acceptedAt) {
    }
}
