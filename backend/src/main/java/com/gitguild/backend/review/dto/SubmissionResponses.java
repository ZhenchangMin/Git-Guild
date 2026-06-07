package com.gitguild.backend.review.dto;

import com.gitguild.backend.codehost.domain.CodePullRequest;
import com.gitguild.backend.quest.domain.QuestStatus;
import com.gitguild.backend.review.domain.ReviewDecision;
import com.gitguild.backend.review.domain.SubmissionStatus;
import java.time.OffsetDateTime;
import java.util.List;

public final class SubmissionResponses {

    private SubmissionResponses() {
    }

    public record UserBrief(Long userId, String username) {
    }

    public record QuestBrief(Long questId, String title, QuestStatus status) {
    }

    public record PullRequestBrief(
            Long pullRequestId,
            String externalPrId,
            String title,
            String sourceBranch,
            String targetBranch,
            String status,
            String externalUrl) {

        public static PullRequestBrief from(CodePullRequest pr) {
            return new PullRequestBrief(
                    pr.getPullRequestId(),
                    pr.getExternalPrId(),
                    pr.getTitle(),
                    pr.getSourceBranch(),
                    pr.getTargetBranch(),
                    pr.getStatus(),
                    pr.getExternalUrl());
        }
    }

    public record RepositoryBrief(
            Long repositoryId,
            String name,
            String sourceUrl,
            String defaultBranch,
            String syncStatus) {
    }

    public record ReviewItemResponse(Long itemId, String checkpoint, String comment, boolean passed) {
    }

    public record ReviewRecordResponse(
            Long reviewId,
            Long submissionId,
            Long reviewerId,
            ReviewDecision decision,
            String summary,
            boolean requiresChanges,
            List<ReviewItemResponse> items,
            SubmissionStatus submissionStatus,
            QuestStatus questStatus,
            Integer rewardXp,
            OffsetDateTime reviewedAt) {
    }

    public record CreateSubmissionResponse(
            Long submissionId,
            Long questId,
            Long submitterId,
            Long pullRequestId,
            SubmissionStatus status,
            OffsetDateTime submittedAt) {
    }

    public record SubmissionDetailResponse(
            Long submissionId,
            QuestBrief quest,
            UserBrief submitter,
            PullRequestBrief pullRequest,
            String description,
            SubmissionStatus status,
            OffsetDateTime submittedAt,
            List<ReviewRecordResponse> reviewRecords) {
    }

    public record SubmissionReviewQueueItemResponse(
            Long submissionId,
            QuestBrief quest,
            UserBrief submitter,
            RepositoryBrief repository,
            PullRequestBrief pullRequest,
            String description,
            SubmissionStatus status,
            Integer rewardXp,
            String completionCriteria,
            OffsetDateTime submittedAt) {
    }
}
