package com.gitguild.backend.quest.controller;

import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.gitguild.backend.quest.domain.Difficulty;
import com.gitguild.backend.quest.domain.QuestStatus;
import com.gitguild.backend.quest.dto.QuestResponses.IssueBrief;
import com.gitguild.backend.quest.dto.QuestResponses.MyAssignmentResponse;
import com.gitguild.backend.quest.dto.QuestResponses.RepositoryBrief;
import com.gitguild.backend.quest.service.QuestService;
import com.gitguild.backend.security.CurrentUserPrincipal;
import java.time.OffsetDateTime;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.security.authentication.TestingAuthenticationToken;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

class QuestAssignmentControllerTest {

    private QuestService questService;
    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        questService = org.mockito.Mockito.mock(QuestService.class);
        mockMvc = MockMvcBuilders.standaloneSetup(new QuestAssignmentController(questService)).build();
    }

    @Test
    void listMyActiveAssignmentsShouldUseCurrentUser() throws Exception {
        when(questService.listMyActiveAssignments(3001L)).thenReturn(List.of(new MyAssignmentResponse(
                7001L,
                5001L,
                "真实 Gitea 工作台接入",
                "创建 task branch 并展示仓库上下文",
                Difficulty.C,
                List.of("Vue", "Spring Boot"),
                180,
                QuestStatus.IN_PROGRESS,
                "ACTIVE",
                "task/quest-5001-assignment-7001-adventurer",
                OffsetDateTime.parse("2026-06-02T10:30:00+08:00"),
                new RepositoryBrief(1001L, "git-guild-demo", "main", "SYNCED"),
                new IssueBrief(3001L, "42", "接入真实 Gitea", "OPEN", "http://localhost:3000/issues/42"))));

        mockMvc.perform(get("/api/v1/users/me/quest-assignments")
                        .principal(authentication(3001L, "ROLE_BEGINNER")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value("SUCCESS"))
                .andExpect(jsonPath("$.data[0].assignmentId").value(7001))
                .andExpect(jsonPath("$.data[0].questId").value(5001))
                .andExpect(jsonPath("$.data[0].taskBranch").value("task/quest-5001-assignment-7001-adventurer"))
                .andExpect(jsonPath("$.data[0].repository.name").value("git-guild-demo"))
                .andExpect(jsonPath("$.data[0].issue.externalIssueId").value("42"));

        verify(questService).listMyActiveAssignments(3001L);
    }

    private TestingAuthenticationToken authentication(Long userId, String role) {
        return new TestingAuthenticationToken(new CurrentUserPrincipal(userId, List.of(role), 0), null);
    }
}
