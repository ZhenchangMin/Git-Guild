package com.gitguild.backend.review.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.gitguild.backend.review.domain.SubmissionStatus;
import com.gitguild.backend.review.dto.CreateSubmissionRequest;
import com.gitguild.backend.review.dto.ReviewSubmissionRequest;
import com.gitguild.backend.review.dto.SubmissionResponses.CreateSubmissionResponse;
import com.gitguild.backend.review.dto.SubmissionResponses.ReviewRecordResponse;
import com.gitguild.backend.review.domain.ReviewDecision;
import com.gitguild.backend.quest.domain.QuestStatus;
import com.gitguild.backend.review.service.ReviewService;
import com.gitguild.backend.review.service.SubmissionService;
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

class SubmissionControllerTest {

    private SubmissionService submissionService;
    private ReviewService reviewService;
    private MockMvc mockMvc;
    private ObjectMapper objectMapper;

    @BeforeEach
    void setUp() {
        submissionService = org.mockito.Mockito.mock(SubmissionService.class);
        reviewService = org.mockito.Mockito.mock(ReviewService.class);
        mockMvc = MockMvcBuilders.standaloneSetup(new SubmissionController(submissionService, reviewService)).build();
        objectMapper = new ObjectMapper();
    }

    @Test
    void createSubmissionShouldReturnCreatedResponse() throws Exception {
        when(submissionService.createSubmission(eq(3001L), any(CreateSubmissionRequest.class)))
                .thenReturn(new CreateSubmissionResponse(
                        9001L,
                        5001L,
                        3001L,
                        8001L,
                        SubmissionStatus.PENDING_REVIEW,
                        OffsetDateTime.parse("2026-05-29T11:00:00+08:00")));

        CreateSubmissionRequest request = new CreateSubmissionRequest();
        request.setQuestId(5001L);
        request.setPullRequestId(8001L);
        request.setDescription("Implemented the requested backend feature.");

        TestingAuthenticationToken authentication = new TestingAuthenticationToken(
                new CurrentUserPrincipal(3001L, List.of("ROLE_BEGINNER"), 0),
                null);

        mockMvc.perform(post("/api/v1/submissions")
                        .principal(authentication)
                        .contentType("application/json")
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.code").value("SUCCESS"))
                .andExpect(jsonPath("$.data.submissionId").value(9001))
                .andExpect(jsonPath("$.data.questId").value(5001))
                .andExpect(jsonPath("$.data.submitterId").value(3001))
                .andExpect(jsonPath("$.data.pullRequestId").value(8001))
                .andExpect(jsonPath("$.data.status").value("PENDING_REVIEW"));

        ArgumentCaptor<CreateSubmissionRequest> requestCaptor = ArgumentCaptor.forClass(CreateSubmissionRequest.class);
        verify(submissionService).createSubmission(eq(3001L), requestCaptor.capture());
        assertThat(requestCaptor.getValue().getQuestId()).isEqualTo(5001L);
        assertThat(requestCaptor.getValue().getPullRequestId()).isEqualTo(8001L);
    }

    @Test
    void reviewSubmissionShouldReturnCreatedResponse() throws Exception {
        when(reviewService.reviewSubmission(eq(9001L), eq(2001L), any(ReviewSubmissionRequest.class)))
                .thenReturn(new ReviewRecordResponse(
                        7001L,
                        9001L,
                        2001L,
                        ReviewDecision.CHANGES_REQUESTED,
                        "Please add more tests.",
                        true,
                        List.of(),
                        SubmissionStatus.CHANGES_REQUESTED,
                        QuestStatus.IN_REVIEW,
                        null,
                        OffsetDateTime.parse("2026-05-29T11:30:00+08:00")));

        ReviewSubmissionRequest request = new ReviewSubmissionRequest();
        request.setDecision(ReviewDecision.CHANGES_REQUESTED);
        request.setSummary("Please add more tests.");

        TestingAuthenticationToken authentication = new TestingAuthenticationToken(
                new CurrentUserPrincipal(2001L, List.of("ROLE_MAINTAINER"), 0),
                null);

        mockMvc.perform(post("/api/v1/submissions/9001/reviews")
                        .principal(authentication)
                        .contentType("application/json")
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.code").value("SUCCESS"))
                .andExpect(jsonPath("$.data.reviewId").value(7001))
                .andExpect(jsonPath("$.data.submissionId").value(9001))
                .andExpect(jsonPath("$.data.reviewerId").value(2001))
                .andExpect(jsonPath("$.data.decision").value("CHANGES_REQUESTED"))
                .andExpect(jsonPath("$.data.requiresChanges").value(true))
                .andExpect(jsonPath("$.data.submissionStatus").value("CHANGES_REQUESTED"));

        ArgumentCaptor<ReviewSubmissionRequest> requestCaptor = ArgumentCaptor.forClass(ReviewSubmissionRequest.class);
        verify(reviewService).reviewSubmission(eq(9001L), eq(2001L), requestCaptor.capture());
        assertThat(requestCaptor.getValue().getDecision()).isEqualTo(ReviewDecision.CHANGES_REQUESTED);
        assertThat(requestCaptor.getValue().getSummary()).isEqualTo("Please add more tests.");
    }
}
