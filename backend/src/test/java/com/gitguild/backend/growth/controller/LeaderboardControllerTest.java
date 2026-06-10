package com.gitguild.backend.growth.controller;

import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.gitguild.backend.growth.dto.LeaderboardResponse;
import com.gitguild.backend.growth.service.GrowthService;
import java.time.OffsetDateTime;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

class LeaderboardControllerTest {

    private GrowthService growthService;
    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        growthService = org.mockito.Mockito.mock(GrowthService.class);
        mockMvc = MockMvcBuilders.standaloneSetup(new LeaderboardController(growthService)).build();
    }

    @Test
    void getXpLeaderboardShouldReturnLeaderboardResponse() throws Exception {
        when(growthService.getXpLeaderboard("ALL_TIME", 3))
                .thenReturn(new LeaderboardResponse(
                        "ALL_TIME",
                        OffsetDateTime.parse("2026-06-02T20:30:00+08:00"),
                        List.of(new LeaderboardResponse.LeaderboardItem(
                                1, 3001L, "alice", 4, 320, 5))));

        mockMvc.perform(get("/api/v1/leaderboards/xp")
                        .param("period", "ALL_TIME")
                        .param("limit", "3"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value("SUCCESS"))
                .andExpect(jsonPath("$.data.period").value("ALL_TIME"))
                .andExpect(jsonPath("$.data.items[0].rank").value(1))
                .andExpect(jsonPath("$.data.items[0].userId").value(3001))
                .andExpect(jsonPath("$.data.items[0].username").value("alice"))
                .andExpect(jsonPath("$.data.items[0].totalXp").value(320));

        verify(growthService).getXpLeaderboard("ALL_TIME", 3);
    }
}
