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
