package com.gitguild.backend.codehost.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import java.time.OffsetDateTime;

@Entity
@Table(name = "issues")
public class CodeIssue {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "issue_id")
    private Long issueId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "repository_id", nullable = false)
    private CodeRepository repository;

    @Column(name = "external_issue_id", nullable = false, length = 128)
    private String externalIssueId;

    @Column(nullable = false, length = 200)
    private String title;

    @Column(columnDefinition = "TEXT")
    private String body;

    @Column(nullable = false, length = 32)
    private String status;

    @Column(name = "external_url", length = 512)
    private String externalUrl;

    @Column(name = "synced_at")
    private OffsetDateTime syncedAt;

    @Column(name = "created_at", nullable = false)
    private OffsetDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private OffsetDateTime updatedAt;

    protected CodeIssue() {
    }

    public CodeIssue(CodeRepository repository, String externalIssueId, String title, String status) {
        this.repository = repository;
        this.externalIssueId = externalIssueId;
        this.title = title;
        this.status = status;
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

    public boolean canCreateQuest() {
        return "OPEN".equalsIgnoreCase(status);
    }

    public void updateFromSync(String title, String status, String externalUrl) {
        if (title != null && !title.isBlank()) {
            this.title = title;
        }
        if (status != null && !status.isBlank()) {
            this.status = status;
        }
        this.externalUrl = externalUrl;
        this.syncedAt = OffsetDateTime.now();
    }

    public Long getIssueId() {
        return issueId;
    }

    public CodeRepository getRepository() {
        return repository;
    }

    public String getExternalIssueId() {
        return externalIssueId;
    }

    public String getTitle() {
        return title;
    }

    public String getStatus() {
        return status;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public void setIssueId(Long issueId) {
        this.issueId = issueId;
    }

    public void setExternalUrl(String externalUrl) {
        this.externalUrl = externalUrl;
    }

    public void setBody(String body) {
        this.body = body;
    }

    public void setSyncedAt(OffsetDateTime syncedAt) {
        this.syncedAt = syncedAt;
    }

    public String getExternalUrl() {
        return externalUrl;
    }

    public String getBody() {
        return body;
    }

    public OffsetDateTime getSyncedAt() {
        return syncedAt;
    }
}
