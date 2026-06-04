package com.gitguild.backend.review.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.gitguild.backend.codehost.domain.CodeIssue;
import com.gitguild.backend.codehost.domain.CodePullRequest;
import com.gitguild.backend.codehost.domain.CodeRepository;
import com.gitguild.backend.codehost.repository.CodePullRequestRepository;
import com.gitguild.backend.codehost.service.CodePullRequestSyncService;
import com.gitguild.backend.common.BusinessException;
import com.gitguild.backend.notification.service.NotificationService;
import com.gitguild.backend.quest.domain.AssignmentStatus;
import com.gitguild.backend.quest.domain.Difficulty;
import com.gitguild.backend.quest.domain.Quest;
import com.gitguild.backend.quest.domain.QuestAssignment;
import com.gitguild.backend.quest.domain.QuestCategory;
import com.gitguild.backend.quest.domain.QuestStatus;
import com.gitguild.backend.quest.repository.QuestAssignmentRepository;
import com.gitguild.backend.quest.repository.QuestRepository;
import com.gitguild.backend.review.domain.Submission;
import com.gitguild.backend.review.domain.SubmissionStatus;
import com.gitguild.backend.review.dto.CreateSubmissionRequest;
import com.gitguild.backend.review.dto.SubmissionResponses.CreateSubmissionResponse;
import com.gitguild.backend.review.dto.SubmissionResponses.SubmissionReviewQueueItemResponse;
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
class SubmissionServiceImplTest {

    @Mock
    private SubmissionRepository submissionRepository;
    @Mock
    private ReviewRecordRepository reviewRecordRepository;
    @Mock
    private QuestRepository questRepository;
    @Mock
    private QuestAssignmentRepository assignmentRepository;
    @Mock
    private CodePullRequestRepository pullRequestRepository;
    @Mock
    private CodePullRequestSyncService pullRequestSyncService;
    @Mock
    private UserRepository userRepository;
    @Mock
    private NotificationService notificationService;

    private SubmissionServiceImpl submissionService;

    @BeforeEach
    void setUp() {
        submissionService = new SubmissionServiceImpl(
                submissionRepository,
                reviewRecordRepository,
                questRepository,
                assignmentRepository,
                pullRequestRepository,
                pullRequestSyncService,
                userRepository,
                notificationService);
    }

    @Test
    void createSubmissionShouldCreatePendingSubmissionAndKeepQuestInProgress() {
        User maintainer = user(2001L, UserRole.MAINTAINER);
        User submitter = user(3001L, UserRole.BEGINNER);
        CodeRepository repository = repository(maintainer);
        Quest quest = quest(maintainer, repository, QuestStatus.IN_PROGRESS);
        QuestAssignment assignment = new QuestAssignment(quest, submitter);
        CodePullRequest pullRequest = pullRequest(repository);
        CreateSubmissionRequest request = request();

        when(userRepository.findById(3001L)).thenReturn(Optional.of(submitter));
        when(questRepository.findById(5001L)).thenReturn(Optional.of(quest));
        when(assignmentRepository.findByQuestAndAssigneeUserIdAndStatus(quest, 3001L, AssignmentStatus.ACTIVE))
                .thenReturn(Optional.of(assignment));
        when(submissionRepository.existsByQuestAndSubmitterUserIdAndStatusIn(any(), any(), any())).thenReturn(false);
        when(pullRequestRepository.findById(8001L)).thenReturn(Optional.of(pullRequest));
        when(submissionRepository.save(any(Submission.class))).thenAnswer(invocation -> {
            Submission submission = invocation.getArgument(0);
            submission.setSubmissionId(9001L);
            return submission;
        });

        CreateSubmissionResponse response = submissionService.createSubmission(3001L, request);

        assertThat(response.submissionId()).isEqualTo(9001L);
        assertThat(response.status()).isEqualTo(SubmissionStatus.PENDING_REVIEW);
        assertThat(quest.getStatus()).isEqualTo(QuestStatus.IN_REVIEW);
        verify(questRepository).save(quest);
    }

    @Test
    void createSubmissionShouldRejectUserWithoutActiveAssignment() {
        User maintainer = user(2001L, UserRole.MAINTAINER);
        User submitter = user(3001L, UserRole.BEGINNER);
        CodeRepository repository = repository(maintainer);
        Quest quest = quest(maintainer, repository, QuestStatus.IN_PROGRESS);

        when(userRepository.findById(3001L)).thenReturn(Optional.of(submitter));
        when(questRepository.findById(5001L)).thenReturn(Optional.of(quest));
        when(assignmentRepository.findByQuestAndAssigneeUserIdAndStatus(quest, 3001L, AssignmentStatus.ACTIVE))
                .thenReturn(Optional.empty());

        assertThatThrownBy(() -> submissionService.createSubmission(3001L, request()))
                .isInstanceOf(BusinessException.class)
                .extracting("code")
                .isEqualTo("FORBIDDEN");
    }

    @Test
    void createSubmissionShouldRejectDuplicatePendingSubmission() {
        User maintainer = user(2001L, UserRole.MAINTAINER);
        User submitter = user(3001L, UserRole.BEGINNER);
        CodeRepository repository = repository(maintainer);
        Quest quest = quest(maintainer, repository, QuestStatus.IN_PROGRESS);

        when(userRepository.findById(3001L)).thenReturn(Optional.of(submitter));
        when(questRepository.findById(5001L)).thenReturn(Optional.of(quest));
        when(assignmentRepository.findByQuestAndAssigneeUserIdAndStatus(quest, 3001L, AssignmentStatus.ACTIVE))
                .thenReturn(Optional.of(new QuestAssignment(quest, submitter)));
        when(submissionRepository.existsByQuestAndSubmitterUserIdAndStatusIn(any(), any(), any())).thenReturn(true);

        assertThatThrownBy(() -> submissionService.createSubmission(3001L, request()))
                .isInstanceOf(BusinessException.class)
                .extracting("code")
                .isEqualTo("SUBMISSION_NOT_REVIEWABLE");
    }

    @Test
    void createSubmissionShouldRejectAdminSubmitterBeforeLoadingQuest() {
        when(userRepository.findById(1001L)).thenReturn(Optional.of(user(1001L, UserRole.ADMIN)));

        assertThatThrownBy(() -> submissionService.createSubmission(1001L, request()))
                .isInstanceOf(BusinessException.class)
                .extracting("code")
                .isEqualTo("FORBIDDEN");

        verify(questRepository, never()).findById(any());
        verify(submissionRepository, never()).save(any());
    }

    @Test
    void createSubmissionShouldRejectQuestThatIsNotAcceptableForSubmission() {
        User maintainer = user(2001L, UserRole.MAINTAINER);
        User submitter = user(3001L, UserRole.BEGINNER);
        CodeRepository repository = repository(maintainer);
        Quest quest = quest(maintainer, repository, QuestStatus.PUBLISHED);

        when(userRepository.findById(3001L)).thenReturn(Optional.of(submitter));
        when(questRepository.findById(5001L)).thenReturn(Optional.of(quest));

        assertThatThrownBy(() -> submissionService.createSubmission(3001L, request()))
                .isInstanceOf(BusinessException.class)
                .extracting("code")
                .isEqualTo("QUEST_NOT_ACCEPTABLE");

        verify(assignmentRepository, never()).findByQuestAndAssigneeUserIdAndStatus(any(), any(), any());
        verify(submissionRepository, never()).save(any());
    }

    @Test
    void createSubmissionShouldRejectPullRequestFromAnotherRepository() {
        User maintainer = user(2001L, UserRole.MAINTAINER);
        User submitter = user(3001L, UserRole.BEGINNER);
        CodeRepository repository = repository(maintainer);
        CodeRepository otherRepository = repository(maintainer);
        otherRepository.setRepositoryId(1002L);
        Quest quest = quest(maintainer, repository, QuestStatus.IN_PROGRESS);
        QuestAssignment assignment = new QuestAssignment(quest, submitter);
        CodePullRequest pullRequest = pullRequest(otherRepository);

        when(userRepository.findById(3001L)).thenReturn(Optional.of(submitter));
        when(questRepository.findById(5001L)).thenReturn(Optional.of(quest));
        when(assignmentRepository.findByQuestAndAssigneeUserIdAndStatus(quest, 3001L, AssignmentStatus.ACTIVE))
                .thenReturn(Optional.of(assignment));
        when(submissionRepository.existsByQuestAndSubmitterUserIdAndStatusIn(any(), any(), any())).thenReturn(false);
        when(pullRequestRepository.findById(8001L)).thenReturn(Optional.of(pullRequest));

        assertThatThrownBy(() -> submissionService.createSubmission(3001L, request()))
                .isInstanceOf(BusinessException.class)
                .extracting("code")
                .isEqualTo("PR_NOT_FOUND");

        verify(submissionRepository, never()).save(any());
        verify(questRepository, never()).save(any());
    }

    @Test
    void listReviewQueueShouldReturnMaintainerVisibleSubmissions() {
        User maintainer = user(2001L, UserRole.MAINTAINER);
        User submitter = user(3001L, UserRole.BEGINNER);
        CodeRepository repository = repository(maintainer);
        Quest quest = quest(maintainer, repository, QuestStatus.IN_REVIEW);
        CodePullRequest pullRequest = pullRequest(repository);
        Submission submission = new Submission(quest, submitter, pullRequest, "Implemented the feature.");
        submission.setSubmissionId(9001L);

        when(userRepository.findById(2001L)).thenReturn(Optional.of(maintainer));
        when(submissionRepository.findReviewQueueForReviewer(2001L, false)).thenReturn(List.of(submission));
        when(pullRequestSyncService.syncRepositoryPullRequests(repository)).thenReturn(List.of(pullRequest));

        List<SubmissionReviewQueueItemResponse> queue = submissionService.listReviewQueue(2001L);

        verify(pullRequestSyncService).syncRepositoryPullRequests(repository);
        assertThat(queue).hasSize(1);
        assertThat(queue.get(0).submissionId()).isEqualTo(9001L);
        assertThat(queue.get(0).quest().questId()).isEqualTo(5001L);
        assertThat(queue.get(0).submitter().userId()).isEqualTo(3001L);
        assertThat(queue.get(0).repository().repositoryId()).isEqualTo(1001L);
        assertThat(queue.get(0).pullRequest().externalPrId()).isEqualTo("7");
        assertThat(queue.get(0).status()).isEqualTo(SubmissionStatus.PENDING_REVIEW);
    }

    @Test
    void listReviewQueueShouldRejectBeginner() {
        when(userRepository.findById(3001L)).thenReturn(Optional.of(user(3001L, UserRole.BEGINNER)));

        assertThatThrownBy(() -> submissionService.listReviewQueue(3001L))
                .isInstanceOf(BusinessException.class)
                .extracting("code")
                .isEqualTo("FORBIDDEN");

        verify(submissionRepository, never()).findReviewQueueForReviewer(any(), anyBoolean());
    }

    private CreateSubmissionRequest request() {
        CreateSubmissionRequest request = new CreateSubmissionRequest();
        request.setQuestId(5001L);
        request.setPullRequestId(8001L);
        request.setDescription("Implemented the requested backend feature and added tests.");
        request.setChecklist(List.of("Tests passed"));
        request.setEvidenceUrls(List.of("http://localhost:3000/pulls/7"));
        return request;
    }

    private Quest quest(User maintainer, CodeRepository repository, QuestStatus status) {
        Quest quest = new Quest(
                maintainer,
                repository,
                issue(repository),
                category(),
                "Implement submission API",
                "Task description",
                "Completion criteria",
                Difficulty.C,
                "[\"Spring Boot\"]",
                180,
                6);
        quest.setQuestId(5001L);
        quest.setStatus(status);
        return quest;
    }

    private CodePullRequest pullRequest(CodeRepository repository) {
        CodePullRequest pullRequest = new CodePullRequest(
                repository,
                "7",
                "Implement submission API",
                "feature/submission",
                "main",
                "OPEN",
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
        CodeIssue issue = new CodeIssue(repository, "42", "Submission API", "OPEN");
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
