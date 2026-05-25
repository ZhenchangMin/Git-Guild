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
@Table(name = "pull_requests")
public class CodePullRequest {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "pull_request_id")
    private Long pullRequestId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "repository_id", nullable = false)
    private CodeRepository repository;

    @Column(name = "external_pr_id", nullable = false, length = 128)
    private String externalPrId;

    @Column(nullable = false, length = 200)
    private String title;

    @Column(name = "source_branch", nullable = false, length = 128)
    private String sourceBranch;

    @Column(name = "target_branch", nullable = false, length = 128)
    private String targetBranch;

    @Column(nullable = false, length = 32)
    private String status;

    @Column(name = "external_url", length = 512)
    private String externalUrl;

    @Column(name = "merged_at")
    private OffsetDateTime mergedAt;

    @Column(name = "created_at", nullable = false)
    private OffsetDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private OffsetDateTime updatedAt;

    protected CodePullRequest() {
    }

    public CodePullRequest(CodeRepository repository, String externalPrId, String title,
            String sourceBranch, String targetBranch, String status, String externalUrl) {
        this.repository = repository;
        this.externalPrId = externalPrId;
        this.title = title;
        this.sourceBranch = sourceBranch;
        this.targetBranch = targetBranch;
        this.status = status;
        this.externalUrl = externalUrl;
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

    public boolean isMerged() {
        return "MERGED".equals(status);
    }

    public Long getPullRequestId() {
        return pullRequestId;
    }

    public CodeRepository getRepository() {
        return repository;
    }

    public String getExternalPrId() {
        return externalPrId;
    }

    public String getTitle() {
        return title;
    }

    public String getSourceBranch() {
        return sourceBranch;
    }

    public String getTargetBranch() {
        return targetBranch;
    }

    public String getStatus() {
        return status;
    }

    public String getExternalUrl() {
        return externalUrl;
    }

    public OffsetDateTime getMergedAt() {
        return mergedAt;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public void setMergedAt(OffsetDateTime mergedAt) {
        this.mergedAt = mergedAt;
    }
}
