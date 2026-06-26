package com.gitguild.backend.codehost.controller;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.gitguild.backend.codehost.domain.CodePullRequest;
import com.gitguild.backend.codehost.domain.CodeRepository;
import com.gitguild.backend.codehost.gitea.GiteaAdapter;
import com.gitguild.backend.codehost.gitea.dto.FileCommitInfo;
import com.gitguild.backend.codehost.gitea.dto.PrInfo;
import com.gitguild.backend.codehost.repository.CodeIssueRepository;
import com.gitguild.backend.codehost.repository.CodePullRequestRepository;
import com.gitguild.backend.codehost.service.RepositoryService;
import com.gitguild.backend.codehost.service.RepositorySyncService;
import com.gitguild.backend.security.CurrentUserPrincipal;
import com.gitguild.backend.user.domain.User;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

class CodeHostControllerTest {

    private GiteaAdapter giteaAdapter;
    private RepositoryService repositoryService;
    private RepositorySyncService repositorySyncService;
    private CodeIssueRepository issueRepository;
    private CodePullRequestRepository pullRequestRepository;
    private MockMvc mockMvc;
    private ObjectMapper objectMapper;

    @BeforeEach
    void setUp() {
        giteaAdapter = org.mockito.Mockito.mock(GiteaAdapter.class);
        repositoryService = org.mockito.Mockito.mock(RepositoryService.class);
        repositorySyncService = org.mockito.Mockito.mock(RepositorySyncService.class);
        issueRepository = org.mockito.Mockito.mock(CodeIssueRepository.class);
        pullRequestRepository = org.mockito.Mockito.mock(CodePullRequestRepository.class);
        mockMvc = MockMvcBuilders.standaloneSetup(new CodeHostController(
                giteaAdapter,
                repositoryService,
                repositorySyncService,
                issueRepository,
                pullRequestRepository)).build();
        objectMapper = new ObjectMapper();
    }

    @Test
    void importRepositoryShouldDelegateToServiceAndReturnPlatformRepo() throws Exception {
        CodeRepository migrated = repository();
        when(repositoryService.importRepository(
                eq(42L),
                eq("https://gitea.com/ZhenchangMin/Operating-System.git"),
                eq("OS 课程仓库"),
                eq("GITEA")))
                .thenReturn(migrated);

        UsernamePasswordAuthenticationToken auth = new UsernamePasswordAuthenticationToken(
                new CurrentUserPrincipal(42L, List.of("MAINTAINER"), 1), null);

        mockMvc.perform(post("/api/v1/repositories/import")
                        .principal(auth)
                        .contentType("application/json")
                        .content(objectMapper.writeValueAsString(Map.of(
                                "sourceUrl", "https://gitea.com/ZhenchangMin/Operating-System.git",
                                "name", "OS 课程仓库",
                                "hostType", "GITEA"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.repositoryId").value(1001))
                .andExpect(jsonPath("$.data.name").value("demo-repo"));

        verify(repositoryService).importRepository(
                eq(42L),
                eq("https://gitea.com/ZhenchangMin/Operating-System.git"),
                eq("OS 课程仓库"),
                eq("GITEA"));
    }

    @Test
    void createCommitShouldUploadFileToGitea() throws Exception {
        CodeRepository repository = repository();
        when(repositoryService.requireWritableRepository(42L, 1001L)).thenReturn(repository);
        when(giteaAdapter.createFile(
                eq("spike-admin"),
                eq("demo-repo"),
                eq("task/quest-1"),
                eq("proof.md"),
                eq("finish quest"),
                eq("done")))
                .thenReturn(new FileCommitInfo("abc123", "task/quest-1", "proof.md", "http://localhost:3000/commit/abc123"));

        mockMvc.perform(post("/api/v1/repositories/1001/commits")
                        .principal(auth(42L))
                        .contentType("application/json")
                        .content(objectMapper.writeValueAsString(Map.of(
                                "branch", "task/quest-1",
                                "message", "finish quest",
                                "path", "proof.md",
                                "content", "done"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.commitId").value("abc123"))
                .andExpect(jsonPath("$.data.branch").value("task/quest-1"))
                .andExpect(jsonPath("$.data.path").value("proof.md"));
    }

    @Test
    void createPullRequestShouldCallGiteaAndPersistLocalMirror() throws Exception {
        CodeRepository repository = repository();
        when(repositoryService.requireWritableRepository(42L, 1001L)).thenReturn(repository);
        when(giteaAdapter.createPullRequest(
                eq("spike-admin"),
                eq("demo-repo"),
                eq("QST-1 finish quest"),
                eq("ready for review"),
                eq("task/quest-1"),
                eq("main")))
                .thenReturn(new PrInfo(7, "QST-1 finish quest", "open", false,
                        "task/quest-1", "main", "http://localhost:3000/spike-admin/demo-repo/pulls/7", "spike-admin"));
        when(pullRequestRepository.findByRepositoryRepositoryIdAndExternalPrId(1001L, "7"))
                .thenReturn(Optional.empty());
        when(pullRequestRepository.save(any(CodePullRequest.class))).thenAnswer(inv -> {
            CodePullRequest pr = inv.getArgument(0);
            pr.setPullRequestId(8001L);
            return pr;
        });

        mockMvc.perform(post("/api/v1/repositories/1001/pull-requests")
                        .principal(auth(42L))
                        .contentType("application/json")
                        .content(objectMapper.writeValueAsString(Map.of(
                                "title", "QST-1 finish quest",
                                "body", "ready for review",
                                "sourceBranch", "task/quest-1",
                                "targetBranch", "main"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.pullRequestId").value(8001))
                .andExpect(jsonPath("$.data.externalPrId").value("7"))
                .andExpect(jsonPath("$.data.status").value("OPEN"));

        ArgumentCaptor<CodePullRequest> captor = ArgumentCaptor.forClass(CodePullRequest.class);
        verify(pullRequestRepository).save(captor.capture());
        assertThat(captor.getValue().getSourceBranch()).isEqualTo("task/quest-1");
        assertThat(captor.getValue().getTargetBranch()).isEqualTo("main");
    }

    @Test
    void mergePullRequestShouldMergeInGiteaAndMarkLocalMirrorMerged() throws Exception {
        CodeRepository repository = repository();
        CodePullRequest pullRequest = new CodePullRequest(
                repository,
                "7",
                "QST-1 finish quest",
                "task/quest-1",
                "main",
                "OPEN",
                "http://localhost:3000/spike-admin/demo-repo/pulls/7");
        pullRequest.setPullRequestId(8001L);

        when(repositoryService.requireWritableRepository(42L, 1001L)).thenReturn(repository);
        when(pullRequestRepository.findByPullRequestIdAndRepositoryRepositoryId(8001L, 1001L))
                .thenReturn(Optional.of(pullRequest));
        when(giteaAdapter.mergePullRequest("spike-admin", "demo-repo", 7))
                .thenReturn(new PrInfo(7, "QST-1 finish quest", "closed", true,
                        "task/quest-1", "main", "http://localhost:3000/spike-admin/demo-repo/pulls/7", "spike-admin"));
        when(pullRequestRepository.save(any(CodePullRequest.class))).thenAnswer(inv -> inv.getArgument(0));

        mockMvc.perform(post("/api/v1/repositories/1001/pull-requests/8001/merge")
                        .principal(auth(42L)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.pullRequestId").value(8001))
                .andExpect(jsonPath("$.data.status").value("MERGED"));

        assertThat(pullRequest.isMerged()).isTrue();
        assertThat(pullRequest.getMergedAt()).isNotNull();
    }

    private CodeRepository repository() {
        User owner = org.mockito.Mockito.mock(User.class);
        CodeRepository repository = new CodeRepository(owner, "demo-repo", "GITEA", "http://localhost:3000/spike-admin/demo-repo");
        repository.setRepositoryId(1001L);
        repository.setDefaultBranch("main");
        return repository;
    }

    private UsernamePasswordAuthenticationToken auth(Long userId) {
        return new UsernamePasswordAuthenticationToken(
                new CurrentUserPrincipal(userId, List.of("MAINTAINER"), 1), null);
    }
}
