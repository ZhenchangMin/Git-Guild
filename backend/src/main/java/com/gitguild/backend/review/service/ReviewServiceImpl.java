package com.gitguild.backend.review.service;

import com.gitguild.backend.common.BusinessException;
import com.gitguild.backend.quest.domain.AssignmentStatus;
import com.gitguild.backend.quest.domain.Quest;
import com.gitguild.backend.quest.repository.QuestAssignmentRepository;
import com.gitguild.backend.quest.repository.QuestRepository;
import com.gitguild.backend.review.domain.ReviewDecision;
import com.gitguild.backend.review.domain.ReviewItem;
import com.gitguild.backend.review.domain.ReviewRecord;
import com.gitguild.backend.review.domain.Submission;
import com.gitguild.backend.review.dto.ReviewSubmissionRequest;
import com.gitguild.backend.review.dto.SubmissionResponses;
import com.gitguild.backend.review.dto.SubmissionResponses.ReviewRecordResponse;
import com.gitguild.backend.review.repository.ReviewRecordRepository;
import com.gitguild.backend.review.repository.SubmissionRepository;
import com.gitguild.backend.user.domain.User;
import com.gitguild.backend.user.domain.UserRole;
import com.gitguild.backend.user.repository.UserRepository;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class ReviewServiceImpl implements ReviewService {

    private final SubmissionRepository submissionRepository;
    private final ReviewRecordRepository reviewRecordRepository;
    private final QuestRepository questRepository;
    private final QuestAssignmentRepository assignmentRepository;
    private final UserRepository userRepository;

    public ReviewServiceImpl(
            SubmissionRepository submissionRepository,
            ReviewRecordRepository reviewRecordRepository,
            QuestRepository questRepository,
            QuestAssignmentRepository assignmentRepository,
            UserRepository userRepository) {
        this.submissionRepository = submissionRepository;
        this.reviewRecordRepository = reviewRecordRepository;
        this.questRepository = questRepository;
        this.assignmentRepository = assignmentRepository;
        this.userRepository = userRepository;
    }

    @Override
    @Transactional
    public ReviewRecordResponse reviewSubmission(Long submissionId, Long reviewerId, ReviewSubmissionRequest request) {
        Submission submission = submissionRepository.findById(submissionId)
                .orElseThrow(() -> new BusinessException("SUBMISSION_NOT_FOUND", HttpStatus.NOT_FOUND, "Submission not found", "submissionId=" + submissionId));
        User reviewer = findUser(reviewerId);
        if (!canReview(submission, reviewer)) {
            throw new BusinessException("FORBIDDEN", HttpStatus.FORBIDDEN, "Current user cannot review this submission", "Only quest publisher, repository maintainer or admin can review submission");
        }
        if (!submission.isReviewable()) {
            throw new BusinessException("SUBMISSION_ALREADY_REVIEWED", HttpStatus.CONFLICT, "Submission has already been reviewed", "submissionId=" + submissionId + ", currentStatus=" + submission.getStatus());
        }
        if (request.getDecision() == ReviewDecision.APPROVED && !submission.getPullRequest().isMerged()) {
            throw new BusinessException("PR_NOT_MERGED", HttpStatus.CONFLICT, "Pull request is not merged", "pullRequestId=" + submission.getPullRequest().getPullRequestId());
        }

        ReviewRecord record = new ReviewRecord(submission, reviewer, request.getDecision(), request.getSummary());
        request.getItems().forEach(item -> record.addItem(new ReviewItem(
                item.getCheckpoint(),
                item.getComment(),
                item.getPassed())));

        applyDecision(submission, request.getDecision());
        ReviewRecord saved = reviewRecordRepository.save(record);
        submissionRepository.save(submission);
        questRepository.save(submission.getQuest());
        return toResponse(saved);
    }

    private User findUser(Long userId) {
        return userRepository.findById(userId)
                .orElseThrow(() -> new BusinessException("USER_NOT_FOUND", HttpStatus.NOT_FOUND, "User not found", "userId=" + userId));
    }

    private boolean canReview(Submission submission, User reviewer) {
        if (reviewer.getRole() != UserRole.MAINTAINER && reviewer.getRole() != UserRole.ADMIN) {
            return false;
        }
        Long reviewerId = reviewer.getUserId();
        Quest quest = submission.getQuest();
        return reviewer.getRole() == UserRole.ADMIN
                || quest.getPublisher().getUserId().equals(reviewerId)
                || quest.getRepository().getOwner().getUserId().equals(reviewerId);
    }

    private void applyDecision(Submission submission, ReviewDecision decision) {
        if (decision == ReviewDecision.APPROVED) {
            submission.approve();
            submission.getQuest().markCompleted();
            assignmentRepository
                    .findByQuestAndAssigneeUserIdAndStatus(
                            submission.getQuest(),
                            submission.getSubmitter().getUserId(),
                            AssignmentStatus.ACTIVE)
                    .ifPresent(assignment -> {
                        assignment.complete();
                        assignmentRepository.save(assignment);
                    });
            return;
        }
        if (decision == ReviewDecision.CHANGES_REQUESTED) {
            submission.requestChanges();
            return;
        }
        submission.reject();
    }

    private ReviewRecordResponse toResponse(ReviewRecord record) {
        Submission submission = record.getSubmission();
        Quest quest = submission.getQuest();
        return new ReviewRecordResponse(
                record.getReviewId(),
                submission.getSubmissionId(),
                record.getReviewer().getUserId(),
                record.getDecision(),
                record.getSummary(),
                record.requiresChanges(),
                record.getItems().stream().map(this::toItemResponse).toList(),
                submission.getStatus(),
                quest.getStatus(),
                record.getDecision() == ReviewDecision.APPROVED ? quest.getRewardXp() : null,
                record.getReviewedAt());
    }

    private SubmissionResponses.ReviewItemResponse toItemResponse(ReviewItem item) {
        return new SubmissionResponses.ReviewItemResponse(
                item.getItemId(),
                item.getCheckpoint(),
                item.getComment(),
                item.isPassed());
    }
}
