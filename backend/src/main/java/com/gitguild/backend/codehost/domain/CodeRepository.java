package com.gitguild.backend.codehost.domain;

import com.gitguild.backend.user.domain.User;
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
@Table(name = "repositories")
public class CodeRepository {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "repository_id")
    private Long repositoryId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "owner_id", nullable = false)
    private User owner;

    @Column(nullable = false, length = 128)
    private String name;

    @Column(name = "host_type", nullable = false, length = 32)
    private String hostType;

    @Column(name = "source_url", nullable = false, length = 512)
    private String sourceUrl;

    @Column(name = "external_repository_id", length = 128)
    private String externalRepositoryId;

    @Column(name = "default_branch", nullable = false, length = 128)
    private String defaultBranch = "main";

    @Column(length = 512)
    private String description;

    @Column(name = "sync_status", nullable = false, length = 32)
    private String syncStatus = "PENDING";

    @Column(name = "last_synced_at")
    private OffsetDateTime lastSyncedAt;

    @Column(name = "sync_error_message", length = 512)
    private String syncErrorMessage;

    @Column(name = "created_at", nullable = false)
    private OffsetDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private OffsetDateTime updatedAt;

    protected CodeRepository() {
    }

    public CodeRepository(User owner, String name, String hostType, String sourceUrl) {
        this.owner = owner;
        this.name = name;
        this.hostType = hostType;
        this.sourceUrl = sourceUrl;
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

    public boolean isAvailable() {
        return "SYNCED".equals(syncStatus) || "SUCCESS".equals(syncStatus);
    }

    public Long getRepositoryId() {
        return repositoryId;
    }

    public User getOwner() {
        return owner;
    }

    public String getName() {
        return name;
    }

    public String getHostType() {
        return hostType;
    }

    public String getSourceUrl() {
        return sourceUrl;
    }

    public String getSyncStatus() {
        return syncStatus;
    }

    public String getDefaultBranch() {
        return defaultBranch;
    }

    public String getDescription() {
        return description;
    }

    public String getExternalRepositoryId() {
        return externalRepositoryId;
    }

    public void setRepositoryId(Long repositoryId) {
        this.repositoryId = repositoryId;
    }

    public void setName(String name) {
        this.name = name;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public void setHostType(String hostType) {
        this.hostType = hostType;
    }

    public void setSourceUrl(String sourceUrl) {
        this.sourceUrl = sourceUrl;
    }

    public void setDefaultBranch(String defaultBranch) {
        this.defaultBranch = defaultBranch;
    }

    public void setExternalRepositoryId(String externalRepositoryId) {
        this.externalRepositoryId = externalRepositoryId;
    }

    public void setSyncStatus(String syncStatus) {
        this.syncStatus = syncStatus;
    }

    public void markSynced() {
        this.syncStatus = "SYNCED";
        this.syncErrorMessage = null;
        this.lastSyncedAt = OffsetDateTime.now();
    }

    /** 标记一次同步失败：保留上一次 {@code lastSyncedAt} 快照，仅落错误信息。 */
    public void markSyncFailed(String errorMessage) {
        this.syncStatus = "FAILED";
        if (errorMessage != null && errorMessage.length() > 512) {
            errorMessage = errorMessage.substring(0, 512);
        }
        this.syncErrorMessage = errorMessage;
    }

    public OffsetDateTime getLastSyncedAt() {
        return lastSyncedAt;
    }

    public String getSyncErrorMessage() {
        return syncErrorMessage;
    }
}
