package com.gitguild.backend.quest.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.gitguild.backend.quest.domain.AdminDecision;
import com.gitguild.backend.quest.domain.Difficulty;
import com.gitguild.backend.quest.domain.QuestStatus;
import com.gitguild.backend.quest.dto.AdminReviewRequest;
import com.gitguild.backend.quest.dto.QuestResponses.AdminQuestPageResponse;
import com.gitguild.backend.quest.dto.QuestResponses.AdminQuestSummaryResponse;
import com.gitguild.backend.quest.dto.QuestResponses.AdminReviewResponse;
import com.gitguild.backend.quest.dto.QuestResponses.UserBrief;
import com.gitguild.backend.quest.service.AdminQuestService;
import com.gitguild.backend.security.CurrentUserPrincipal;
import java.time.OffsetDateTime;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.security.authentication.TestingAuthenticationToken;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import static org.assertj.core.api.Assertions.assertThat;

class AdminQuestControllerTest {

    private AdminQuestService adminQuestService;
    private MockMvc mockMvc;
    private ObjectMapper objectMapper;

    @BeforeEach
    void setUp() {
        adminQuestService = org.mockito.Mockito.mock(AdminQuestService.class);
        mockMvc = MockMvcBuilders.standaloneSetup(new AdminQuestController(adminQuestService)).build();
        objectMapper = new ObjectMapper();
    }

    @Test
    void listPendingQuestsShouldReturnPageResponse() throws Exception {
        when(adminQuestService.listPendingQuests(1, 10))
                .thenReturn(new AdminQuestPageResponse(
                        List.of(new AdminQuestSummaryResponse(
                                5001L,
                                "Implement repository sync panel",
                                "Show repository sync status",
                                Difficulty.C,
                                180,
                                QuestStatus.PENDING_ADMIN_REVIEW,
                                new UserBrief(2001L, "maintainer"),
                                OffsetDateTime.parse("2026-06-02T10:00:00+08:00"))),
                        1,
                        10,
                        1,
                        1));

        mockMvc.perform(get("/api/v1/admin/quests")
                        .param("page", "1")
                        .param("size", "10"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value("SUCCESS"))
                .andExpect(jsonPath("$.data.items[0].questId").value(5001))
                .andExpect(jsonPath("$.data.items[0].status").value("PENDING_ADMIN_REVIEW"))
                .andExpect(jsonPath("$.data.page").value(1))
                .andExpect(jsonPath("$.data.size").value(10));

        verify(adminQuestService).listPendingQuests(1, 10);
    }

    @Test
    void reviewQuestShouldReturnCreatedResponseAndUseCurrentAdmin() throws Exception {
        when(adminQuestService.reviewQuest(eq(5001L), eq(1001L), any(AdminReviewRequest.class)))
                .thenReturn(new AdminReviewResponse(
                        6001L,
                        5001L,
                        1001L,
                        AdminDecision.APPROVE_PUBLISH,
                        "Ready to publish",
                        QuestStatus.PUBLISHED,
                        OffsetDateTime.parse("2026-06-02T10:15:00+08:00")));

        AdminReviewRequest request = new AdminReviewRequest();
        request.setDecision(AdminDecision.APPROVE_PUBLISH);
        request.setReason("Ready to publish");
        request.setVisibleToPublisher(true);

        TestingAuthenticationToken authentication = new TestingAuthenticationToken(
                new CurrentUserPrincipal(1001L, List.of("ROLE_ADMIN"), 0),
                null);

        mockMvc.perform(post("/api/v1/quests/5001/admin-reviews")
                        .principal(authentication)
                        .contentType("application/json")
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.code").value("SUCCESS"))
                .andExpect(jsonPath("$.data.adminReviewId").value(6001))
                .andExpect(jsonPath("$.data.questId").value(5001))
                .andExpect(jsonPath("$.data.adminId").value(1001))
                .andExpect(jsonPath("$.data.decision").value("APPROVE_PUBLISH"))
                .andExpect(jsonPath("$.data.questStatus").value("PUBLISHED"));

        ArgumentCaptor<AdminReviewRequest> requestCaptor = ArgumentCaptor.forClass(AdminReviewRequest.class);
        verify(adminQuestService).reviewQuest(eq(5001L), eq(1001L), requestCaptor.capture());
        assertThat(requestCaptor.getValue().getDecision()).isEqualTo(AdminDecision.APPROVE_PUBLISH);
        assertThat(requestCaptor.getValue().getReason()).isEqualTo("Ready to publish");
        assertThat(requestCaptor.getValue().isVisibleToPublisher()).isTrue();
    }
}
