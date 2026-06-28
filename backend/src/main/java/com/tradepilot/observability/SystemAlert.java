package com.tradepilot.observability;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "system_alerts", indexes = {@Index(name = "idx_system_alerts_resolved_created_at", columnList = "resolved, created_at"), @Index(name = "idx_system_alerts_type_created_at", columnList = "type, created_at"), @Index(name = "idx_system_alerts_status_created_at", columnList = "status, created_at"), @Index(name = "idx_system_alerts_severity_created_at", columnList = "severity, created_at")})
public class SystemAlert {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @Column(nullable = false, length = 80)
    private String type;
    @Column(nullable = false, length = 24)
    private String severity;
    @Column(nullable = false, length = 700)
    private String message;
    @Column(nullable = false)
    private Boolean resolved;
    @Column(nullable = false, length = 24)
    private String status;
    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;
    @Column(name = "resolved_at")
    private LocalDateTime resolvedAt;
    @Column(name = "resolved_by")
    private Long resolvedBy;

    @PrePersist
    public void onCreate() {
        if (resolved == null) {
            resolved = false;
        }
        if (status == null || status.isBlank()) {
            status = Boolean.TRUE.equals(resolved) ? "RESOLVED" : "OPEN";
        }
        if (createdAt == null) {
            createdAt = LocalDateTime.now();
        }
    }

    @java.lang.SuppressWarnings("all")
    private static Boolean $default$resolved() {
        return false;
    }

    @java.lang.SuppressWarnings("all")
    private static String $default$status() {
        return "OPEN";
    }


    @java.lang.SuppressWarnings("all")
    public static class SystemAlertBuilder {
        @java.lang.SuppressWarnings("all")
        private Long id;
        @java.lang.SuppressWarnings("all")
        private String type;
        @java.lang.SuppressWarnings("all")
        private String severity;
        @java.lang.SuppressWarnings("all")
        private String message;
        @java.lang.SuppressWarnings("all")
        private boolean resolved$set;
        @java.lang.SuppressWarnings("all")
        private Boolean resolved$value;
        @java.lang.SuppressWarnings("all")
        private boolean status$set;
        @java.lang.SuppressWarnings("all")
        private String status$value;
        @java.lang.SuppressWarnings("all")
        private LocalDateTime createdAt;
        @java.lang.SuppressWarnings("all")
        private LocalDateTime resolvedAt;
        @java.lang.SuppressWarnings("all")
        private Long resolvedBy;

        @java.lang.SuppressWarnings("all")
        SystemAlertBuilder() {
        }

        /**
         * @return {@code this}.
         */
        @java.lang.SuppressWarnings("all")
        public SystemAlert.SystemAlertBuilder id(final Long id) {
            this.id = id;
            return this;
        }

        /**
         * @return {@code this}.
         */
        @java.lang.SuppressWarnings("all")
        public SystemAlert.SystemAlertBuilder type(final String type) {
            this.type = type;
            return this;
        }

        /**
         * @return {@code this}.
         */
        @java.lang.SuppressWarnings("all")
        public SystemAlert.SystemAlertBuilder severity(final String severity) {
            this.severity = severity;
            return this;
        }

        /**
         * @return {@code this}.
         */
        @java.lang.SuppressWarnings("all")
        public SystemAlert.SystemAlertBuilder message(final String message) {
            this.message = message;
            return this;
        }

        /**
         * @return {@code this}.
         */
        @java.lang.SuppressWarnings("all")
        public SystemAlert.SystemAlertBuilder resolved(final Boolean resolved) {
            this.resolved$value = resolved;
            resolved$set = true;
            return this;
        }

        /**
         * @return {@code this}.
         */
        @java.lang.SuppressWarnings("all")
        public SystemAlert.SystemAlertBuilder status(final String status) {
            this.status$value = status;
            status$set = true;
            return this;
        }

        /**
         * @return {@code this}.
         */
        @java.lang.SuppressWarnings("all")
        public SystemAlert.SystemAlertBuilder createdAt(final LocalDateTime createdAt) {
            this.createdAt = createdAt;
            return this;
        }

        /**
         * @return {@code this}.
         */
        @java.lang.SuppressWarnings("all")
        public SystemAlert.SystemAlertBuilder resolvedAt(final LocalDateTime resolvedAt) {
            this.resolvedAt = resolvedAt;
            return this;
        }

        /**
         * @return {@code this}.
         */
        @java.lang.SuppressWarnings("all")
        public SystemAlert.SystemAlertBuilder resolvedBy(final Long resolvedBy) {
            this.resolvedBy = resolvedBy;
            return this;
        }

        @java.lang.SuppressWarnings("all")
        public SystemAlert build() {
            Boolean resolved$value = this.resolved$value;
            if (!this.resolved$set) resolved$value = SystemAlert.$default$resolved();
            String status$value = this.status$value;
            if (!this.status$set) status$value = SystemAlert.$default$status();
            return new SystemAlert(this.id, this.type, this.severity, this.message, resolved$value, status$value, this.createdAt, this.resolvedAt, this.resolvedBy);
        }

        @java.lang.Override
        @java.lang.SuppressWarnings("all")
        public java.lang.String toString() {
            return "SystemAlert.SystemAlertBuilder(id=" + this.id + ", type=" + this.type + ", severity=" + this.severity + ", message=" + this.message + ", resolved$value=" + this.resolved$value + ", status$value=" + this.status$value + ", createdAt=" + this.createdAt + ", resolvedAt=" + this.resolvedAt + ", resolvedBy=" + this.resolvedBy + ")";
        }
    }

    @java.lang.SuppressWarnings("all")
    public static SystemAlert.SystemAlertBuilder builder() {
        return new SystemAlert.SystemAlertBuilder();
    }

    @java.lang.SuppressWarnings("all")
    public Long getId() {
        return this.id;
    }

    @java.lang.SuppressWarnings("all")
    public String getType() {
        return this.type;
    }

    @java.lang.SuppressWarnings("all")
    public String getSeverity() {
        return this.severity;
    }

    @java.lang.SuppressWarnings("all")
    public String getMessage() {
        return this.message;
    }

    @java.lang.SuppressWarnings("all")
    public Boolean getResolved() {
        return this.resolved;
    }

    @java.lang.SuppressWarnings("all")
    public String getStatus() {
        return this.status;
    }

    @java.lang.SuppressWarnings("all")
    public LocalDateTime getCreatedAt() {
        return this.createdAt;
    }

    @java.lang.SuppressWarnings("all")
    public LocalDateTime getResolvedAt() {
        return this.resolvedAt;
    }

    @java.lang.SuppressWarnings("all")
    public Long getResolvedBy() {
        return this.resolvedBy;
    }

    @java.lang.SuppressWarnings("all")
    public void setId(final Long id) {
        this.id = id;
    }

    @java.lang.SuppressWarnings("all")
    public void setType(final String type) {
        this.type = type;
    }

    @java.lang.SuppressWarnings("all")
    public void setSeverity(final String severity) {
        this.severity = severity;
    }

    @java.lang.SuppressWarnings("all")
    public void setMessage(final String message) {
        this.message = message;
    }

    @java.lang.SuppressWarnings("all")
    public void setResolved(final Boolean resolved) {
        this.resolved = resolved;
    }

    @java.lang.SuppressWarnings("all")
    public void setStatus(final String status) {
        this.status = status;
    }

    @java.lang.SuppressWarnings("all")
    public void setCreatedAt(final LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    @java.lang.SuppressWarnings("all")
    public void setResolvedAt(final LocalDateTime resolvedAt) {
        this.resolvedAt = resolvedAt;
    }

    @java.lang.SuppressWarnings("all")
    public void setResolvedBy(final Long resolvedBy) {
        this.resolvedBy = resolvedBy;
    }

    @java.lang.SuppressWarnings("all")
    public SystemAlert() {
        this.resolved = SystemAlert.$default$resolved();
        this.status = SystemAlert.$default$status();
    }

    @java.lang.SuppressWarnings("all")
    public SystemAlert(final Long id, final String type, final String severity, final String message, final Boolean resolved, final String status, final LocalDateTime createdAt, final LocalDateTime resolvedAt, final Long resolvedBy) {
        this.id = id;
        this.type = type;
        this.severity = severity;
        this.message = message;
        this.resolved = resolved;
        this.status = status;
        this.createdAt = createdAt;
        this.resolvedAt = resolvedAt;
        this.resolvedBy = resolvedBy;
    }
}
