package com.gitguild.backend.growth.domain;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

/** 升级公式纯逻辑测试：line = totalXp / 100 + 1。 */
class GrowthProfileTest {

    @Test
    void newProfileStartsAtLevelOneWithNoXp() {
        GrowthProfile profile = new GrowthProfile(null);
        assertThat(profile.getTotalXp()).isZero();
        assertThat(profile.getLevel()).isEqualTo(1);
        assertThat(profile.getCompletedQuestCount()).isZero();
        assertThat(profile.nextLevelXp()).isEqualTo(100);
    }

    @Test
    void xpBelowThresholdStaysLevelOne() {
        GrowthProfile profile = new GrowthProfile(null);
        profile.recordQuestCompletion(99);
        assertThat(profile.getTotalXp()).isEqualTo(99);
        assertThat(profile.getLevel()).isEqualTo(1);
        assertThat(profile.getCompletedQuestCount()).isEqualTo(1);
        assertThat(profile.nextLevelXp()).isEqualTo(100);
    }

    @Test
    void crossingHundredReachesLevelTwo() {
        GrowthProfile profile = new GrowthProfile(null);
        profile.recordQuestCompletion(99);
        profile.recordQuestCompletion(1); // total 100
        assertThat(profile.getTotalXp()).isEqualTo(100);
        assertThat(profile.getLevel()).isEqualTo(2);
        assertThat(profile.nextLevelXp()).isEqualTo(200);
    }

    @Test
    void accumulatesAcrossMultipleCompletions() {
        GrowthProfile profile = new GrowthProfile(null);
        profile.recordQuestCompletion(100);
        profile.recordQuestCompletion(100); // total 200
        assertThat(profile.getTotalXp()).isEqualTo(200);
        assertThat(profile.getLevel()).isEqualTo(3);
        assertThat(profile.getCompletedQuestCount()).isEqualTo(2);
    }
}
