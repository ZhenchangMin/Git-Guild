package com.gitguild.backend.review.service;

import com.gitguild.backend.codehost.domain.CodePullRequest;
import com.gitguild.backend.codehost.domain.CodeRepository;
import com.gitguild.backend.codehost.repository.CodePullRequestRepository;
import com.gitguild.backend.codehost.service.CodePullRequestSyncService;
import com.gitguild.backend.common.BusinessException;
import com.gitguild.backend.notification.domain.NotificationType;
import com.gitguild.backend.notification.service.NotificationService;
import com.gitguild.backend.quest.domain.AssignmentStatus;
import com.gitguild.backend.quest.domain.Quest;
import com.gitguild.backend.quest.domain.QuestAssignment;
import com.gitguild.backend.quest.domain.QuestStatus;
import com.gitguild.backend.quest.repository.QuestAssignmentRepository;
import com.gitguild.backend.quest.repository.QuestRepository;
import com.gitguild.backend.review.domain.ReviewItem;
import com.gitguild.backend.review.domain.ReviewRecord;
import com.gitguild.backend.review.domain.Submission;
import com.gitguild.backend.review.domain.SubmissionStatus;
import com.gitguild.backend.review.dto.CreateSubmissionRequest;
import com.gitguild.backend.review.dto.SubmissionResponses;
import com.gitguild.backend.review.dto.SubmissionResponses.CreateSubmissionResponse;
import com.gitguild.backend.review.dto.SubmissionResponses.SubmissionReviewQueueItemResponse;
import com.gitguild.backend.review.dto.SubmissionResponses.SubmissionDetailResponse;
import com.gitguild.backend.review.repository.ReviewRecordRepository;
import com.gitguild.backend.review.repository.SubmissionRepository;
import com.gitguild.backend.user.domain.User;
import com.gitguild.backend.user.domain.UserRole;
import com.gitguild.backend.user.repository.UserRepository;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class SubmissionServiceImpl implements SubmissionService {

    private static final Logger log = LoggerFactory.getLogger(SubmissionServiceImpl.class);

    private static final List<SubmissionStatus> OPEN_SUBMISSION_STATUSES = List.of(SubmissionStatus.PENDING_REVIEW);

    private final SubmissionRepository submissionRepository;
    private final ReviewRecordRepository reviewRecordRepository;
    private final QuestRepository questRepository;
    private final QuestAssignmentRepository assignmentRepository;
    private final CodePullRequestRepository pullRequestRepository;
    private final CodePullRequestSyncService pullRequestSyncService;
    private final UserRepository userRepository;
    private final NotificationService notificationService;

    public SubmissionServiceImpl(
            SubmissionRepository submissionRepository,
            ReviewRecordRepository reviewRecordRepository,
            QuestRepository questRepository,
            QuestAssignmentRepository assignmentRepository,
            CodePullRequestRepository pullRequestRepository,
            CodePullRequestSyncService pullRequestSyncService,
            UserRepository userRepository,
            NotificationService notificationService) {
        this.submissionRepository = submissionRepository;
        this.reviewRecordRepository = reviewRecordRepository;
        this.questRepository = questRepository;
        this.assignmentRepository = assignmentRepository;
        this.pullRequestRepository = pullRequestRepository;
        this.pullRequestSyncService = pullRequestSyncService;
        this.userRepository = userRepository;
        this.notificationService = notificationService;
    }

    @Override
    @Transactional
    public CreateSubmissionResponse createSubmission(Long submitterId, CreateSubmissionRequest request) {
        User submitter = findUser(submitterId);
        if (submitter.getRole() == UserRole.ADMIN) {
            throw new BusinessException("FORBIDDEN", HttpStatus.FORBIDDEN, "Current user cannot submit quest result", "Admins do not submit normal quest results");
        }

        Quest quest = questRepository.findById(request.getQuestId())
                .orElseThrow(() -> new BusinessException("QUEST_NOT_FOUND", HttpStatus.NOT_FOUND, "Quest not found", "questId=" + request.getQuestId()));
        if (quest.getStatus() != QuestStatus.IN_PROGRESS && quest.getStatus() != QuestStatus.IN_REVIEW) {
            throw new BusinessException("QUEST_NOT_ACCEPTABLE", HttpStatus.CONFLICT, "Quest is not acceptable for submission", "currentStatus=" + quest.getStatus());
        }

        assignmentRepository
                .findByQuestAndAssigneeUserIdAndStatus(quest, submitterId, AssignmentStatus.ACTIVE)
                .orElseThrow(() -> new BusinessException("FORBIDDEN", HttpStatus.FORBIDDEN, "Current user cannot submit this quest", "Only an active assignee can submit result"));
        if (submissionRepository.existsByQuestAndSubmitterUserIdAndStatusIn(quest, submitterId, OPEN_SUBMISSION_STATUSES)) {
            throw new BusinessException("SUBMISSION_NOT_REVIEWABLE", HttpStatus.CONFLICT, "Submission is not reviewable", "There is already a pending submission for this quest");
        }

        CodePullRequest pullRequest = pullRequestRepository.findById(request.getPullRequestId())
                .orElseThrow(() -> new BusinessException("PR_NOT_FOUND", HttpStatus.NOT_FOUND, "Pull request not found", "pullRequestId=" + request.getPullRequestId()));
        if (!pullRequest.getRepository().getRepositoryId().equals(quest.getRepository().getRepositoryId())) {
            throw new BusinessException("PR_NOT_FOUND", HttpStatus.NOT_FOUND, "Pull request not found", "pullRequestId does not belong to quest repository");
        }

        Submission saved = submissionRepository.save(new Submission(quest, submitter, pullRequest, request.getDescription()));
        quest.markInReview();
        questRepository.save(quest);

        notifySubmissionReceived(quest, submitter, saved.getSubmissionId());

        return new CreateSubmissionResponse(
                saved.getSubmissionId(),
                quest.getQuestId(),
                submitter.getUserId(),
                pullRequest.getPullRequestId(),
                saved.getStatus(),
                saved.getSubmittedAt());
    }

    @Override
    @Transactional(readOnly = true)
    public SubmissionDetailResponse getSubmission(Long submissionId, Long currentUserId) {
        Submission submission = submissionRepository.findById(submissionId)
                .orElseThrow(() -> new BusinessException("SUBMISSION_NOT_FOUND", HttpStatus.NOT_FOUND, "Submission not found", "submissionId=" + submissionId));
        User currentUser = findUser(currentUserId);
        if (!canView(submission, currentUser)) {
            throw new BusinessException("FORBIDDEN", HttpStatus.FORBIDDEN, "Current user cannot view this submission", "Only submitter, quest publisher or admin can view it");
        }
        List<ReviewRecord> records = reviewRecordRepository.findBySubmissionSubmissionIdOrderByReviewedAtDesc(submissionId);
        return toDetail(submission, records);
    }

    @Override
    @Transactional
    public List<SubmissionReviewQueueItemResponse> listReviewQueue(Long currentUserId) {
        User currentUser = findUser(currentUserId);
        if (currentUser.getRole() != UserRole.MAINTAINER && currentUser.getRole() != UserRole.ADMIN) {
            throw new BusinessException("FORBIDDEN", HttpStatus.FORBIDDEN, "Current user cannot review submissions", "Only maintainer or admin can access review queue");
        }
        boolean admin = currentUser.getRole() == UserRole.ADMIN;
        List<Submission> submissions = submissionRepository.findReviewQueueForReviewer(currentUserId, admin);
        syncReviewQueuePullRequests(submissions);
        return submissions.stream().map(this::toReviewQueueItem).toList();
    }

    private void syncReviewQueuePullRequests(List<Submission> submissions) {
        submissions.stream()
                .map(submission -> submission.getQuest().getRepository())
                .filter(repository -> repository != null && repository.getSourceUrl() != null)
                .distinct()
                .forEach(pullRequestSyncService::syncRepositoryPullRequests);
    }

    private User findUser(Long userId) {
        return userRepository.findById(userId)
                .orElseThrow(() -> new BusinessException("USER_NOT_FOUND", HttpStatus.NOT_FOUND, "User not found", "userId=" + userId));
    }

    /**
     * 提醒任务发布者有新成果待审核（P4-024）。
     *
     * <p>best-effort：通知失败仅记日志，绝不回滚成果提交——投递通知不应成为提交成功的前置条件。
     * 发布者与提交者为同一人时跳过，避免自我提醒。
     */
    private void notifySubmissionReceived(Quest quest, User submitter, Long submissionId) {
        User publisher = quest.getPublisher();
        if (publisher == null || publisher.getUserId().equals(submitter.getUserId())) {
            return;
        }
        try {
            String content = String.format(
                    "冒险家 %s 提交了任务《%s》的成果，等待你的审核。",
                    submitter.getUsername(), quest.getTitle());
            notificationService.notify(
                    publisher, NotificationType.SUBMISSION_RECEIVED, content, "SUBMISSION", submissionId);
        } catch (RuntimeException ex) {
            log.warn("发送成果待审核通知失败 submissionId={}, receiverId={}", submissionId, publisher.getUserId(), ex);
        }
    }

    private boolean canView(Submission submission, User currentUser) {
        Long currentUserId = currentUser.getUserId();
        return currentUser.getRole() == UserRole.ADMIN
                || submission.getSubmitter().getUserId().equals(currentUserId)
                || submission.getQuest().getPublisher().getUserId().equals(currentUserId)
                || submission.getQuest().getRepository().getOwner().getUserId().equals(currentUserId);
    }

    private SubmissionDetailResponse toDetail(Submission submission, List<ReviewRecord> records) {
        Quest quest = submission.getQuest();
        CodePullRequest pullRequest = submission.getPullRequest();
        return new SubmissionDetailResponse(
                submission.getSubmissionId(),
                new SubmissionResponses.QuestBrief(quest.getQuestId(), quest.getTitle(), quest.getStatus()),
                new SubmissionResponses.UserBrief(submission.getSubmitter().getUserId(), submission.getSubmitter().getUsername()),
                new SubmissionResponses.PullRequestBrief(
                        pullRequest.getPullRequestId(),
                        pullRequest.getExternalPrId(),
                        pullRequest.getTitle(),
                        pullRequest.getSourceBranch(),
                        pullRequest.getTargetBranch(),
                        pullRequest.getStatus(),
                        pullRequest.getExternalUrl()),
                submission.getDescription(),
                submission.getStatus(),
                submission.getSubmittedAt(),
                records.stream().map(this::toReviewResponse).toList());
    }

    private SubmissionReviewQueueItemResponse toReviewQueueItem(Submission submission) {
        Quest quest = submission.getQuest();
        CodeRepository repository = quest.getRepository();
        CodePullRequest pullRequest = submission.getPullRequest();
        return new SubmissionReviewQueueItemResponse(
                submission.getSubmissionId(),
                new SubmissionResponses.QuestBrief(quest.getQuestId(), quest.getTitle(), quest.getStatus()),
                new SubmissionResponses.UserBrief(submission.getSubmitter().getUserId(), submission.getSubmitter().getUsername()),
                new SubmissionResponses.RepositoryBrief(
                        repository.getRepositoryId(),
                        repository.getName(),
                        repository.getSourceUrl(),
                        repository.getDefaultBranch(),
                        repository.getSyncStatus()),
                new SubmissionResponses.PullRequestBrief(
                        pullRequest.getPullRequestId(),
                        pullRequest.getExternalPrId(),
                        pullRequest.getTitle(),
                        pullRequest.getSourceBranch(),
                        pullRequest.getTargetBranch(),
                        pullRequest.getStatus(),
                        pullRequest.getExternalUrl()),
                submission.getDescription(),
                submission.getStatus(),
                quest.getRewardXp(),
                quest.getCompletionCriteria(),
                submission.getSubmittedAt());
    }

    private SubmissionResponses.ReviewRecordResponse toReviewResponse(ReviewRecord record) {
        Submission submission = record.getSubmission();
        Quest quest = submission.getQuest();
        return new SubmissionResponses.ReviewRecordResponse(
                record.getReviewId(),
                submission.getSubmissionId(),
                record.getReviewer().getUserId(),
                record.getDecision(),
                record.getSummary(),
                record.requiresChanges(),
                record.getItems().stream().map(this::toReviewItemResponse).toList(),
                submission.getStatus(),
                quest.getStatus(),
                record.getDecision().name().equals("APPROVED") ? quest.getRewardXp() : null,
                record.getReviewedAt());
    }

    private SubmissionResponses.ReviewItemResponse toReviewItemResponse(ReviewItem item) {
        return new SubmissionResponses.ReviewItemResponse(
                item.getItemId(),
                item.getCheckpoint(),
                item.getComment(),
                item.isPassed());
    }
}
