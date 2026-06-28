package com.tradepilot.billing;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "ai_usage_events", indexes = {@Index(name = "idx_ai_usage_events_user_id", columnList = "user_id"), @Index(name = "idx_ai_usage_events_user_type_created", columnList = "user_id, usage_type, created_at")})
public class AiUsageEvent {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @Column(name = "user_id", nullable = false)
    private Long userId;
    @Enumerated(EnumType.STRING)
    @Column(name = "usage_type", nullable = false, length = 40)
    private UsageType usageType;
    @Column(name = "related_resource_id")
    private Long relatedResourceId;
    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    @PrePersist
    void onCreate() {
        if (createdAt == null) {
            createdAt = LocalDateTime.now();
        }
    }


    @java.lang.SuppressWarnings("all")
    public static class AiUsageEventBuilder {
        @java.lang.SuppressWarnings("all")
        private Long id;
        @java.lang.SuppressWarnings("all")
        private Long userId;
        @java.lang.SuppressWarnings("all")
        private UsageType usageType;
        @java.lang.SuppressWarnings("all")
        private Long relatedResourceId;
        @java.lang.SuppressWarnings("all")
        private LocalDateTime createdAt;

        @java.lang.SuppressWarnings("all")
        AiUsageEventBuilder() {
        }

        /**
         * @return {@code this}.
         */
        @java.lang.SuppressWarnings("all")
        public AiUsageEvent.AiUsageEventBuilder id(final Long id) {
            this.id = id;
            return this;
        }

        /**
         * @return {@code this}.
         */
        @java.lang.SuppressWarnings("all")
        public AiUsageEvent.AiUsageEventBuilder userId(final Long userId) {
            this.userId = userId;
            return this;
        }

        /**
         * @return {@code this}.
         */
        @java.lang.SuppressWarnings("all")
        public AiUsageEvent.AiUsageEventBuilder usageType(final UsageType usageType) {
            this.usageType = usageType;
            return this;
        }

        /**
         * @return {@code this}.
         */
        @java.lang.SuppressWarnings("all")
        public AiUsageEvent.AiUsageEventBuilder relatedResourceId(final Long relatedResourceId) {
            this.relatedResourceId = relatedResourceId;
            return this;
        }

        /**
         * @return {@code this}.
         */
        @java.lang.SuppressWarnings("all")
        public AiUsageEvent.AiUsageEventBuilder createdAt(final LocalDateTime createdAt) {
            this.createdAt = createdAt;
            return this;
        }

        @java.lang.SuppressWarnings("all")
        public AiUsageEvent build() {
            return new AiUsageEvent(this.id, this.userId, this.usageType, this.relatedResourceId, this.createdAt);
        }

        @java.lang.Override
        @java.lang.SuppressWarnings("all")
        public java.lang.String toString() {
            return "AiUsageEvent.AiUsageEventBuilder(id=" + this.id + ", userId=" + this.userId + ", usageType=" + this.usageType + ", relatedResourceId=" + this.relatedResourceId + ", createdAt=" + this.createdAt + ")";
        }
    }

    @java.lang.SuppressWarnings("all")
    public static AiUsageEvent.AiUsageEventBuilder builder() {
        return new AiUsageEvent.AiUsageEventBuilder();
    }

    @java.lang.SuppressWarnings("all")
    public Long getId() {
        return this.id;
    }

    @java.lang.SuppressWarnings("all")
    public Long getUserId() {
        return this.userId;
    }

    @java.lang.SuppressWarnings("all")
    public UsageType getUsageType() {
        return this.usageType;
    }

    @java.lang.SuppressWarnings("all")
    public Long getRelatedResourceId() {
        return this.relatedResourceId;
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
    public void setUserId(final Long userId) {
        this.userId = userId;
    }

    @java.lang.SuppressWarnings("all")
    public void setUsageType(final UsageType usageType) {
        this.usageType = usageType;
    }

    @java.lang.SuppressWarnings("all")
    public void setRelatedResourceId(final Long relatedResourceId) {
        this.relatedResourceId = relatedResourceId;
    }

    @java.lang.SuppressWarnings("all")
    public void setCreatedAt(final LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    @java.lang.SuppressWarnings("all")
    public AiUsageEvent() {
    }

    @java.lang.SuppressWarnings("all")
    public AiUsageEvent(final Long id, final Long userId, final UsageType usageType, final Long relatedResourceId, final LocalDateTime createdAt) {
        this.id = id;
        this.userId = userId;
        this.usageType = usageType;
        this.relatedResourceId = relatedResourceId;
        this.createdAt = createdAt;
    }
}
