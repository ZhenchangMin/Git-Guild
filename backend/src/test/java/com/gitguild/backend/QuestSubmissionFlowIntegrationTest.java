package com.gitguild.backend;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.gitguild.backend.codehost.gitea.GiteaAdapter;
import com.gitguild.backend.codehost.gitea.dto.IssueInfo;
import com.gitguild.backend.codehost.gitea.dto.PrInfo;
import com.gitguild.backend.growth.repository.ContributionRecordRepository;
import com.gitguild.backend.growth.repository.GrowthProfileRepository;
import com.gitguild.backend.growth.repository.XpTransactionRepository;
import com.gitguild.backend.quest.domain.AssignmentStatus;
import com.gitguild.backend.quest.domain.Difficulty;
import com.gitguild.backend.quest.domain.Quest;
import com.gitguild.backend.quest.domain.QuestCategory;
import com.gitguild.backend.quest.domain.QuestStatus;
import com.gitguild.backend.quest.repository.QuestAssignmentRepository;
import com.gitguild.backend.quest.repository.QuestCategoryRepository;
import com.gitguild.backend.quest.repository.QuestRepository;
import com.gitguild.backend.review.domain.ReviewDecision;
import com.gitguild.backend.review.domain.SubmissionStatus;
import com.gitguild.backend.review.repository.ReviewRecordRepository;
import com.gitguild.backend.review.repository.SubmissionRepository;
import com.gitguild.backend.security.JwtTokenProvider;
import com.gitguild.backend.user.domain.User;
import com.gitguild.backend.user.domain.UserRole;
import com.gitguild.backend.user.repository.UserRepository;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class QuestSubmissionFlowIntegrationTest {

    private static final String PASSWORD = "Password123";
    private static final int REWARD_XP = 180;

    @Autowired private MockMvc mockMvc;
    @Autowired private ObjectMapper objectMapper;
    @Autowired private JwtTokenProvider jwtTokenProvider;
    @Autowired private UserRepository userRepository;
    @Autowired private QuestCategoryRepository categoryRepository;
    @Autowired private QuestRepository questRepository;
    @Autowired private QuestAssignmentRepository assignmentRepository;
    @Autowired private SubmissionRepository submissionRepository;
    @Autowired private ReviewRecordRepository reviewRecordRepository;
    @Autowired private GrowthProfileRepository growthProfileRepository;
    @Autowired private XpTransactionRepository xpTransactionRepository;
    @Autowired private ContributionRecordRepository contributionRecordRepository;
    @MockBean private GiteaAdapter giteaAdapter;

    @BeforeEach
    void resetMocks() {
        reset(giteaAdapter);
    }

    @Test
    void completeQuestSubmissionJourneyFromRegistrationToReviewApproval() throws Exception {
        String suffix = uniqueSuffix();
        AuthUser maintainer = registerAndLogin("maint-" + suffix, UserRole.MAINTAINER);
        AuthUser adventurer = registerAndLogin("adv-" + suffix, UserRole.BEGINNER);
        String adminToken = token(saveAdmin("admin-" + suffix));
        QuestCategory category = categoryRepository.save(new QuestCategory("Integration Flow " + suffix, "integration"));
        String sourceUrl = "http://localhost:3000/guild/flow-" + suffix + ".git";

        MvcResult repositoryResult = mockMvc.perform(post("/api/v1/repositories/import")
                        .header("Authorization", bearer(maintainer.token()))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(Map.of("sourceUrl", sourceUrl, "hostType", "GITEA"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value("SUCCESS"))
                .andExpect(jsonPath("$.data.repositoryId").exists())
                .andReturn();
        long repositoryId = data(repositoryResult).get("repositoryId").asLong();

        when(giteaAdapter.listIssues("guild", "flow-" + suffix))
                .thenReturn(List.of(new IssueInfo(1, "Implement integration flow", "open",
                        "http://localhost:3000/guild/flow-" + suffix + "/issues/1")));
        MvcResult issueSyncResult = mockMvc.perform(post("/api/v1/repositories/" + repositoryId + "/sync")
                        .header("Authorization", bearer(maintainer.token())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.syncStatus").value("SYNCED"))
                .andReturn();
        assertThat(data(issueSyncResult).get("repositoryId").asLong()).isEqualTo(repositoryId);

        MvcResult issueListResult = mockMvc.perform(get("/api/v1/repositories/" + repositoryId + "/issues")
                        .header("Authorization", bearer(maintainer.token()))
                        .param("status", "OPEN")
                        .param("page", "1")
                        .param("size", "20"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.items[0].title").value("Implement integration flow"))
                .andReturn();
        long issueId = data(issueListResult).get("items").get(0).get("issueId").asLong();

        long questId = createQuest(maintainer.token(), repositoryId, issueId, category.getCategoryId(), "Complete flow " + suffix);

        mockMvc.perform(post("/api/v1/quests/" + questId + "/submit")
                        .header("Authorization", bearer(maintainer.token())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status").value("PENDING_ADMIN_REVIEW"));

        mockMvc.perform(post("/api/v1/quests/" + questId + "/admin-reviews")
                        .header("Authorization", bearer(adminToken))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(Map.of(
                                "decision", "APPROVE_PUBLISH",
                                "reason", "ready for integration test",
                                "visibleToPublisher", true))))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.data.questStatus").value("PUBLISHED"));

        mockMvc.perform(get("/api/v1/quests")
                        .param("keyword", "Complete flow " + suffix)
                        .param("page", "1")
                        .param("size", "10"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.items[0].questId").value(questId));

        mockMvc.perform(get("/api/v1/quests/" + questId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.questId").value(questId));

        mockMvc.perform(post("/api/v1/quests/" + questId + "/assignments")
                        .header("Authorization", bearer(adventurer.token())))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.data.questStatus").value("IN_PROGRESS"));

        when(giteaAdapter.listPulls("guild", "flow-" + suffix))
                .thenReturn(List.of(new PrInfo(2, "Integration PR", "closed", true,
                        "feature/integration-flow", "main",
                        "http://localhost:3000/guild/flow-" + suffix + "/pulls/2",
                        adventurer.username())));
        MvcResult draftResult = mockMvc.perform(get("/api/v1/quests/" + questId + "/submission-draft")
                        .header("Authorization", bearer(adventurer.token())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.pullRequests[0].status").value("MERGED"))
                .andReturn();
        long pullRequestId = data(draftResult).get("pullRequests").get(0).get("pullRequestId").asLong();

        MvcResult submissionResult = mockMvc.perform(post("/api/v1/submissions")
                        .header("Authorization", bearer(adventurer.token()))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(Map.of(
                                "questId", questId,
                                "pullRequestId", pullRequestId,
                                "description", "Implemented and verified the integration flow.",
                                "checklist", List.of("tests pass", "PR merged"),
                                "evidenceUrls", List.of("http://localhost:3000/guild/flow-" + suffix + "/pulls/2")))))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.data.status").value("PENDING_REVIEW"))
                .andReturn();
        long submissionId = data(submissionResult).get("submissionId").asLong();

        mockMvc.perform(get("/api/v1/submissions/review-queue")
                        .header("Authorization", bearer(maintainer.token())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data[0].submissionId").value(submissionId));

        MvcResult reviewResult = mockMvc.perform(post("/api/v1/submissions/" + submissionId + "/reviews")
                        .header("Authorization", bearer(maintainer.token()))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(Map.of(
                                "decision", "APPROVED",
                                "summary", "Accepted. The implementation meets the review checkpoints.",
                                "items", List.of(
                                        Map.of("checkpoint", "PR merged", "comment", "Merged before review", "passed", true),
                                        Map.of("checkpoint", "tests", "comment", "Integration path verified", "passed", true)))) ))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.data.decision").value("APPROVED"))
                .andExpect(jsonPath("$.data.questStatus").value("COMPLETED"))
                .andExpect(jsonPath("$.data.rewardXp").value(REWARD_XP))
                .andExpect(jsonPath("$.data.items.length()").value(2))
                .andReturn();
        long reviewId = data(reviewResult).get("reviewId").asLong();

        Quest completedQuest = questRepository.findById(questId).orElseThrow();
        assertThat(completedQuest.getStatus()).isEqualTo(QuestStatus.COMPLETED);
        assertThat(submissionRepository.findById(submissionId).orElseThrow().getStatus()).isEqualTo(SubmissionStatus.APPROVED);
        assertThat(assignmentRepository.findByAssigneeUserIdAndStatus(adventurer.userId(), AssignmentStatus.COMPLETED))
                .hasSize(1);
        assertThat(growthProfileRepository.findByUserUserId(adventurer.userId()).orElseThrow().getTotalXp())
                .isEqualTo(REWARD_XP);
        assertThat(growthProfileRepository.findByUserUserId(adventurer.userId()).orElseThrow().getCompletedQuestCount())
                .isEqualTo(1);
        assertThat(contributionRecordRepository.existsByUserUserIdAndQuestQuestId(adventurer.userId(), questId))
                .isTrue();
        assertThat(xpTransactionRepository.findAll())
                .anySatisfy(transaction -> {
                    assertThat(transaction.getUser().getUserId()).isEqualTo(adventurer.userId());
                    assertThat(transaction.getQuest().getQuestId()).isEqualTo(questId);
                    assertThat(transaction.getAmount()).isEqualTo(REWARD_XP);
                });
        assertThat(reviewRecordRepository.findBySubmissionSubmissionIdOrderByReviewedAtDesc(submissionId))
                .singleElement()
                .satisfies(record -> {
                    assertThat(record.getReviewId()).isEqualTo(reviewId);
                    assertThat(record.getDecision()).isEqualTo(ReviewDecision.APPROVED);
                });
    }

    @Test
    void protectedWorkflowEndpointsShouldRejectAnonymousAccess() throws Exception {
        mockMvc.perform(post("/api/v1/quests/999/assignments"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("UNAUTHORIZED"));

        mockMvc.perform(post("/api/v1/submissions")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(Map.of(
                                "questId", 999,
                                "pullRequestId", 888,
                                "description", "anonymous submission"))))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("UNAUTHORIZED"));

        mockMvc.perform(get("/api/v1/submissions/review-queue"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("UNAUTHORIZED"));
    }

    @Test
    void duplicateAssignmentShouldBeRejected() throws Exception {
        String suffix = uniqueSuffix();
        AuthUser maintainer = registerAndLogin("dup-maint-" + suffix, UserRole.MAINTAINER);
        AuthUser adventurer = registerAndLogin("dup-adv-" + suffix, UserRole.BEGINNER);
        PublishedQuest publishedQuest = createPublishedQuest("dup-" + suffix, maintainer);

        mockMvc.perform(post("/api/v1/quests/" + publishedQuest.questId() + "/assignments")
                        .header("Authorization", bearer(adventurer.token())))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.data.questStatus").value("IN_PROGRESS"));

        mockMvc.perform(post("/api/v1/quests/" + publishedQuest.questId() + "/assignments")
                        .header("Authorization", bearer(adventurer.token())))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("DUPLICATE_ASSIGNMENT"));

        long activeAssignments = assignmentRepository.findByAssigneeUserIdAndStatus(adventurer.userId(), AssignmentStatus.ACTIVE)
                .stream()
                .filter(assignment -> assignment.getQuest().getQuestId().equals(publishedQuest.questId()))
                .count();
        assertThat(activeAssignments).isEqualTo(1);
    }

    @Test
    void approvalShouldBeRejectedWhenPullRequestIsNotMerged() throws Exception {
        String suffix = uniqueSuffix();
        AuthUser maintainer = registerAndLogin("open-maint-" + suffix, UserRole.MAINTAINER);
        AuthUser adventurer = registerAndLogin("open-adv-" + suffix, UserRole.BEGINNER);
        PublishedQuest publishedQuest = createPublishedQuest("open-" + suffix, maintainer);

        mockMvc.perform(post("/api/v1/quests/" + publishedQuest.questId() + "/assignments")
                        .header("Authorization", bearer(adventurer.token())))
                .andExpect(status().isCreated());

        when(giteaAdapter.listPulls("guild", publishedQuest.repoName()))
                .thenReturn(List.of(new PrInfo(3, "Open PR", "open", false,
                        "feature/open-pr", "main",
                        "http://localhost:3000/guild/" + publishedQuest.repoName() + "/pulls/3",
                        adventurer.username())));
        MvcResult draftResult = mockMvc.perform(get("/api/v1/quests/" + publishedQuest.questId() + "/submission-draft")
                        .header("Authorization", bearer(adventurer.token())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.pullRequests[0].status").value("OPEN"))
                .andReturn();
        long pullRequestId = data(draftResult).get("pullRequests").get(0).get("pullRequestId").asLong();

        MvcResult submissionResult = mockMvc.perform(post("/api/v1/submissions")
                        .header("Authorization", bearer(adventurer.token()))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(Map.of(
                                "questId", publishedQuest.questId(),
                                "pullRequestId", pullRequestId,
                                "description", "Work is submitted before merge."))))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.data.status").value("PENDING_REVIEW"))
                .andReturn();
        long submissionId = data(submissionResult).get("submissionId").asLong();

        mockMvc.perform(post("/api/v1/submissions/" + submissionId + "/reviews")
                        .header("Authorization", bearer(maintainer.token()))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(Map.of(
                                "decision", "APPROVED",
                                "summary", "Attempting to approve before merge.",
                                "items", List.of(Map.of(
                                        "checkpoint", "PR merged",
                                        "comment", "PR must be merged first",
                                        "passed", false)))) ))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("PR_NOT_MERGED"));

        assertThat(questRepository.findById(publishedQuest.questId()).orElseThrow().getStatus())
                .isEqualTo(QuestStatus.IN_REVIEW);
        assertThat(submissionRepository.findById(submissionId).orElseThrow().getStatus())
                .isEqualTo(SubmissionStatus.PENDING_REVIEW);
        assertThat(growthProfileRepository.findByUserUserId(adventurer.userId())).isEmpty();
        assertThat(contributionRecordRepository.existsByUserUserIdAndQuestQuestId(adventurer.userId(), publishedQuest.questId()))
                .isFalse();
    }

    private PublishedQuest createPublishedQuest(String suffix, AuthUser maintainer) throws Exception {
        String adminToken = token(saveAdmin("admin-" + suffix));
        QuestCategory category = categoryRepository.save(new QuestCategory("Category " + suffix, "integration"));
        String repoName = "repo-" + suffix;
        String sourceUrl = "http://localhost:3000/guild/" + repoName + ".git";

        MvcResult repositoryResult = mockMvc.perform(post("/api/v1/repositories/import")
                        .header("Authorization", bearer(maintainer.token()))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(Map.of("sourceUrl", sourceUrl, "hostType", "GITEA"))))
                .andExpect(status().isOk())
                .andReturn();
        long repositoryId = data(repositoryResult).get("repositoryId").asLong();

        when(giteaAdapter.listIssues("guild", repoName))
                .thenReturn(List.of(new IssueInfo(1, "Issue " + suffix, "open",
                        "http://localhost:3000/guild/" + repoName + "/issues/1")));
        mockMvc.perform(post("/api/v1/repositories/" + repositoryId + "/sync")
                        .header("Authorization", bearer(maintainer.token())))
                .andExpect(status().isOk());
        MvcResult issueListResult = mockMvc.perform(get("/api/v1/repositories/" + repositoryId + "/issues")
                        .header("Authorization", bearer(maintainer.token()))
                        .param("status", "OPEN"))
                .andExpect(status().isOk())
                .andReturn();
        long issueId = data(issueListResult).get("items").get(0).get("issueId").asLong();

        long questId = createQuest(maintainer.token(), repositoryId, issueId, category.getCategoryId(), "Quest " + suffix);
        mockMvc.perform(post("/api/v1/quests/" + questId + "/submit")
                        .header("Authorization", bearer(maintainer.token())))
                .andExpect(status().isOk());
        mockMvc.perform(post("/api/v1/quests/" + questId + "/admin-reviews")
                        .header("Authorization", bearer(adminToken))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(Map.of(
                                "decision", "APPROVE_PUBLISH",
                                "reason", "publish for integration test",
                                "visibleToPublisher", true))))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.data.questStatus").value("PUBLISHED"));
        return new PublishedQuest(questId, repoName);
    }

    private long createQuest(String maintainerToken, long repositoryId, long issueId, long categoryId, String title) throws Exception {
        MvcResult questResult = mockMvc.perform(post("/api/v1/quests")
                        .header("Authorization", bearer(maintainerToken))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(Map.ofEntries(
                                Map.entry("repositoryId", repositoryId),
                                Map.entry("issueId", issueId),
                                Map.entry("title", title),
                                Map.entry("description", "Implement the requested change and verify it with tests."),
                                Map.entry("completionCriteria", "Pull request is merged and review checkpoints pass."),
                                Map.entry("difficulty", Difficulty.B.name()),
                                Map.entry("techStack", List.of("Java", "Spring Boot")),
                                Map.entry("estimatedHours", 3),
                                Map.entry("rewardXp", REWARD_XP),
                                Map.entry("categoryId", categoryId),
                                Map.entry("tagIds", List.of())))))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.data.questId").exists())
                .andReturn();
        return data(questResult).get("questId").asLong();
    }

    private AuthUser registerAndLogin(String username, UserRole role) throws Exception {
        String email = username + "@example.com";
        mockMvc.perform(post("/api/v1/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(Map.of(
                                "username", username,
                                "email", email,
                                "password", PASSWORD,
                                "role", role.name()))))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.data.username").value(username))
                .andExpect(jsonPath("$.data.role").value(role.name()));

        MvcResult loginResult = mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(Map.of(
                                "account", email,
                                "password", PASSWORD,
                                "remember", true))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.accessToken").exists())
                .andReturn();
        JsonNode loginData = data(loginResult);
        return new AuthUser(
                loginData.get("user").get("userId").asLong(),
                username,
                loginData.get("accessToken").asText());
    }

    private User saveAdmin(String username) {
        return userRepository.save(new User(username, username + "@example.com", "{noop}" + PASSWORD, UserRole.ADMIN));
    }

    private String token(User user) {
        return jwtTokenProvider.generateAccessToken(user.getUserId(), List.of("ROLE_" + user.getRole().name()), user.getTokenVersion());
    }

    private String bearer(String token) {
        return "Bearer " + token;
    }

    private String json(Object value) throws Exception {
        return objectMapper.writeValueAsString(value);
    }

    private JsonNode data(MvcResult result) throws Exception {
        return objectMapper.readTree(result.getResponse().getContentAsString()).get("data");
    }

    private String uniqueSuffix() {
        return UUID.randomUUID().toString().replace("-", "").substring(0, 8);
    }

    private record AuthUser(Long userId, String username, String token) {
    }

    private record PublishedQuest(Long questId, String repoName) {
    }
}
