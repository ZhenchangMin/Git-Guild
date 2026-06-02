package com.gitguild.backend.growth.controller;

import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.gitguild.backend.growth.dto.BadgeResponse;
import com.gitguild.backend.growth.dto.GrowthSummaryResponse;
import com.gitguild.backend.growth.service.GrowthService;
import com.gitguild.backend.security.CurrentUserPrincipal;
import java.time.OffsetDateTime;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.security.authentication.TestingAuthenticationToken;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

class GrowthControllerTest {

    private GrowthService growthService;
    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        growthService = org.mockito.Mockito.mock(GrowthService.class);
        mockMvc = MockMvcBuilders.standaloneSetup(new GrowthController(growthService)).build();
    }

    @Test
    void getGrowthSummaryShouldUseCurrentUser() throws Exception {
        when(growthService.getSummary(3001L)).thenReturn(new GrowthSummaryResponse(2, 150, 200, 1));

        mockMvc.perform(get("/api/v1/users/me/growth")
                        .principal(authentication()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value("SUCCESS"))
                .andExpect(jsonPath("$.data.level").value(2))
                .andExpect(jsonPath("$.data.totalXp").value(150));

        verify(growthService).getSummary(3001L);
    }

    @Test
    void getBadgesShouldUseCurrentUser() throws Exception {
        when(growthService.getBadges(3001L))
                .thenReturn(new BadgeResponse(List.of(new BadgeResponse.BadgeItem(
                        "FIRST_COMPLETION",
                        "首次贡献",
                        "完成第一个任务后获得",
                        "完成任务数量 >= 1",
                        true,
                        OffsetDateTime.parse("2026-06-02T18:12:00+08:00"),
                        1,
                        1))));

        mockMvc.perform(get("/api/v1/users/me/badges")
                        .principal(authentication()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value("SUCCESS"))
                .andExpect(jsonPath("$.data.items[0].badgeId").value("FIRST_COMPLETION"))
                .andExpect(jsonPath("$.data.items[0].earned").value(true))
                .andExpect(jsonPath("$.data.items[0].progress").value(1));

        verify(growthService).getBadges(3001L);
    }

    private TestingAuthenticationToken authentication() {
        return new TestingAuthenticationToken(
                new CurrentUserPrincipal(3001L, List.of("ROLE_BEGINNER"), 0),
                null);
    }
}
