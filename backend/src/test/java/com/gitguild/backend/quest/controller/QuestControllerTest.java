package com.gitguild.backend.quest.controller;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.gitguild.backend.quest.domain.Difficulty;
import com.gitguild.backend.quest.domain.QuestStatus;
import com.gitguild.backend.quest.dto.CreateQuestRequest;
import com.gitguild.backend.quest.dto.QuestResponses.AssignmentResponse;
import com.gitguild.backend.quest.dto.QuestResponses.CategoryBrief;
import com.gitguild.backend.quest.dto.QuestResponses.CreateQuestResponse;
import com.gitguild.backend.quest.dto.QuestResponses.QuestPageResponse;
import com.gitguild.backend.quest.dto.QuestResponses.QuestSummaryResponse;
import com.gitguild.backend.quest.dto.QuestResponses.RepositoryBrief;
import com.gitguild.backend.quest.dto.QuestResponses.UserBrief;
import com.gitguild.backend.quest.dto.QuestSearchCriteria;
import com.gitguild.backend.quest.service.QuestService;
import com.gitguild.backend.security.CurrentUserPrincipal;
import java.time.OffsetDateTime;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.security.authentication.TestingAuthenticationToken;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

class QuestControllerTest {

    private QuestService questService;
    private MockMvc mockMvc;
    private ObjectMapper objectMapper;

    @BeforeEach
    void setUp() {
        questService = org.mockito.Mockito.mock(QuestService.class);
        mockMvc = MockMvcBuilders.standaloneSetup(new QuestController(questService)).build();
        objectMapper = new ObjectMapper();
    }

    @Test
    void createQuestShouldReturnCreatedResponseAndUseCurrentUser() throws Exception {
        when(questService.createQuest(eq(2001L), any(CreateQuestRequest.class)))
                .thenReturn(new CreateQuestResponse(
                        5001L,
                        "Implement repository sync panel",
                        QuestStatus.DRAFT,
                        1001L,
                        3001L,
                        Difficulty.C,
                        180,
                        OffsetDateTime.parse("2026-06-02T10:00:00+08:00")));

        TestingAuthenticationToken authentication = authentication(2001L, "ROLE_MAINTAINER");

        mockMvc.perform(post("/api/v1/quests")
                        .principal(authentication)
                        .contentType("application/json")
                        .content(objectMapper.writeValueAsString(createQuestRequest())))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.code").value("SUCCESS"))
                .andExpect(jsonPath("$.data.questId").value(5001))
                .andExpect(jsonPath("$.data.status").value("DRAFT"))
                .andExpect(jsonPath("$.data.repositoryId").value(1001))
                .andExpect(jsonPath("$.data.issueId").value(3001));

        ArgumentCaptor<CreateQuestRequest> requestCaptor = ArgumentCaptor.forClass(CreateQuestRequest.class);
        verify(questService).createQuest(eq(2001L), requestCaptor.capture());
        assertThat(requestCaptor.getValue().getRepositoryId()).isEqualTo(1001L);
        assertThat(requestCaptor.getValue().getIssueId()).isEqualTo(3001L);
        assertThat(requestCaptor.getValue().getTechStack()).containsExactly("Vue", "Spring Boot");
    }

    @Test
    void searchQuestsShouldParseFilterParametersIntoCriteria() throws Exception {
        when(questService.searchQuests(any(QuestSearchCriteria.class)))
                .thenReturn(new QuestPageResponse(
                        List.of(new QuestSummaryResponse(
                                5001L,
                                "Implement repository sync panel",
                                "Show sync status",
                                Difficulty.C,
                                List.of("Vue", "Spring Boot"),
                                180,
                                6,
                                QuestStatus.PUBLISHED,
                                new CategoryBrief(2L, "Backend"),
                                List.of(),
                                new RepositoryBrief(1001L, "git-guild", "SYNCED"),
                                OffsetDateTime.parse("2026-06-02T10:00:00+08:00"))),
                        2,
                        8,
                        1,
                        1));

        mockMvc.perform(get("/api/v1/quests")
                        .param("keyword", "sync")
                        .param("categoryId", "2")
                        .param("tagIds", "11, 12")
                        .param("difficulty", "C")
                        .param("techStack", "Vue, Spring Boot")
                        .param("status", "PUBLISHED")
                        .param("sortBy", "rewardXp")
                        .param("sortOrder", "asc")
                        .param("page", "2")
                        .param("size", "8"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value("SUCCESS"))
                .andExpect(jsonPath("$.data.items[0].questId").value(5001))
                .andExpect(jsonPath("$.data.page").value(2))
                .andExpect(jsonPath("$.data.size").value(8));

        ArgumentCaptor<QuestSearchCriteria> criteriaCaptor = ArgumentCaptor.forClass(QuestSearchCriteria.class);
        verify(questService).searchQuests(criteriaCaptor.capture());
        QuestSearchCriteria criteria = criteriaCaptor.getValue();
        assertThat(criteria.keyword()).isEqualTo("sync");
        assertThat(criteria.categoryId()).isEqualTo(2L);
        assertThat(criteria.tagIds()).containsExactly(11L, 12L);
        assertThat(criteria.difficulty()).isEqualTo(Difficulty.C);
        assertThat(criteria.techStack()).containsExactly("Vue", "Spring Boot");
        assertThat(criteria.status()).isEqualTo(QuestStatus.PUBLISHED);
        assertThat(criteria.sortBy()).isEqualTo("rewardXp");
        assertThat(criteria.sortOrder()).isEqualTo("asc");
        assertThat(criteria.page()).isEqualTo(2);
        assertThat(criteria.size()).isEqualTo(8);
    }

    @Test
    void acceptQuestShouldReturnCreatedResponseAndUseCurrentUser() throws Exception {
        when(questService.acceptQuest(5001L, 3001L))
                .thenReturn(new AssignmentResponse(
                        7001L,
                        5001L,
                        new UserBrief(3001L, "adventurer"),
                        QuestStatus.IN_PROGRESS,
                        "ACTIVE",
                        OffsetDateTime.parse("2026-06-02T10:30:00+08:00")));

        mockMvc.perform(post("/api/v1/quests/5001/assignments")
                        .principal(authentication(3001L, "ROLE_BEGINNER")))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.code").value("SUCCESS"))
                .andExpect(jsonPath("$.data.assignmentId").value(7001))
                .andExpect(jsonPath("$.data.questId").value(5001))
                .andExpect(jsonPath("$.data.assignee.userId").value(3001))
                .andExpect(jsonPath("$.data.questStatus").value("IN_PROGRESS"))
                .andExpect(jsonPath("$.data.assignmentStatus").value("ACTIVE"));

        verify(questService).acceptQuest(5001L, 3001L);
    }

    private CreateQuestRequest createQuestRequest() {
        CreateQuestRequest request = new CreateQuestRequest();
        request.setRepositoryId(1001L);
        request.setIssueId(3001L);
        request.setTitle("Implement repository sync panel");
        request.setDescription("Show repository sync status.");
        request.setCompletionCriteria("Display latest sync time and failure reason.");
        request.setDifficulty(Difficulty.C);
        request.setTechStack(List.of("Vue", "Spring Boot"));
        request.setEstimatedHours(6);
        request.setRewardXp(180);
        request.setCategoryId(2L);
        request.setTagIds(List.of(11L, 12L));
        return request;
    }

    private TestingAuthenticationToken authentication(Long userId, String role) {
        return new TestingAuthenticationToken(new CurrentUserPrincipal(userId, List.of(role), 0), null);
    }
}
