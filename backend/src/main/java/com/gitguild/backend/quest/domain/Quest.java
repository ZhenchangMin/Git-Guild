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
        this.status = QuestStatus.PENDING_ADMIN_REVIEW;
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

    public boolean canBeAccepted() {
        return status == QuestStatus.PUBLISHED;
    }

    public void markInProgress() {
        this.status = QuestStatus.IN_PROGRESS;
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
