package com.gitguild.backend.growth.controller;

import static org.hamcrest.Matchers.containsString;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.gitguild.backend.codehost.domain.CodeRepository;
import com.gitguild.backend.codehost.repository.CodeRepositoryRepository;
import com.gitguild.backend.growth.domain.ContributionRecord;
import com.gitguild.backend.growth.domain.GrowthProfile;
import com.gitguild.backend.growth.repository.ContributionRecordRepository;
import com.gitguild.backend.growth.repository.GrowthProfileRepository;
import com.gitguild.backend.quest.domain.Difficulty;
import com.gitguild.backend.quest.domain.Quest;
import com.gitguild.backend.quest.domain.QuestCategory;
import com.gitguild.backend.quest.repository.QuestCategoryRepository;
import com.gitguild.backend.quest.repository.QuestRepository;
import com.gitguild.backend.security.JwtTokenProvider;
import com.gitguild.backend.user.domain.User;
import com.gitguild.backend.user.domain.UserRole;
import com.gitguild.backend.user.repository.UserRepository;
import java.time.OffsetDateTime;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
class GrowthApiIntegrationTest {

    @Autowired private MockMvc mockMvc;
    @Autowired private JwtTokenProvider jwtTokenProvider;
    @Autowired private UserRepository userRepository;
    @Autowired private CodeRepositoryRepository codeRepositoryRepository;
    @Autowired private QuestCategoryRepository questCategoryRepository;
    @Autowired private QuestRepository questRepository;
    @Autowired private GrowthProfileRepository growthProfileRepository;
    @Autowired private ContributionRecordRepository contributionRecordRepository;

    @Test
    void leaderboardShouldBePublicAndReturnRankedUsersWithoutSensitiveFields() throws Exception {
        seedGrowthUser("alice-it", 320, 2);
        seedGrowthUser("bob-it", 180, 1);

        mockMvc.perform(get("/api/v1/leaderboards/xp")
                        .param("period", "ALL_TIME")
                        .param("limit", "2"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value("SUCCESS"))
                .andExpect(jsonPath("$.data.period").value("ALL_TIME"))
                .andExpect(jsonPath("$.data.items[0].rank").value(1))
                .andExpect(jsonPath("$.data.items[0].username").value("alice-it"))
                .andExpect(jsonPath("$.data.items[0].totalXp").value(320))
                .andExpect(jsonPath("$.data.items[0].email").doesNotExist())
                .andExpect(jsonPath("$.data.items[0].role").doesNotExist())
                .andExpect(jsonPath("$.data.items[1].rank").value(2))
                .andExpect(jsonPath("$.data.items[1].username").value("bob-it"));
    }

    @Test
    void leaderboardShouldRejectUnsupportedPeriodAndInvalidLimit() throws Exception {
        mockMvc.perform(get("/api/v1/leaderboards/xp").param("period", "WEEKLY"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("VALIDATION_FAILED"))
                .andExpect(jsonPath("$.details").value("period only supports ALL_TIME"));

        mockMvc.perform(get("/api/v1/leaderboards/xp").param("limit", "0"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("VALIDATION_FAILED"))
                .andExpect(jsonPath("$.details").value("limit must be between 1 and 100"));

        mockMvc.perform(get("/api/v1/leaderboards/xp").param("limit", "abc"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("VALIDATION_FAILED"))
                .andExpect(jsonPath("$.details", containsString("limit")));
    }

    @Test
    void badgesShouldRequireLoginAndReturnCurrentUserProgress() throws Exception {
        User user = seedGrowthUser("badge-user", 350, 3);
        String token = jwtTokenProvider.generateAccessToken(user.getUserId(), List.of("ROLE_BEGINNER"), 0);

        mockMvc.perform(get("/api/v1/users/me/badges"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("UNAUTHORIZED"));

        mockMvc.perform(get("/api/v1/users/me/badges")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value("SUCCESS"))
                .andExpect(jsonPath("$.data.items[0].badgeId").value("FIRST_COMPLETION"))
                .andExpect(jsonPath("$.data.items[0].earned").value(true))
                .andExpect(jsonPath("$.data.items[0].progress").value(3))
                .andExpect(jsonPath("$.data.items[1].badgeId").value("XP_APPRENTICE"))
                .andExpect(jsonPath("$.data.items[1].earned").value(true))
                .andExpect(jsonPath("$.data.items[1].progress").value(350))
                .andExpect(jsonPath("$.data.items[2].badgeId").value("QUEST_EXPLORER"))
                .andExpect(jsonPath("$.data.items[2].earned").value(true))
                .andExpect(jsonPath("$.data.items[3].badgeId").value("LEVEL_RISER"))
                .andExpect(jsonPath("$.data.items[3].earned").value(true));
    }

    private User seedGrowthUser(String username, int totalXp, int completedQuestCount) {
        User user = userRepository.save(new User(
                username,
                username + "@example.com",
                "{noop}password",
                UserRole.BEGINNER));
        User maintainer = userRepository.save(new User(
                username + "-maintainer",
                username + "-maintainer@example.com",
                "{noop}password",
                UserRole.MAINTAINER));
        CodeRepository repository = codeRepositoryRepository.save(new CodeRepository(
                maintainer,
                username + "-repo",
                "GITEA",
                "http://localhost:3000/" + username + "/repo"));
        QuestCategory category = questCategoryRepository.save(new QuestCategory(username + "-category", "test"));
        Quest quest = questRepository.save(new Quest(
                maintainer,
                repository,
                null,
                category,
                username + " quest",
                "description",
                "criteria",
                Difficulty.B,
                "[\"Java\"]",
                totalXp,
                4));

        GrowthProfile profile = new GrowthProfile(user);
        profile.recordQuestCompletion(totalXp);
        for (int i = 1; i < completedQuestCount; i++) {
            profile.recordQuestCompletion(0);
        }
        growthProfileRepository.save(profile);

        contributionRecordRepository.save(new ContributionRecord(
                user,
                quest,
                repository,
                quest.getTitle(),
                OffsetDateTime.parse("2026-06-02T18:12:00+08:00")));
        return user;
    }
}
