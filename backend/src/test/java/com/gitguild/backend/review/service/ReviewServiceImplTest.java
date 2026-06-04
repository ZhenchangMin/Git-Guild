package com.gitguild.backend.review.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.gitguild.backend.codehost.domain.CodeIssue;
import com.gitguild.backend.codehost.domain.CodePullRequest;
import com.gitguild.backend.codehost.domain.CodeRepository;
import com.gitguild.backend.codehost.service.CodePullRequestSyncService;
import com.gitguild.backend.common.BusinessException;
import com.gitguild.backend.growth.service.GrowthService;
import com.gitguild.backend.notification.service.NotificationService;
import com.gitguild.backend.quest.domain.AssignmentStatus;
import com.gitguild.backend.quest.domain.Difficulty;
import com.gitguild.backend.quest.domain.Quest;
import com.gitguild.backend.quest.domain.QuestAssignment;
import com.gitguild.backend.quest.domain.QuestCategory;
import com.gitguild.backend.quest.domain.QuestStatus;
import com.gitguild.backend.quest.repository.QuestAssignmentRepository;
import com.gitguild.backend.quest.repository.QuestRepository;
import com.gitguild.backend.review.domain.ReviewDecision;
import com.gitguild.backend.review.domain.ReviewRecord;
import com.gitguild.backend.review.domain.Submission;
import com.gitguild.backend.review.domain.SubmissionStatus;
import com.gitguild.backend.review.dto.ReviewSubmissionRequest;
import com.gitguild.backend.review.dto.SubmissionResponses.ReviewRecordResponse;
import com.gitguild.backend.review.repository.ReviewRecordRepository;
import com.gitguild.backend.review.repository.SubmissionRepository;
import com.gitguild.backend.user.domain.User;
import com.gitguild.backend.user.domain.UserRole;
import com.gitguild.backend.user.repository.UserRepository;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class ReviewServiceImplTest {

    @Mock
    private SubmissionRepository submissionRepository;
    @Mock
    private ReviewRecordRepository reviewRecordRepository;
    @Mock
    private QuestRepository questRepository;
    @Mock
    private QuestAssignmentRepository assignmentRepository;
    @Mock
    private UserRepository userRepository;
    @Mock
    private CodePullRequestSyncService pullRequestSyncService;
    @Mock
    private GrowthService growthService;
    @Mock
    private NotificationService notificationService;

    private ReviewServiceImpl reviewService;

    @BeforeEach
    void setUp() {
        reviewService = new ReviewServiceImpl(
                submissionRepository,
                reviewRecordRepository,
                questRepository,
                assignmentRepository,
                userRepository,
                pullRequestSyncService,
                growthService,
                notificationService);
    }

    @Test
    void reviewSubmissionShouldApproveMergedPullRequestAndCompleteQuest() {
        User maintainer = user(2001L, UserRole.MAINTAINER);
        User submitter = user(3001L, UserRole.BEGINNER);
        CodeRepository repository = repository(maintainer);
        Quest quest = quest(maintainer, repository);
        Submission submission = submission(quest, submitter, pullRequest(repository, "MERGED"));
        QuestAssignment assignment = new QuestAssignment(quest, submitter);

        when(submissionRepository.findById(9001L)).thenReturn(Optional.of(submission));
        when(userRepository.findById(2001L)).thenReturn(Optional.of(maintainer));
        when(pullRequestSyncService.syncRepositoryPullRequests(repository)).thenReturn(List.of(submission.getPullRequest()));
        when(assignmentRepository.findByQuestAndAssigneeUserIdAndStatus(quest, 3001L, AssignmentStatus.ACTIVE))
                .thenReturn(Optional.of(assignment));
        when(reviewRecordRepository.save(any(ReviewRecord.class))).thenAnswer(invocation -> invocation.getArgument(0));

        ReviewRecordResponse response = reviewService.reviewSubmission(9001L, 2001L, request(ReviewDecision.APPROVED));

        assertThat(response.decision()).isEqualTo(ReviewDecision.APPROVED);
        assertThat(response.submissionStatus()).isEqualTo(SubmissionStatus.APPROVED);
        assertThat(response.questStatus()).isEqualTo(QuestStatus.COMPLETED);
        assertThat(response.rewardXp()).isEqualTo(180);
        assertThat(submission.getStatus()).isEqualTo(SubmissionStatus.APPROVED);
        assertThat(quest.getStatus()).isEqualTo(QuestStatus.COMPLETED);
        assertThat(assignment.getStatus()).isEqualTo(AssignmentStatus.COMPLETED);
        verify(assignmentRepository).save(assignment);
        verify(growthService).grantQuestCompletion(submitter, quest);
        verify(pullRequestSyncService).syncRepositoryPullRequests(repository);
        verify(submissionRepository).save(submission);
        verify(questRepository).save(quest);
    }

    @Test
    void reviewSubmissionShouldRejectApprovalWhenPullRequestIsNotMerged() {
        User maintainer = user(2001L, UserRole.MAINTAINER);
        User submitter = user(3001L, UserRole.BEGINNER);
        CodeRepository repository = repository(maintainer);
        Submission submission = submission(quest(maintainer, repository), submitter, pullRequest(repository, "OPEN"));

        when(submissionRepository.findById(9001L)).thenReturn(Optional.of(submission));
        when(userRepository.findById(2001L)).thenReturn(Optional.of(maintainer));
        when(pullRequestSyncService.syncRepositoryPullRequests(repository)).thenReturn(List.of(submission.getPullRequest()));

        assertThatThrownBy(() -> reviewService.reviewSubmission(9001L, 2001L, request(ReviewDecision.APPROVED)))
                .isInstanceOf(BusinessException.class)
                .extracting("code")
                .isEqualTo("PR_NOT_MERGED");

        verify(pullRequestSyncService).syncRepositoryPullRequests(repository);
    }

    @Test
    void reviewSubmissionShouldSyncPullRequestBeforeApproval() {
        User maintainer = user(2001L, UserRole.MAINTAINER);
        User submitter = user(3001L, UserRole.BEGINNER);
        CodeRepository repository = repository(maintainer);
        Quest quest = quest(maintainer, repository);
        CodePullRequest pullRequest = pullRequest(repository, "OPEN");
        Submission submission = submission(quest, submitter, pullRequest);
        QuestAssignment assignment = new QuestAssignment(quest, submitter);

        when(submissionRepository.findById(9001L)).thenReturn(Optional.of(submission));
        when(userRepository.findById(2001L)).thenReturn(Optional.of(maintainer));
        when(pullRequestSyncService.syncRepositoryPullRequests(repository)).thenAnswer(invocation -> {
            pullRequest.setStatus("MERGED");
            return List.of(pullRequest);
        });
        when(assignmentRepository.findByQuestAndAssigneeUserIdAndStatus(quest, 3001L, AssignmentStatus.ACTIVE))
                .thenReturn(Optional.of(assignment));
        when(reviewRecordRepository.save(any(ReviewRecord.class))).thenAnswer(invocation -> invocation.getArgument(0));

        ReviewRecordResponse response = reviewService.reviewSubmission(9001L, 2001L, request(ReviewDecision.APPROVED));

        assertThat(response.submissionStatus()).isEqualTo(SubmissionStatus.APPROVED);
        assertThat(response.questStatus()).isEqualTo(QuestStatus.COMPLETED);
        assertThat(pullRequest.getStatus()).isEqualTo("MERGED");
        verify(pullRequestSyncService).syncRepositoryPullRequests(repository);
        verify(growthService).grantQuestCompletion(submitter, quest);
    }

    @Test
    void reviewSubmissionShouldRequestChangesWithoutCompletingQuest() {
        User maintainer = user(2001L, UserRole.MAINTAINER);
        User submitter = user(3001L, UserRole.BEGINNER);
        CodeRepository repository = repository(maintainer);
        Quest quest = quest(maintainer, repository);
        Submission submission = submission(quest, submitter, pullRequest(repository, "OPEN"));

        when(submissionRepository.findById(9001L)).thenReturn(Optional.of(submission));
        when(userRepository.findById(2001L)).thenReturn(Optional.of(maintainer));
        when(reviewRecordRepository.save(any(ReviewRecord.class))).thenAnswer(invocation -> invocation.getArgument(0));

        ReviewRecordResponse response = reviewService.reviewSubmission(9001L, 2001L, request(ReviewDecision.CHANGES_REQUESTED));

        assertThat(response.requiresChanges()).isTrue();
        assertThat(response.submissionStatus()).isEqualTo(SubmissionStatus.CHANGES_REQUESTED);
        assertThat(response.questStatus()).isEqualTo(QuestStatus.IN_REVIEW);
        assertThat(submission.getStatus()).isEqualTo(SubmissionStatus.CHANGES_REQUESTED);
        assertThat(quest.getStatus()).isEqualTo(QuestStatus.IN_REVIEW);
        verify(growthService, never()).grantQuestCompletion(any(), any());
    }

    @Test
    void reviewSubmissionShouldRejectBeginnerReviewer() {
        User maintainer = user(2001L, UserRole.MAINTAINER);
        User beginner = user(3002L, UserRole.BEGINNER);
        User submitter = user(3001L, UserRole.BEGINNER);
        CodeRepository repository = repository(maintainer);
        Submission submission = submission(quest(maintainer, repository), submitter, pullRequest(repository, "OPEN"));

        when(submissionRepository.findById(9001L)).thenReturn(Optional.of(submission));
        when(userRepository.findById(3002L)).thenReturn(Optional.of(beginner));

        assertThatThrownBy(() -> reviewService.reviewSubmission(9001L, 3002L, request(ReviewDecision.REJECTED)))
                .isInstanceOf(BusinessException.class)
                .extracting("code")
                .isEqualTo("FORBIDDEN");
    }

    @Test
    void reviewSubmissionShouldRejectAlreadyReviewedSubmission() {
        User maintainer = user(2001L, UserRole.MAINTAINER);
        User submitter = user(3001L, UserRole.BEGINNER);
        CodeRepository repository = repository(maintainer);
        Submission submission = submission(quest(maintainer, repository), submitter, pullRequest(repository, "MERGED"));
        submission.approve();

        when(submissionRepository.findById(9001L)).thenReturn(Optional.of(submission));
        when(userRepository.findById(2001L)).thenReturn(Optional.of(maintainer));

        assertThatThrownBy(() -> reviewService.reviewSubmission(9001L, 2001L, request(ReviewDecision.REJECTED)))
                .isInstanceOf(BusinessException.class)
                .extracting("code")
                .isEqualTo("SUBMISSION_ALREADY_REVIEWED");
    }

    private ReviewSubmissionRequest request(ReviewDecision decision) {
        ReviewSubmissionRequest request = new ReviewSubmissionRequest();
        request.setDecision(decision);
        request.setSummary("Review summary");
        ReviewSubmissionRequest.ReviewItemRequest item = new ReviewSubmissionRequest.ReviewItemRequest();
        item.setCheckpoint("Functionality");
        item.setComment("Looks good");
        item.setPassed(decision != ReviewDecision.CHANGES_REQUESTED);
        request.setItems(List.of(item));
        return request;
    }

    private Submission submission(Quest quest, User submitter, CodePullRequest pullRequest) {
        Submission submission = new Submission(quest, submitter, pullRequest, "Implemented the feature.");
        submission.setSubmissionId(9001L);
        return submission;
    }

    private Quest quest(User maintainer, CodeRepository repository) {
        Quest quest = new Quest(
                maintainer,
                repository,
                issue(repository),
                category(),
                "Implement review API",
                "Task description",
                "Completion criteria",
                Difficulty.C,
                "[\"Spring Boot\"]",
                180,
                6);
        quest.setQuestId(5001L);
        quest.setStatus(QuestStatus.IN_REVIEW);
        return quest;
    }

    private CodePullRequest pullRequest(CodeRepository repository, String status) {
        CodePullRequest pullRequest = new CodePullRequest(
                repository,
                "7",
                "Implement review API",
                "feature/review",
                "main",
                status,
                "http://localhost:3000/pulls/7");
        pullRequest.setPullRequestId(8001L);
        return pullRequest;
    }

    private QuestCategory category() {
        QuestCategory category = new QuestCategory("Backend", "Backend tasks");
        category.setCategoryId(2L);
        return category;
    }

    private CodeIssue issue(CodeRepository repository) {
        CodeIssue issue = new CodeIssue(repository, "42", "Review API", "OPEN");
        issue.setIssueId(3001L);
        return issue;
    }

    private CodeRepository repository(User owner) {
        CodeRepository repository = new CodeRepository(owner, "git-guild", "GITEA", "http://localhost:3000/git-guild");
        repository.setRepositoryId(1001L);
        repository.setSyncStatus("SYNCED");
        return repository;
    }

    private User user(Long id, UserRole role) {
        User user = new User("user" + id, "user" + id + "@example.com", "password-hash", role);
        user.setUserId(id);
        return user;
    }
}
