package com.gitguild.backend.ops.domain;

import jakarta.persistence.CollectionTable;
import jakarta.persistence.Column;
import jakarta.persistence.ElementCollection;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.OrderColumn;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * 一条平台运维异常（表 {@code platform_exceptions}）——异常处理中心的真实数据。
 *
 * <p><b>语义：</b>记录平台侧自动检测到的失败事件（当前主要是仓库同步失败），供管理员在
 * 异常处理中心查看原因、影响、处置建议与日志，并执行重试 / 处置动作。
 *
 * <p><b>不变量：</b>{@code reason}/{@code impact}/{@code suggestion} 在创建那一刻固化为人类可读文案；
 * {@code logs} 只追加不回改，是异常从发生到处置的审计轨迹。
 *
 * <p><b>去重：</b>同一仓库若已存在未闭环（UNRESOLVED/IN_REVIEW）的同类异常，不再新建一行，
 * 而是向其日志追加新发生时间——避免同步连续失败把异常队列刷爆。详见 {@code ExceptionRecorder}。
 */
@Entity
@Table(name = "platform_exceptions")
public class PlatformException {

    public static final int MAX_TEXT_LENGTH = 1000;

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "exception_id")
    private Long exceptionId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 32)
    private ExceptionCategory category;

    /** 人类可读的异常类型标签，如「仓库同步失败」。 */
    @Column(nullable = false, length = 64)
    private String type;

    @Column(nullable = false, length = 255)
    private String title;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 32)
    private ExceptionStatus status;

    /** 关联仓库 id（SYNC 类必填，其它类可空）。轻引用，不建外键约束以免与仓库生命周期强耦合。 */
    @Column(name = "repository_id")
    private Long repositoryId;

    /** 仓库展示名快照（避免列表再去 join 仓库表）。 */
    @Column(name = "repository_name", length = 255)
    private String repositoryName;

    /** 关联任务编码快照，如 QST-0461（可空）。 */
    @Column(name = "related_quest", length = 64)
    private String relatedQuest;

    @Column(nullable = false, length = MAX_TEXT_LENGTH)
    private String reason;

    @Column(length = MAX_TEXT_LENGTH)
    private String impact;

    @Column(length = MAX_TEXT_LENGTH)
    private String suggestion;

    /** 是否支持「重试」动作（SYNC 类为 true）。 */
    @Column(nullable = false)
    private boolean retryable;

    /** 处置动作（resolve 时落库，如 MARK_RESOLVED / REQUEST_GRANT）。 */
    @Column(name = "resolution_action", length = 64)
    private String resolutionAction;

    @Column(name = "resolution_comment", length = MAX_TEXT_LENGTH)
    private String resolutionComment;

    /** 处置人 user id（轻引用）。 */
    @Column(name = "resolved_by")
    private Long resolvedBy;

    @Column(name = "detected_at", nullable = false)
    private OffsetDateTime detectedAt;

    @Column(name = "resolved_at")
    private OffsetDateTime resolvedAt;

    /** 异常日志（只追加），独立子表 {@code platform_exception_logs}，按 seq 保序。 */
    @ElementCollection
    @CollectionTable(
            name = "platform_exception_logs",
            joinColumns = @JoinColumn(name = "exception_id"))
    @OrderColumn(name = "seq")
    @Column(name = "line", length = MAX_TEXT_LENGTH, nullable = false)
    private List<String> logs = new ArrayList<>();

    protected PlatformException() {
    }

    public PlatformException(
            ExceptionCategory category,
            String type,
            String title,
            Long repositoryId,
            String repositoryName,
            String relatedQuest,
            String reason,
            String impact,
            String suggestion,
            boolean retryable) {
        this.category = category;
        this.type = type;
        this.title = title;
        this.repositoryId = repositoryId;
        this.repositoryName = repositoryName;
        this.relatedQuest = relatedQuest;
        this.reason = reason;
        this.impact = impact;
        this.suggestion = suggestion;
        this.retryable = retryable;
        this.status = ExceptionStatus.UNRESOLVED;
    }

    @PrePersist
    void prePersist() {
        if (this.detectedAt == null) {
            this.detectedAt = OffsetDateTime.now();
        }
        if (this.status == null) {
            this.status = ExceptionStatus.UNRESOLVED;
        }
    }

    /** 追加一条日志（自动加时间戳前缀），并刷新「最近发生时间」。 */
    public void appendLog(String line) {
        OffsetDateTime now = OffsetDateTime.now();
        this.logs.add(stamp(now) + " " + truncate(line));
        this.detectedAt = now;
    }

    /** 标记为「需复核」并记录处置动作/说明（动作未真正闭环时使用）。 */
    public void markInReview(Long adminId, String action, String comment) {
        this.status = ExceptionStatus.IN_REVIEW;
        this.resolvedBy = adminId;
        this.resolutionAction = action;
        this.resolutionComment = truncate(comment);
    }

    /** 标记为「已解决」并落处置信息。 */
    public void markResolved(Long adminId, String action, String comment) {
        this.status = ExceptionStatus.RESOLVED;
        this.resolvedBy = adminId;
        this.resolutionAction = action;
        this.resolutionComment = truncate(comment);
        this.resolvedAt = OffsetDateTime.now();
    }

    public boolean isOpen() {
        return status == ExceptionStatus.UNRESOLVED || status == ExceptionStatus.IN_REVIEW;
    }

    private static String stamp(OffsetDateTime time) {
        return String.format("%02d:%02d", time.getHour(), time.getMinute());
    }

    private static String truncate(String value) {
        if (value == null) {
            return null;
        }
        return value.length() > MAX_TEXT_LENGTH ? value.substring(0, MAX_TEXT_LENGTH) : value;
    }

    public Long getExceptionId() {
        return exceptionId;
    }

    public ExceptionCategory getCategory() {
        return category;
    }

    public String getType() {
        return type;
    }

    public String getTitle() {
        return title;
    }

    public ExceptionStatus getStatus() {
        return status;
    }

    public Long getRepositoryId() {
        return repositoryId;
    }

    public String getRepositoryName() {
        return repositoryName;
    }

    public String getRelatedQuest() {
        return relatedQuest;
    }

    public String getReason() {
        return reason;
    }

    public String getImpact() {
        return impact;
    }

    public String getSuggestion() {
        return suggestion;
    }

    public boolean isRetryable() {
        return retryable;
    }

    public String getResolutionAction() {
        return resolutionAction;
    }

    public String getResolutionComment() {
        return resolutionComment;
    }

    public Long getResolvedBy() {
        return resolvedBy;
    }

    public OffsetDateTime getDetectedAt() {
        return detectedAt;
    }

    public OffsetDateTime getResolvedAt() {
        return resolvedAt;
    }

    public List<String> getLogs() {
        return logs;
    }
}
