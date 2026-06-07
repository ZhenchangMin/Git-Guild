package com.gitguild.backend;

import static org.hamcrest.Matchers.containsString;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.gitguild.backend.codehost.domain.CodePullRequest;
import com.gitguild.backend.codehost.domain.CodeIssue;
import com.gitguild.backend.codehost.domain.CodeRepository;
import com.gitguild.backend.codehost.gitea.GiteaAdapter;
import com.gitguild.backend.codehost.gitea.dto.BranchInfo;
import com.gitguild.backend.codehost.gitea.dto.FileCommitInfo;
import com.gitguild.backend.codehost.gitea.dto.IssueInfo;
import com.gitguild.backend.codehost.gitea.dto.PrInfo;
import com.gitguild.backend.codehost.repository.CodeIssueRepository;
import com.gitguild.backend.codehost.repository.CodePullRequestRepository;
import com.gitguild.backend.codehost.repository.CodeRepositoryRepository;
import com.gitguild.backend.growth.domain.GrowthProfile;
import com.gitguild.backend.growth.repository.GrowthProfileRepository;
import com.gitguild.backend.quest.domain.Difficulty;
import com.gitguild.backend.quest.domain.Quest;
import com.gitguild.backend.quest.domain.QuestCategory;
import com.gitguild.backend.quest.repository.QuestCategoryRepository;
import com.gitguild.backend.quest.repository.QuestRepository;
import com.gitguild.backend.review.domain.Submission;
import com.gitguild.backend.review.repository.SubmissionRepository;
import com.gitguild.backend.security.JwtTokenProvider;
import com.gitguild.backend.user.domain.User;
import com.gitguild.backend.user.domain.UserRole;
import com.gitguild.backend.user.repository.UserRepository;
import java.util.List;
import java.util.Map;
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
class P3ApiDocumentIntegrationTest {

    @Autowired private MockMvc mockMvc;
    @Autowired private ObjectMapper objectMapper;
    @Autowired private JwtTokenProvider jwtTokenProvider;
    @Autowired private UserRepository userRepository;
    @Autowired private QuestCategoryRepository categoryRepository;
    @Autowired private QuestRepository questRepository;
    @Autowired private CodeRepositoryRepository repositoryRepository;
    @Autowired private CodeIssueRepository issueRepository;
    @Autowired private CodePullRequestRepository pullRequestRepository;
    @Autowired private SubmissionRepository submissionRepository;
    @Autowired private GrowthProfileRepository growthProfileRepository;
    @MockBean private GiteaAdapter giteaAdapter;

    @Test
    void authDocumentEndpointsWorkAsUserJourney() throws Exception {
        String username = "p3-auth-user";
        String email = "p3-auth-user@example.com";

        mockMvc.perform(post("/api/v1/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(Map.of(
                                "username", username,
                                "email", email,
                                "password", "Password123",
                                "role", "BEGINNER"))))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.code").value("SUCCESS"))
                .andExpect(jsonPath("$.data.username").value(username));

        MvcResult login = mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(Map.of(
                                "account", email,
                                "password", "Password123",
                                "remember", true))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value("SUCCESS"))
                .andExpect(jsonPath("$.data.accessToken").exists())
                .andExpect(jsonPath("$.data.refreshToken").exists())
                .andReturn();
        String accessToken = data(login).get("accessToken").asText();
        String refreshToken = data(login).get("refreshToken").asText();

        // 同一账号也支持用用户名登录。
        mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(Map.of(
                                "account", username,
                                "password", "Password123"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.accessToken").exists());

        mockMvc.perform(post("/api/v1/auth/refresh")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(Map.of("refreshToken", refreshToken))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value("SUCCESS"))
                .andExpect(jsonPath("$.data.accessToken").exists());

        mockMvc.perform(get("/api/v1/users/me").header("Authorization", bearer(accessToken)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.email").value(email));

        mockMvc.perform(patch("/api/v1/users/me/password")
                        .header("Authorization", bearer(accessToken))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(Map.of(
                                "oldPassword", "Password123",
                                "newPassword", "NewPassword123"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value("SUCCESS"));

        mockMvc.perform(post("/api/v1/auth/logout").header("Authorization", bearer(accessToken)))
                .andExpect(status().isNoContent());

        mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(Map.of("account", "", "password", "x"))))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("VALIDATION_FAILED"));
    }

    @Test
    void adminTaxonomyManagementSupportsDisabledVisibilityAndDuplicates() throws Exception {
        User admin = saveUser("p3-admin-tax2", UserRole.ADMIN);
        String adminToken = token(admin);

        // 创建分类：响应应回带 description（之前 CategoryResponse 漏了该字段）。
        MvcResult created = mockMvc.perform(post("/api/v1/quest-categories")
                        .header("Authorization", bearer(adminToken))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(Map.of("name", "可停用分类", "description", "中文说明"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.description").value("中文说明"))
                .andReturn();
        long categoryId = data(created).get("categoryId").asLong();

        // 重名分类 → 409 ALREADY_EXISTS（预检命中）。
        mockMvc.perform(post("/api/v1/quest-categories")
                        .header("Authorization", bearer(adminToken))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(Map.of("name", "可停用分类"))))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("ALREADY_EXISTS"));

        // 停用该分类（不被引用拦截）。
        mockMvc.perform(patch("/api/v1/quest-categories/" + categoryId)
                        .header("Authorization", bearer(adminToken))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(Map.of("enabled", false))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.enabled").value(false));

        // 默认列表（仅启用）看不到它；includeDisabled=true 才能看到（供重新启用）。
        mockMvc.perform(get("/api/v1/quest-categories"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data[?(@.name == '可停用分类')]").isEmpty());
        mockMvc.perform(get("/api/v1/quest-categories").param("includeDisabled", "true"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data[?(@.name == '可停用分类')]").isNotEmpty());

        // 标签：创建带真实 questCount=0；重名 → 409。
        mockMvc.perform(post("/api/v1/quest-tags")
                        .header("Authorization", bearer(adminToken))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(Map.of("name", "新手友好", "color", "#43613a"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.questCount").value(0));
        mockMvc.perform(post("/api/v1/quest-tags")
                        .header("Authorization", bearer(adminToken))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(Map.of("name", "新手友好", "color", "#000000"))))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("ALREADY_EXISTS"));
    }

    @Test
    void taxonomyCodeHostAndGuideDocumentEndpointsWork() throws Exception {
        User admin = saveUser("p3-admin-tax", UserRole.ADMIN);
        User maintainer = saveUser("p3-maint-tax", UserRole.MAINTAINER);
        String adminToken = token(admin);
        String maintainerToken = token(maintainer);

        MvcResult categoryResult = mockMvc.perform(post("/api/v1/quest-categories")
                        .header("Authorization", bearer(adminToken))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(Map.of("name", "P3 Backend", "description", "backend quests"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value("SUCCESS"))
                .andExpect(jsonPath("$.data.categoryId").exists())
                .andReturn();
        long categoryId = data(categoryResult).get("categoryId").asLong();

        mockMvc.perform(get("/api/v1/quest-categories")
                        .param("withQuestCount", "true")
                        .param("sortBy", "questCount")
                        .param("sortOrder", "desc"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value("SUCCESS"));

        mockMvc.perform(patch("/api/v1/quest-categories/" + categoryId)
                        .header("Authorization", bearer(adminToken))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(Map.of("description", "updated"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.categoryId").value(categoryId));

        MvcResult tagResult = mockMvc.perform(post("/api/v1/quest-tags")
                        .header("Authorization", bearer(adminToken))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(Map.of("name", "P3Tag", "color", "#3366ff"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.tagId").exists())
                .andReturn();
        long tagId = data(tagResult).get("tagId").asLong();

        mockMvc.perform(get("/api/v1/quest-tags").param("keyword", "P3").param("page", "1").param("size", "20"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value("SUCCESS"));

        mockMvc.perform(patch("/api/v1/quest-tags/" + tagId)
                        .header("Authorization", bearer(adminToken))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(Map.of("color", "#111111"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.tagId").value(tagId));

        MvcResult repositoryResult = mockMvc.perform(post("/api/v1/repositories/import")
                        .header("Authorization", bearer(maintainerToken))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(Map.of(
                                "sourceUrl", "http://localhost:3000/p3/repo.git",
                                "hostType", "GITEA"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.repositoryId").exists())
                .andReturn();
        long repositoryId = data(repositoryResult).get("repositoryId").asLong();
        mockMvc.perform(post("/api/v1/repositories/import")
                        .header("Authorization", bearer(maintainerToken))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(Map.of(
                                "sourceUrl", "http://localhost:3000/p3/repo.git",
                                "hostType", "GITEA"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.repositoryId").value(repositoryId));
        when(giteaAdapter.listIssues("p3", "repo"))
                .thenReturn(List.of(new IssueInfo(1, "P3 synced issue", null, "open", "http://localhost:3000/p3/repo/issues/1")));
        // 平台代理写操作现已接入真实 GiteaAdapter，集成测试需 stub 写方法（否则返回 null → 500）。
        when(giteaAdapter.createBranch(any(), any(), any(), any()))
                .thenReturn(new BranchInfo("feature/p3", "sha-p3"));
        when(giteaAdapter.createFile(any(), any(), any(), any(), any(), any()))
                .thenReturn(new FileCommitInfo("sha-p3", "feature/p3", "p3.md",
                        "http://localhost:3000/p3/repo/commit/sha-p3"));
        when(giteaAdapter.createPullRequest(any(), any(), any(), any(), any(), any()))
                .thenReturn(new PrInfo(7, "P3 PR", "open", false, "feature/p3", "main",
                        "http://localhost:3000/p3/repo/pulls/7", "p3-user"));

        mockMvc.perform(get("/api/v1/repositories/" + repositoryId).header("Authorization", bearer(maintainerToken)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.repositoryId").value(repositoryId));

        mockMvc.perform(post("/api/v1/repositories/" + repositoryId + "/sync").header("Authorization", bearer(maintainerToken)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.syncStatus").value("SYNCED"));

        mockMvc.perform(get("/api/v1/repositories/" + repositoryId + "/issues")
                        .header("Authorization", bearer(maintainerToken))
                        .param("status", "OPEN")
                        .param("page", "1")
                        .param("size", "20"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.items").isArray())
                .andExpect(jsonPath("$.data.items[0].externalIssueId").value("1"))
                .andExpect(jsonPath("$.data.items[0].title").value("P3 synced issue"));

        mockMvc.perform(post("/api/v1/repositories/" + repositoryId + "/branches")
                        .header("Authorization", bearer(maintainerToken))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(Map.of("branchName", "feature/p3", "baseBranch", "main"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status").value("CREATED"));

        mockMvc.perform(post("/api/v1/repositories/" + repositoryId + "/commits")
                        .header("Authorization", bearer(maintainerToken))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(Map.of("branch", "feature/p3", "message", "Implement P3 api"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.commitId").exists());

        MvcResult prResult = mockMvc.perform(post("/api/v1/repositories/" + repositoryId + "/pull-requests")
                        .header("Authorization", bearer(maintainerToken))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(Map.of("title", "P3 PR", "sourceBranch", "feature/p3", "targetBranch", "main"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.pullRequestId").exists())
                .andReturn();
        long pullRequestId = data(prResult).get("pullRequestId").asLong();

        mockMvc.perform(get("/api/v1/repositories/" + repositoryId + "/pull-requests/" + pullRequestId)
                        .header("Authorization", bearer(maintainerToken)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.pullRequestId").value(pullRequestId));

        mockMvc.perform(post("/api/v1/code-host/webhooks/GITEA")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"event\":\"ping\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status").value("ACCEPTED"));

        mockMvc.perform(get("/api/v1/repositories/" + repositoryId + "/guide")
                        .param("includeFaq", "true")
                        .param("includeRecommendedQuests", "true")
                        .param("language", "zh-CN"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.sections").isArray());

        mockMvc.perform(post("/api/v1/repositories/import")
                        .header("Authorization", bearer(maintainerToken))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(Map.of("name", "missing-url"))))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.details").value("sourceUrl is required"));
    }

    @Test
    void questAdminSubmissionRecommendationAndGrowthDocumentEndpointsWork() throws Exception {
        User admin = saveUser("p3-admin-flow", UserRole.ADMIN);
        User maintainer = saveUser("p3-maint-flow", UserRole.MAINTAINER);
        User beginner = saveUser("p3-beginner-flow", UserRole.BEGINNER);
        String adminToken = token(admin);
        String maintainerToken = token(maintainer);
        String beginnerToken = token(beginner);
        QuestCategory category = categoryRepository.save(new QuestCategory("P3 Flow", "flow"));
        CodeRepository repository = repositoryRepository.save(new CodeRepository(
                maintainer,
                "p3-flow-repo",
                "GITEA",
                "http://localhost:3000/p3/flow"));
        repository.markSynced();
        repositoryRepository.save(repository);
        CodeIssue issue = issueRepository.save(new CodeIssue(repository, "p3-issue-flow", "P3 flow issue", "OPEN"));

        MvcResult questResult = mockMvc.perform(post("/api/v1/quests")
                        .header("Authorization", bearer(maintainerToken))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(Map.ofEntries(
                                Map.entry("repositoryId", repository.getRepositoryId()),
                                Map.entry("issueId", issue.getIssueId()),
                                Map.entry("title", "P3 flow quest"),
                                Map.entry("description", "Implement documented API flow."),
                                Map.entry("completionCriteria", "All tests pass."),
                                Map.entry("difficulty", "B"),
                                Map.entry("techStack", List.of("Java", "Spring Boot")),
                                Map.entry("estimatedHours", 4),
                                Map.entry("rewardXp", 160),
                                Map.entry("categoryId", category.getCategoryId()),
                                Map.entry("tagIds", List.of())))))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.data.questId").exists())
                .andReturn();
        long questId = data(questResult).get("questId").asLong();

        mockMvc.perform(post("/api/v1/quests/" + questId + "/submit").header("Authorization", bearer(maintainerToken)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value("SUCCESS"));

        mockMvc.perform(post("/api/v1/quests/" + questId + "/admin-reviews")
                        .header("Authorization", bearer(adminToken))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(Map.of(
                                "decision", "APPROVE_PUBLISH",
                                "reason", "meets publishing rules",
                                "visibleToPublisher", true))))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.data.questStatus").value("PUBLISHED"));

        mockMvc.perform(get("/api/v1/quests")
                        .param("difficulty", "B")
                        .param("techStack", "Java")
                        .param("page", "1")
                        .param("size", "10"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.items").isArray());

        mockMvc.perform(get("/api/v1/quests/" + questId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.questId").value(questId));

        mockMvc.perform(post("/api/v1/quests/" + questId + "/assignments").header("Authorization", bearer(beginnerToken)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.data.questId").value(questId));

        CodePullRequest pullRequest = pullRequestRepository.save(new CodePullRequest(
                repository,
                "p3-review-pr",
                "P3 review PR",
                "feature/p3-review",
                "main",
                "OPEN",
                repository.getSourceUrl()));
        Quest quest = questRepository.findById(questId).orElseThrow();
        Submission submission = submissionRepository.save(new Submission(
                quest,
                beginner,
                pullRequest,
                "Implemented the task."));

        mockMvc.perform(post("/api/v1/submissions/" + submission.getSubmissionId() + "/reviews")
                        .header("Authorization", bearer(maintainerToken))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(Map.of(
                                "decision", "CHANGES_REQUESTED",
                                "summary", "Please improve tests.",
                                "items", List.of(Map.of(
                                        "checkpoint", "tests",
                                        "comment", "add edge cases",
                                        "passed", false))))))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.data.decision").value("CHANGES_REQUESTED"));

        GrowthProfile profile = new GrowthProfile(beginner);
        profile.recordQuestCompletion(120);
        growthProfileRepository.save(profile);

        mockMvc.perform(get("/api/v1/recommendations/quests")
                        .header("Authorization", bearer(beginnerToken))
                        .param("strategy", "tech-difficulty")
                        .param("limit", "5"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value("SUCCESS"));

        mockMvc.perform(get("/api/v1/recommendations/quests/" + questId + "/contributors")
                        .header("Authorization", bearer(maintainerToken))
                        .param("limit", "5"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.questId").value(questId));

        mockMvc.perform(get("/api/v1/recommendations/reasons")
                        .header("Authorization", bearer(beginnerToken))
                        .param("questId", String.valueOf(questId)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.reasons").isArray());

        mockMvc.perform(get("/api/v1/leaderboards/xp").param("limit", "10"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.items").isArray());

        mockMvc.perform(get("/api/v1/users/me/badges").header("Authorization", bearer(beginnerToken)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.items").isArray());

        mockMvc.perform(get("/api/v1/recommendations/quests/" + questId + "/contributors")
                        .header("Authorization", bearer(maintainerToken))
                        .param("limit", "0"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.details").value("limit must be between 1 and 100"));
    }

    private User saveUser(String username, UserRole role) {
        return userRepository.save(new User(username, username + "@example.com", "{noop}Password123", role));
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
}
