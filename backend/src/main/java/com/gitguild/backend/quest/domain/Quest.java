package com.gitguild.backend.quest.domain;

import com.gitguild.backend.codehost.domain.CodeIssue;
import com.gitguild.backend.codehost.domain.CodeRepository;
import com.gitguild.backend.user.domain.User;
import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.JoinTable;
import jakarta.persistence.ManyToMany;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import java.time.OffsetDateTime;
import java.util.LinkedHashSet;
import java.util.Set;

@Entity
@Table(name = "quests")
public class Quest {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "quest_id")
    private Long questId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "publisher_id", nullable = false)
    private User publisher;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "repository_id", nullable = false)
    private CodeRepository repository;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "issue_id")
    private CodeIssue issue;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "category_id", nullable = false)
    private QuestCategory category;

    @Column(nullable = false, length = 200)
    private String title;

    @Column(nullable = false, columnDefinition = "TEXT")
    private String description;

    @Column(name = "completion_criteria", nullable = false, columnDefinition = "TEXT")
    private String completionCriteria;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 16)
    private Difficulty difficulty;

    @Column(name = "tech_stack", nullable = false, columnDefinition = "json")
    private String techStackJson;

    @Column(name = "reward_xp", nullable = false)
    private int rewardXp;

    @Column(name = "estimated_hours", nullable = false)
    private int estimatedHours;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 32)
    private QuestStatus status;

    @Column(name = "published_at")
    private OffsetDateTime publishedAt;

    @Column(name = "created_at", nullable = false)
    private OffsetDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private OffsetDateTime updatedAt;

    @ManyToMany(cascade = {CascadeType.PERSIST, CascadeType.MERGE})
    @JoinTable(
            name = "quest_tag_relations",
            joinColumns = @JoinColumn(name = "quest_id"),
            inverseJoinColumns = @JoinColumn(name = "tag_id"))
    private Set<QuestTag> tags = new LinkedHashSet<>();

    protected Quest() {
    }

    public Quest(
            User publisher,
            CodeRepository repository,
            CodeIssue issue,
            QuestCategory category,
            String title,
            String description,
            String completionCriteria,
            Difficulty difficulty,
            String techStackJson,
            int rewardXp,
            int estimatedHours) {
        this.publisher = publisher;
        this.repository = repository;
        this.issue = issue;
        this.category = category;
        this.title = title;
        this.description = description;
        this.completionCriteria = completionCriteria;
        this.difficulty = difficulty;
        this.techStackJson = techStackJson;
        this.rewardXp = rewardXp;
        this.estimatedHours = estimatedHours;
        this.status = QuestStatus.DRAFT;
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

    public boolean canSubmitForReview() {
        return status == QuestStatus.DRAFT || status == QuestStatus.REJECTED;
    }

    public void submitForReview() {
        this.status = QuestStatus.PENDING_ADMIN_REVIEW;
    }

    public boolean canBeApprovedOrRejected() {
        return status == QuestStatus.PENDING_ADMIN_REVIEW;
    }

    public void approve() {
        this.status = QuestStatus.PUBLISHED;
        this.publishedAt = OffsetDateTime.now();
    }

    public void reject() {
        this.status = QuestStatus.REJECTED;
    }

    public boolean canBeTakenDown() {
        return status != QuestStatus.CLOSED;
    }

    public void takeDown() {
        this.status = QuestStatus.CLOSED;
    }

    public boolean canBeReopened() {
        return status == QuestStatus.CLOSED;
    }

    /** 重新上架：CLOSED → PUBLISHED。此前因下架被取消的接取记录不会恢复，需要冒险家重新接取。 */
    public void reopen() {
        this.status = QuestStatus.PUBLISHED;
        this.publishedAt = OffsetDateTime.now();
    }

    public boolean canBeAccepted() {
        return status == QuestStatus.PUBLISHED || status == QuestStatus.IN_PROGRESS;
    }

    public void markInProgress() {
        this.status = QuestStatus.IN_PROGRESS;
    }

    /**
     * 保留状态，不作为默认主流程节点。
     * 供后续 Guild Master 主动锁定某个候选提交进行最终确认时使用。
     */
    public void markInReview() {
        this.status = QuestStatus.IN_REVIEW;
    }

    public void markCompleted() {
        this.status = QuestStatus.COMPLETED;
    }

    public void addTags(Set<QuestTag> tags) {
        this.tags.addAll(tags);
    }

    public Long getQuestId() {
        return questId;
    }

    public User getPublisher() {
        return publisher;
    }

    public CodeRepository getRepository() {
        return repository;
    }

    public CodeIssue getIssue() {
        return issue;
    }

    public QuestCategory getCategory() {
        return category;
    }

    public String getTitle() {
        return title;
    }

    public String getDescription() {
        return description;
    }

    public String getCompletionCriteria() {
        return completionCriteria;
    }

    public Difficulty getDifficulty() {
        return difficulty;
    }

    public String getTechStackJson() {
        return techStackJson;
    }

    /** 审核通过时把委托技术栈规范化为平台已登记的标准写法（大小写统一），不改变发布表单的自由输入流程。 */
    public void setTechStackJson(String techStackJson) {
        this.techStackJson = techStackJson;
    }

    public int getRewardXp() {
        return rewardXp;
    }

    public int getEstimatedHours() {
        return estimatedHours;
    }

    public QuestStatus getStatus() {
        return status;
    }

    public OffsetDateTime getCreatedAt() {
        return createdAt;
    }

    public OffsetDateTime getUpdatedAt() {
        return updatedAt;
    }

    public Set<QuestTag> getTags() {
        return tags;
    }

    public void setQuestId(Long questId) {
        this.questId = questId;
    }

    public void setStatus(QuestStatus status) {
        this.status = status;
    }
}
