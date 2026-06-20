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

/**
 * 从 Gitea 同步到本地数据库的 PR 元数据镜像。
 *
 * <p>Git-Guild 不持有 PR 内容，仅持有状态快照。{@code externalPrId} 是 Gitea PR 编号
 * 的字符串形式，与 {@code repository} 联合构成唯一键（DB 约束 uk_pr_repository_external）；
 * 创建后不可修改。
 *
 * <p><b>状态语义：</b>{@code status} 取值与 P4-005 接口契约中 "PR 状态" 一致，核心值为
 * {@code OPEN}、{@code MERGED}、{@code CLOSED}。{@link #isMerged()} 是唯一的业务谓词，
 * 其余状态判断应直接比对字符串常量。
 *
 * <p><b>同步模式：</b>外部调用应先通过
 * {@code CodePullRequestRepository.findByRepositoryRepositoryIdAndExternalPrId}
 * 查找已有记录再更新，禁止重复 INSERT（见已知问题清单 P4-016 问题5）。
 */
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

    public void setPullRequestId(Long pullRequestId) {
        this.pullRequestId = pullRequestId;
    }
}
