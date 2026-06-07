package com.gitguild.backend.review.service;

import com.gitguild.backend.codehost.domain.CodePullRequest;
import com.gitguild.backend.common.BusinessException;
import com.gitguild.backend.growth.service.GrowthService;
import com.gitguild.backend.notification.domain.NotificationType;
import com.gitguild.backend.notification.service.NotificationService;
import com.gitguild.backend.quest.domain.AssignmentStatus;
import com.gitguild.backend.quest.domain.Quest;
import com.gitguild.backend.quest.repository.QuestAssignmentRepository;
import com.gitguild.backend.quest.repository.QuestRepository;
import com.gitguild.backend.quest.service.QuestPullRequestService;
import com.gitguild.backend.review.domain.ReviewDecision;
import com.gitguild.backend.review.domain.ReviewItem;
import com.gitguild.backend.review.domain.ReviewRecord;
import com.gitguild.backend.review.domain.Submission;
import com.gitguild.backend.review.dto.ReviewSubmissionRequest;
import com.gitguild.backend.review.dto.SubmissionResponses;
import com.gitguild.backend.review.dto.SubmissionResponses.PullRequestBrief;
import com.gitguild.backend.review.dto.SubmissionResponses.ReviewRecordResponse;
import com.gitguild.backend.review.repository.ReviewRecordRepository;
import com.gitguild.backend.review.repository.SubmissionRepository;
import com.gitguild.backend.user.domain.User;
import com.gitguild.backend.user.domain.UserRole;
import com.gitguild.backend.user.repository.UserRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class ReviewServiceImpl implements ReviewService {

    private static final Logger log = LoggerFactory.getLogger(ReviewServiceImpl.class);

    private final SubmissionRepository submissionRepository;
    private final ReviewRecordRepository reviewRecordRepository;
    private final QuestRepository questRepository;
    private final QuestAssignmentRepository assignmentRepository;
    private final UserRepository userRepository;
    private final QuestPullRequestService questPullRequestService;
    private final GrowthService growthService;
    private final NotificationService notificationService;

    public ReviewServiceImpl(
            SubmissionRepository submissionRepository,
            ReviewRecordRepository reviewRecordRepository,
            QuestRepository questRepository,
            QuestAssignmentRepository assignmentRepository,
            UserRepository userRepository,
            QuestPullRequestService questPullRequestService,
            GrowthService growthService,
            NotificationService notificationService) {
        this.submissionRepository = submissionRepository;
        this.reviewRecordRepository = reviewRecordRepository;
        this.questRepository = questRepository;
        this.assignmentRepository = assignmentRepository;
        this.userRepository = userRepository;
        this.questPullRequestService = questPullRequestService;
        this.growthService = growthService;
        this.notificationService = notificationService;
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
        // 「接受提交」只表示认可成果（APPROVED + 任务完成 + 发 XP），不再自动合并 PR。
        // 是否合并由维护者另行通过 mergeSubmissionPullRequest 决定，二者解耦。
        ReviewRecord record = new ReviewRecord(submission, reviewer, request.getDecision(), request.getSummary());
        request.getItems().forEach(item -> record.addItem(new ReviewItem(
                item.getCheckpoint(),
                item.getComment(),
                item.getPassed())));

        applyDecision(submission, request.getDecision());
        ReviewRecord saved = reviewRecordRepository.save(record);
        submissionRepository.save(submission);
        questRepository.save(submission.getQuest());
        notifyReviewOutcome(submission, request.getDecision(), request.getSummary());
        return toResponse(saved);
    }

    @Override
    @Transactional
    public PullRequestBrief mergeSubmissionPullRequest(Long submissionId, Long reviewerId) {
        Submission submission = submissionRepository.findById(submissionId)
                .orElseThrow(() -> new BusinessException("SUBMISSION_NOT_FOUND", HttpStatus.NOT_FOUND, "Submission not found", "submissionId=" + submissionId));
        User reviewer = findUser(reviewerId);
        if (!canReview(submission, reviewer)) {
            throw new BusinessException("FORBIDDEN", HttpStatus.FORBIDDEN, "Current user cannot merge this pull request", "Only quest publisher, repository maintainer or admin can merge");
        }
        CodePullRequest pullRequest = submission.getPullRequest();
        if (pullRequest == null) {
            throw new BusinessException("PR_NOT_FOUND", HttpStatus.BAD_REQUEST, "该提交没有关联的 PR，无法合并", "submissionId=" + submissionId);
        }
        // 复用代理合并：已合并幂等返回，冲突抛 PR_MERGE_CONFLICT(409)。与审核结论/XP 互不影响。
        CodePullRequest merged = questPullRequestService.mergeForApproval(
                pullRequest, submission.getQuest().getRepository());
        return PullRequestBrief.from(merged);
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
            // P4-022 成长激励：审核通过同事务内发放 XP/等级/贡献记录（幂等，详见 GrowthService）
            growthService.grantQuestCompletion(submission.getSubmitter(), submission.getQuest());
            return;
        }
        if (decision == ReviewDecision.CHANGES_REQUESTED) {
            submission.requestChanges();
            return;
        }
        submission.reject();
    }

    /**
     * 把审核结果以站内通知推送给提交者（P4-024）。
     *
     * <p>best-effort：通知失败仅记日志，绝不回滚审核结果（审核结论与成长发放才是核心，
     * 由外层事务保证；通知走 {@code REQUIRES_NEW} 独立事务，见 {@code NotificationServiceImpl}）。
     */
    private void notifyReviewOutcome(Submission submission, ReviewDecision decision, String summary) {
        Quest quest = submission.getQuest();
        User submitter = submission.getSubmitter();
        NotificationType type;
        String content;
        switch (decision) {
            case APPROVED -> {
                type = NotificationType.REVIEW_APPROVED;
                content = String.format(
                        "你的任务《%s》已通过审核，获得 %d XP，等级与贡献记录已更新。",
                        quest.getTitle(), quest.getRewardXp());
            }
            case CHANGES_REQUESTED -> {
                type = NotificationType.REVIEW_CHANGES_REQUESTED;
                content = String.format(
                        "任务《%s》的成果需要修改：%s。请修订后重新提交。",
                        quest.getTitle(), summaryOrDefault(summary));
            }
            default -> {
                type = NotificationType.REVIEW_REJECTED;
                content = String.format(
                        "任务《%s》的成果未通过审核：%s。",
                        quest.getTitle(), summaryOrDefault(summary));
            }
        }
        try {
            notificationService.notify(submitter, type, content, "SUBMISSION", submission.getSubmissionId());
        } catch (RuntimeException ex) {
            log.warn("发送审核结果通知失败 submissionId={}, receiverId={}",
                    submission.getSubmissionId(), submitter.getUserId(), ex);
        }
    }

    private String summaryOrDefault(String summary) {
        return (summary == null || summary.isBlank()) ? "请查看审核意见" : summary;
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
