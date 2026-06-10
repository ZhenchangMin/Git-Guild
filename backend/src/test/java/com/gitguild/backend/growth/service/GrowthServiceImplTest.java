package com.gitguild.backend.growth.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.gitguild.backend.codehost.domain.CodeRepository;
import com.gitguild.backend.common.BusinessException;
import com.gitguild.backend.growth.domain.ContributionRecord;
import com.gitguild.backend.growth.domain.GrowthProfile;
import com.gitguild.backend.growth.domain.XpTransaction;
import com.gitguild.backend.growth.dto.BadgeResponse;
import com.gitguild.backend.growth.dto.GrowthSummaryResponse;
import com.gitguild.backend.growth.dto.LeaderboardResponse;
import com.gitguild.backend.growth.repository.ContributionRecordRepository;
import com.gitguild.backend.growth.repository.GrowthProfileRepository;
import com.gitguild.backend.growth.repository.XpTransactionRepository;
import com.gitguild.backend.quest.domain.Quest;
import com.gitguild.backend.user.domain.User;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Pageable;

@ExtendWith(MockitoExtension.class)
class GrowthServiceImplTest {

    @Mock private GrowthProfileRepository growthProfileRepository;
    @Mock private XpTransactionRepository xpTransactionRepository;
    @Mock private ContributionRecordRepository contributionRecordRepository;
    @Mock private User submitter;
    @Mock private Quest quest;

    private GrowthServiceImpl service() {
        return new GrowthServiceImpl(growthProfileRepository, xpTransactionRepository, contributionRecordRepository);
    }

    @Test
    void grantSkipsWhenContributionAlreadyExists() {
        when(submitter.getUserId()).thenReturn(3001L);
        when(quest.getQuestId()).thenReturn(7001L);
        when(contributionRecordRepository.existsByUserUserIdAndQuestQuestId(3001L, 7001L)).thenReturn(true);

        service().grantQuestCompletion(submitter, quest);

        // 幂等：已发放过则一概不写
        verify(growthProfileRepository, never()).save(any());
        verify(xpTransactionRepository, never()).save(any());
        verify(contributionRecordRepository, never()).save(any());
    }

    @Test
    void grantCreatesProfileAndLedgerOnFirstCompletion() {
        when(submitter.getUserId()).thenReturn(3001L);
        when(quest.getQuestId()).thenReturn(7001L);
        when(quest.getRewardXp()).thenReturn(150);
        when(quest.getTitle()).thenReturn("Fix the bug");
        CodeRepository repo = org.mockito.Mockito.mock(CodeRepository.class);
        when(quest.getRepository()).thenReturn(repo);
        when(contributionRecordRepository.existsByUserUserIdAndQuestQuestId(3001L, 7001L)).thenReturn(false);
        when(growthProfileRepository.findByUserUserId(3001L)).thenReturn(Optional.empty());

        service().grantQuestCompletion(submitter, quest);

        ArgumentCaptor<GrowthProfile> profileCaptor = ArgumentCaptor.forClass(GrowthProfile.class);
        verify(growthProfileRepository).save(profileCaptor.capture());
        GrowthProfile saved = profileCaptor.getValue();
        assertThat(saved.getTotalXp()).isEqualTo(150);
        assertThat(saved.getLevel()).isEqualTo(2);
        assertThat(saved.getCompletedQuestCount()).isEqualTo(1);

        verify(xpTransactionRepository).save(any(XpTransaction.class));
        verify(contributionRecordRepository).save(any(ContributionRecord.class));
    }

    @Test
    void getSummaryReturnsDefaultsWhenNoProfile() {
        when(growthProfileRepository.findByUserUserId(3001L)).thenReturn(Optional.empty());

        GrowthSummaryResponse summary = service().getSummary(3001L);

        assertThat(summary.level()).isEqualTo(1);
        assertThat(summary.totalXp()).isZero();
        assertThat(summary.nextLevelXp()).isEqualTo(100);
        assertThat(summary.completedQuestCount()).isZero();
    }

    @Test
    void getXpLeaderboardReturnsRankedProfiles() {
        User firstUser = org.mockito.Mockito.mock(User.class);
        when(firstUser.getUserId()).thenReturn(1001L);
        when(firstUser.getUsername()).thenReturn("alice");
        GrowthProfile firstProfile = new GrowthProfile(firstUser);
        firstProfile.recordQuestCompletion(250);

        User secondUser = org.mockito.Mockito.mock(User.class);
        when(secondUser.getUserId()).thenReturn(1002L);
        when(secondUser.getUsername()).thenReturn("bob");
        GrowthProfile secondProfile = new GrowthProfile(secondUser);
        secondProfile.recordQuestCompletion(120);

        ArgumentCaptor<Pageable> pageableCaptor = ArgumentCaptor.forClass(Pageable.class);
        when(growthProfileRepository.findByOrderByTotalXpDescCompletedQuestCountDescUserUserIdAsc(any(Pageable.class)))
                .thenReturn(List.of(firstProfile, secondProfile));

        LeaderboardResponse response = service().getXpLeaderboard("all_time", 2);

        verify(growthProfileRepository)
                .findByOrderByTotalXpDescCompletedQuestCountDescUserUserIdAsc(pageableCaptor.capture());
        assertThat(pageableCaptor.getValue().getPageSize()).isEqualTo(2);
        assertThat(response.period()).isEqualTo("ALL_TIME");
        assertThat(response.items()).hasSize(2);
        assertThat(response.items().get(0).rank()).isEqualTo(1);
        assertThat(response.items().get(0).username()).isEqualTo("alice");
        assertThat(response.items().get(0).totalXp()).isEqualTo(250);
        assertThat(response.items().get(1).rank()).isEqualTo(2);
        assertThat(response.items().get(1).username()).isEqualTo("bob");
    }

    @Test
    void getXpLeaderboardRejectsUnsupportedPeriod() {
        assertThatThrownBy(() -> service().getXpLeaderboard("WEEKLY", 10))
                .isInstanceOf(BusinessException.class)
                .hasMessage("请求参数不合法");
    }

    @Test
    void getXpLeaderboardRejectsInvalidLimit() {
        assertThatThrownBy(() -> service().getXpLeaderboard("ALL_TIME", 101))
                .isInstanceOf(BusinessException.class)
                .hasMessage("请求参数不合法");
    }

    @Test
    void getBadgesReturnsDefaultProgressWhenNoProfile() {
        when(growthProfileRepository.findByUserUserId(3001L)).thenReturn(Optional.empty());
        when(contributionRecordRepository.findFirstByUserUserIdOrderByCompletedAtAsc(3001L)).thenReturn(Optional.empty());

        BadgeResponse response = service().getBadges(3001L);

        assertThat(response.items()).hasSize(4);
        assertThat(response.items())
                .extracting(BadgeResponse.BadgeItem::earned)
                .containsOnly(false);
        assertThat(response.items().get(0).progress()).isZero();
        assertThat(response.items().get(3).progress()).isEqualTo(1);
    }

    @Test
    void getBadgesMarksEarnedBadgesAndEarnedAt() {
        User user = org.mockito.Mockito.mock(User.class);
        GrowthProfile profile = new GrowthProfile(user);
        profile.recordQuestCompletion(150);
        profile.recordQuestCompletion(120);
        profile.recordQuestCompletion(80);
        OffsetDateTime completedAt = OffsetDateTime.parse("2026-06-02T18:12:00+08:00");
        ContributionRecord firstContribution = new ContributionRecord(
                user,
                quest,
                org.mockito.Mockito.mock(CodeRepository.class),
                "Completed quest",
                completedAt);
        when(growthProfileRepository.findByUserUserId(3001L)).thenReturn(Optional.of(profile));
        when(contributionRecordRepository.findFirstByUserUserIdOrderByCompletedAtAsc(3001L))
                .thenReturn(Optional.of(firstContribution));

        BadgeResponse response = service().getBadges(3001L);

        assertThat(response.items()).hasSize(4);
        assertThat(response.items())
                .extracting(BadgeResponse.BadgeItem::badgeId)
                .containsExactly("FIRST_COMPLETION", "XP_APPRENTICE", "QUEST_EXPLORER", "LEVEL_RISER");
        assertThat(response.items())
                .extracting(BadgeResponse.BadgeItem::earned)
                .containsExactly(true, true, true, true);
        assertThat(response.items())
                .extracting(BadgeResponse.BadgeItem::earnedAt)
                .containsOnly(completedAt);
    }
}
