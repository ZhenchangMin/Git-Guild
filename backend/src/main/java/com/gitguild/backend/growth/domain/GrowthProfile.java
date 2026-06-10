package com.gitguild.backend.growth.domain;

import com.gitguild.backend.user.domain.User;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.OneToOne;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import java.time.OffsetDateTime;

/**
 * Adventurer 的成长档案：XP、等级、完成任务数的每人聚合（表 {@code growth_profiles}，UNIQUE user_id）。
 *
 * <p><b>等级语义：</b>{@code level} 由 {@code totalXp} 按线性公式重算（每 {@link #XP_PER_LEVEL} XP 升一级），
 * 而非自增——因此重复调用 {@link #recordQuestCompletion} 不会因为路径重入而错算等级。
 *
 * <p><b>不变量：</b>XP 只增不减；{@code completedQuestCount} 与已发放的贡献记录一一对应
 * （由 {@code GrowthService} 的幂等保证，见 P4-022 设计文档决策 4）。
 */
@Entity
@Table(name = "growth_profiles")
public class GrowthProfile {

    /** 升级阈值：每满 100 XP 升一级。改此常量即可调整升级速度。 */
    public static final int XP_PER_LEVEL = 100;

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "profile_id")
    private Long profileId;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Column(name = "total_xp", nullable = false)
    private int totalXp;

    @Column(nullable = false)
    private int level = 1;

    @Column(name = "completed_quest_count", nullable = false)
    private int completedQuestCount;

    @Column(name = "created_at", nullable = false)
    private OffsetDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private OffsetDateTime updatedAt;

    protected GrowthProfile() {
    }

    public GrowthProfile(User user) {
        this.user = user;
        this.totalXp = 0;
        this.level = 1;
        this.completedQuestCount = 0;
    }

    @PrePersist
    void prePersist() {
        OffsetDateTime now = OffsetDateTime.now();
        this.createdAt = now;
        this.updatedAt = now;
    }

    @PreUpdate
    void preUpdate() {
        this.updatedAt = OffsetDateTime.now();
    }

    /**
     * 记录一次任务完成：累加 XP、按线性公式重算等级、完成数 +1。
     *
     * @param xp 本次发放的 XP（来自 {@code Quest.rewardXp}）
     */
    public void recordQuestCompletion(int xp) {
        this.totalXp += xp;
        this.level = this.totalXp / XP_PER_LEVEL + 1;
        this.completedQuestCount += 1;
    }

    /** 升到下一级所需的累计 XP 阈值（契约 §196 的 {@code nextLevelXp}）。 */
    public int nextLevelXp() {
        return this.level * XP_PER_LEVEL;
    }

    public Long getProfileId() {
        return profileId;
    }

    public User getUser() {
        return user;
    }

    public int getTotalXp() {
        return totalXp;
    }

    public int getLevel() {
        return level;
    }

    public int getCompletedQuestCount() {
        return completedQuestCount;
    }
}
