package com.tradepilot.entity;

import com.tradepilot.enums.AdminAuditAction;
import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "admin_audit_logs", indexes = {@Index(name = "idx_admin_audit_admin_user_id", columnList = "admin_user_id"), @Index(name = "idx_admin_audit_target_user_id", columnList = "target_user_id"), @Index(name = "idx_admin_audit_created_at", columnList = "created_at"), @Index(name = "idx_admin_audit_admin_created_at", columnList = "admin_user_id, created_at"), @Index(name = "idx_admin_audit_target_created_at", columnList = "target_user_id, created_at")})
public class AdminAuditLog {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @Column(name = "admin_user_id", nullable = false)
    private Long adminUserId;
    @Column(name = "target_user_id", nullable = false)
    private Long targetUserId;
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 40)
    private AdminAuditAction action;
    @Column(length = 500)
    private String reason;
    @Column(name = "before_value", columnDefinition = "TEXT")
    private String beforeValue;
    @Column(name = "after_value", columnDefinition = "TEXT")
    private String afterValue;
    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    @PrePersist
    void onCreate() {
        if (createdAt == null) {
            createdAt = LocalDateTime.now();
        }
    }


    @java.lang.SuppressWarnings("all")
    public static class AdminAuditLogBuilder {
        @java.lang.SuppressWarnings("all")
        private Long id;
        @java.lang.SuppressWarnings("all")
        private Long adminUserId;
        @java.lang.SuppressWarnings("all")
        private Long targetUserId;
        @java.lang.SuppressWarnings("all")
        private AdminAuditAction action;
        @java.lang.SuppressWarnings("all")
        private String reason;
        @java.lang.SuppressWarnings("all")
        private String beforeValue;
        @java.lang.SuppressWarnings("all")
        private String afterValue;
        @java.lang.SuppressWarnings("all")
        private LocalDateTime createdAt;

        @java.lang.SuppressWarnings("all")
        AdminAuditLogBuilder() {
        }

        /**
         * @return {@code this}.
         */
        @java.lang.SuppressWarnings("all")
        public AdminAuditLog.AdminAuditLogBuilder id(final Long id) {
            this.id = id;
            return this;
        }

        /**
         * @return {@code this}.
         */
        @java.lang.SuppressWarnings("all")
        public AdminAuditLog.AdminAuditLogBuilder adminUserId(final Long adminUserId) {
            this.adminUserId = adminUserId;
            return this;
        }

        /**
         * @return {@code this}.
         */
        @java.lang.SuppressWarnings("all")
        public AdminAuditLog.AdminAuditLogBuilder targetUserId(final Long targetUserId) {
            this.targetUserId = targetUserId;
            return this;
        }

        /**
         * @return {@code this}.
         */
        @java.lang.SuppressWarnings("all")
        public AdminAuditLog.AdminAuditLogBuilder action(final AdminAuditAction action) {
            this.action = action;
            return this;
        }

        /**
         * @return {@code this}.
         */
        @java.lang.SuppressWarnings("all")
        public AdminAuditLog.AdminAuditLogBuilder reason(final String reason) {
            this.reason = reason;
            return this;
        }

        /**
         * @return {@code this}.
         */
        @java.lang.SuppressWarnings("all")
        public AdminAuditLog.AdminAuditLogBuilder beforeValue(final String beforeValue) {
            this.beforeValue = beforeValue;
            return this;
        }

        /**
         * @return {@code this}.
         */
        @java.lang.SuppressWarnings("all")
        public AdminAuditLog.AdminAuditLogBuilder afterValue(final String afterValue) {
            this.afterValue = afterValue;
            return this;
        }

        /**
         * @return {@code this}.
         */
        @java.lang.SuppressWarnings("all")
        public AdminAuditLog.AdminAuditLogBuilder createdAt(final LocalDateTime createdAt) {
            this.createdAt = createdAt;
            return this;
        }

        @java.lang.SuppressWarnings("all")
        public AdminAuditLog build() {
            return new AdminAuditLog(this.id, this.adminUserId, this.targetUserId, this.action, this.reason, this.beforeValue, this.afterValue, this.createdAt);
        }

        @java.lang.Override
        @java.lang.SuppressWarnings("all")
        public java.lang.String toString() {
            return "AdminAuditLog.AdminAuditLogBuilder(id=" + this.id + ", adminUserId=" + this.adminUserId + ", targetUserId=" + this.targetUserId + ", action=" + this.action + ", reason=" + this.reason + ", beforeValue=" + this.beforeValue + ", afterValue=" + this.afterValue + ", createdAt=" + this.createdAt + ")";
        }
    }

    @java.lang.SuppressWarnings("all")
    public static AdminAuditLog.AdminAuditLogBuilder builder() {
        return new AdminAuditLog.AdminAuditLogBuilder();
    }

    @java.lang.SuppressWarnings("all")
    public Long getId() {
        return this.id;
    }

    @java.lang.SuppressWarnings("all")
    public Long getAdminUserId() {
        return this.adminUserId;
    }

    @java.lang.SuppressWarnings("all")
    public Long getTargetUserId() {
        return this.targetUserId;
    }

    @java.lang.SuppressWarnings("all")
    public AdminAuditAction getAction() {
        return this.action;
    }

    @java.lang.SuppressWarnings("all")
    public String getReason() {
        return this.reason;
    }

    @java.lang.SuppressWarnings("all")
    public String getBeforeValue() {
        return this.beforeValue;
    }

    @java.lang.SuppressWarnings("all")
    public String getAfterValue() {
        return this.afterValue;
    }

    @java.lang.SuppressWarnings("all")
    public LocalDateTime getCreatedAt() {
        return this.createdAt;
    }

    @java.lang.SuppressWarnings("all")
    public void setId(final Long id) {
        this.id = id;
    }

    @java.lang.SuppressWarnings("all")
    public void setAdminUserId(final Long adminUserId) {
        this.adminUserId = adminUserId;
    }

    @java.lang.SuppressWarnings("all")
    public void setTargetUserId(final Long targetUserId) {
        this.targetUserId = targetUserId;
    }

    @java.lang.SuppressWarnings("all")
    public void setAction(final AdminAuditAction action) {
        this.action = action;
    }

    @java.lang.SuppressWarnings("all")
    public void setReason(final String reason) {
        this.reason = reason;
    }

    @java.lang.SuppressWarnings("all")
    public void setBeforeValue(final String beforeValue) {
        this.beforeValue = beforeValue;
    }

    @java.lang.SuppressWarnings("all")
    public void setAfterValue(final String afterValue) {
        this.afterValue = afterValue;
    }

    @java.lang.SuppressWarnings("all")
    public void setCreatedAt(final LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    @java.lang.SuppressWarnings("all")
    public AdminAuditLog() {
    }

    @java.lang.SuppressWarnings("all")
    public AdminAuditLog(final Long id, final Long adminUserId, final Long targetUserId, final AdminAuditAction action, final String reason, final String beforeValue, final String afterValue, final LocalDateTime createdAt) {
        this.id = id;
        this.adminUserId = adminUserId;
        this.targetUserId = targetUserId;
        this.action = action;
        this.reason = reason;
        this.beforeValue = beforeValue;
        this.afterValue = afterValue;
        this.createdAt = createdAt;
    }
}
