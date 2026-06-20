package com.gitguild.backend.quest.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import java.time.OffsetDateTime;

/**
 * 平台配置项：技术栈词表。委托人发布时自由填写技术栈（逗号分隔的自由文本），
 * 管理员审核通过时据此词表校验/规范化，避免出现大小写不一致或拼写各异的同义技术栈。
 */
@Entity
@Table(name = "quest_tech_stacks")
public class QuestTechStack {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "tech_stack_id")
    private Long techStackId;

    @Column(nullable = false, unique = true, length = 64)
    private String name;

    @Column(nullable = false)
    private boolean enabled = true;

    @Column(name = "created_at", nullable = false)
    private OffsetDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private OffsetDateTime updatedAt;

    protected QuestTechStack() {
    }

    public QuestTechStack(String name) {
        this.name = name;
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

    public Long getTechStackId() {
        return techStackId;
    }

    public String getName() {
        return name;
    }

    public boolean isEnabled() {
        return enabled;
    }

    public void setTechStackId(Long techStackId) {
        this.techStackId = techStackId;
    }

    public void update(String name, Boolean enabled) {
        if (name != null && !name.isBlank()) {
            this.name = name.trim();
        }
        if (enabled != null) {
            this.enabled = enabled;
        }
    }
}
