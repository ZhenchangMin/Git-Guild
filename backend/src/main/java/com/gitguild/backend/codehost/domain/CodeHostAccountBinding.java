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

/**
 * Adventurer 的 Git-Guild 账号与 Gitea 账号之间的绑定关系。
 *
 * <p>每条记录存储一个 Git-Guild 用户（{@code user_id}）对应的外部代码托管账号标识
 * （{@code externalAccountId}、{@code externalUsername}）。当前仅支持 Gitea
 * （{@code hostType = "GITEA"}），架构预留了多代码托管平台扩展能力。
 *
 * <p><b>核心业务用途：</b>在 Adventurer 提交 Submission 时，通过比对
 * {@code externalUsername} 与 PR 作者登录名，校验"提交的 PR 确实属于该 Adventurer"。
 *
 * <p><b>不变量：</b>PR 归属校验必须只使用 {@code status = "ACTIVE"} 的绑定记录；
 * 已失效绑定（{@code status = "INACTIVE"}）不得通过校验。查询时应使用
 * {@code findByUserUserIdAndHostTypeAndStatus(userId, hostType, "ACTIVE")}。
 */
@Entity
@Table(name = "code_host_account_bindings")
public class CodeHostAccountBinding {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "binding_id")
    private Long bindingId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Column(name = "host_type", nullable = false, length = 32)
    private String hostType;

    @Column(name = "external_account_id", nullable = false, length = 128)
    private String externalAccountId;

    @Column(name = "external_username", nullable = false, length = 128)
    private String externalUsername;

    @Column(nullable = false, length = 32)
    private String status = "ACTIVE";

    @Column(name = "created_at", nullable = false)
    private OffsetDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private OffsetDateTime updatedAt;

    protected CodeHostAccountBinding() {
    }

    public CodeHostAccountBinding(User user, String hostType, String externalAccountId, String externalUsername) {
        this.user = user;
        this.hostType = hostType;
        this.externalAccountId = externalAccountId;
        this.externalUsername = externalUsername;
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

    public Long getBindingId() {
        return bindingId;
    }

    public User getUser() {
        return user;
    }

    public String getHostType() {
        return hostType;
    }

    public String getExternalAccountId() {
        return externalAccountId;
    }

    public String getExternalUsername() {
        return externalUsername;
    }

    public String getStatus() {
        return status;
    }
}
