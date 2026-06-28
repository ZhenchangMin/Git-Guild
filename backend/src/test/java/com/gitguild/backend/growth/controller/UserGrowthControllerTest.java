package com.gitguild.backend.growth.controller;

import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.gitguild.backend.growth.dto.BadgeResponse;
import com.gitguild.backend.growth.dto.ContributionResponse;
import com.gitguild.backend.growth.dto.GrowthSummaryResponse;
import com.gitguild.backend.growth.service.GrowthService;
import java.time.OffsetDateTime;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

class UserGrowthControllerTest {

    private GrowthService growthService;
    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        growthService = org.mockito.Mockito.mock(GrowthService.class);
        mockMvc = MockMvcBuilders.standaloneSetup(new UserGrowthController(growthService)).build();
    }

    @Test
    void getGrowthSummaryShouldUseRequestedUser() throws Exception {
        when(growthService.getSummary(42L)).thenReturn(new GrowthSummaryResponse(5, 470, 500, 8));

        mockMvc.perform(get("/api/v1/users/42/growth"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.level").value(5))
                .andExpect(jsonPath("$.data.completedQuestCount").value(8));

        verify(growthService).getSummary(42L);
    }

    @Test
    void getBadgesShouldUseRequestedUser() throws Exception {
        when(growthService.getBadges(42L)).thenReturn(new BadgeResponse(List.of(
                new BadgeResponse.BadgeItem(
                        "FIRST_COMPLETION",
                        "首次贡献",
                        "完成第一个任务后获得",
                        "完成任务数量 >= 1",
                        true,
                        OffsetDateTime.parse("2026-06-02T18:12:00+08:00"),
                        1,
                        1))));

        mockMvc.perform(get("/api/v1/users/42/badges"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.items[0].badgeId").value("FIRST_COMPLETION"))
                .andExpect(jsonPath("$.data.items[0].earned").value(true));

        verify(growthService).getBadges(42L);
    }

    @Test
    void getContributionsShouldUseRequestedUser() throws Exception {
        when(growthService.getContributions(42L)).thenReturn(new ContributionResponse(List.of(), 3));

        mockMvc.perform(get("/api/v1/users/42/contributions"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.items").isArray())
                .andExpect(jsonPath("$.data.repoCount").value(3));

        verify(growthService).getContributions(42L);
    }
}
