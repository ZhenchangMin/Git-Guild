package com.gitguild.backend.growth.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.gitguild.backend.codehost.domain.CodeRepository;
import com.gitguild.backend.growth.domain.ContributionRecord;
import com.gitguild.backend.growth.domain.GrowthProfile;
import com.gitguild.backend.growth.domain.XpTransaction;
import com.gitguild.backend.growth.dto.GrowthSummaryResponse;
import com.gitguild.backend.growth.repository.ContributionRecordRepository;
import com.gitguild.backend.growth.repository.GrowthProfileRepository;
import com.gitguild.backend.growth.repository.XpTransactionRepository;
import com.gitguild.backend.quest.domain.Quest;
import com.gitguild.backend.user.domain.User;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

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
}
