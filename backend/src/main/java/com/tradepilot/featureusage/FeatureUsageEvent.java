package com.tradepilot.featureusage;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "feature_usage_events", indexes = {@Index(name = "idx_feature_usage_user_id", columnList = "user_id"), @Index(name = "idx_feature_usage_created_at", columnList = "created_at"), @Index(name = "idx_feature_usage_feature_created", columnList = "feature, created_at"), @Index(name = "idx_feature_usage_user_feature_created", columnList = "user_id, feature, created_at"), @Index(name = "idx_feature_usage_feature_action_created", columnList = "feature, action, created_at"), @Index(name = "idx_feature_usage_user_feature_action_created", columnList = "user_id, feature, action, created_at")})
public class FeatureUsageEvent {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @Column(name = "user_id", nullable = false)
    private Long userId;
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 50)
    private FeatureKey feature;
    @Enumerated(EnumType.STRING)
    @Column(name = "action", length = 80)
    private FeatureUsageAction action;
    @Enumerated(EnumType.STRING)
    @Column(name = "event_type", length = 40)
    private FeatureUsageEventType eventType;
    @Column(name = "account_id")
    private Long accountId;
    @Column(name = "session_id", length = 120)
    private String sessionId;
    @Column(name = "entity_type", length = 80)
    private String entityType;
    @Column(name = "entity_id", length = 120)
    private String entityId;
    @Column(name = "source", length = 80)
    private String source;
    @Column(name = "plan_at_event", length = 20)
    private String planAtEvent;
    @Column(name = "metadata_json", columnDefinition = "text")
    private String metadataJson;
    @Column(name = "occurred_at")
    private LocalDateTime occurredAt;
    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    @PrePersist
    void onCreate() {
        if (createdAt == null) {
            createdAt = LocalDateTime.now();
        }
        if (occurredAt == null) {
            occurredAt = createdAt;
        }
    }


    @java.lang.SuppressWarnings("all")
    public static class FeatureUsageEventBuilder {
        @java.lang.SuppressWarnings("all")
        private Long id;
        @java.lang.SuppressWarnings("all")
        private Long userId;
        @java.lang.SuppressWarnings("all")
        private FeatureKey feature;
        @java.lang.SuppressWarnings("all")
        private FeatureUsageAction action;
        @java.lang.SuppressWarnings("all")
        private FeatureUsageEventType eventType;
        @java.lang.SuppressWarnings("all")
        private Long accountId;
        @java.lang.SuppressWarnings("all")
        private String sessionId;
        @java.lang.SuppressWarnings("all")
        private String entityType;
        @java.lang.SuppressWarnings("all")
        private String entityId;
        @java.lang.SuppressWarnings("all")
        private String source;
        @java.lang.SuppressWarnings("all")
        private String planAtEvent;
        @java.lang.SuppressWarnings("all")
        private String metadataJson;
        @java.lang.SuppressWarnings("all")
        private LocalDateTime occurredAt;
        @java.lang.SuppressWarnings("all")
        private LocalDateTime createdAt;

        @java.lang.SuppressWarnings("all")
        FeatureUsageEventBuilder() {
        }

        /**
         * @return {@code this}.
         */
        @java.lang.SuppressWarnings("all")
        public FeatureUsageEvent.FeatureUsageEventBuilder id(final Long id) {
            this.id = id;
            return this;
        }

        /**
         * @return {@code this}.
         */
        @java.lang.SuppressWarnings("all")
        public FeatureUsageEvent.FeatureUsageEventBuilder userId(final Long userId) {
            this.userId = userId;
            return this;
        }

        /**
         * @return {@code this}.
         */
        @java.lang.SuppressWarnings("all")
        public FeatureUsageEvent.FeatureUsageEventBuilder feature(final FeatureKey feature) {
            this.feature = feature;
            return this;
        }

        @java.lang.SuppressWarnings("all")
        public FeatureUsageEvent.FeatureUsageEventBuilder action(final FeatureUsageAction action) {
            this.action = action;
            return this;
        }

        @java.lang.SuppressWarnings("all")
        public FeatureUsageEvent.FeatureUsageEventBuilder eventType(final FeatureUsageEventType eventType) {
            this.eventType = eventType;
            return this;
        }

        @java.lang.SuppressWarnings("all")
        public FeatureUsageEvent.FeatureUsageEventBuilder accountId(final Long accountId) {
            this.accountId = accountId;
            return this;
        }

        @java.lang.SuppressWarnings("all")
        public FeatureUsageEvent.FeatureUsageEventBuilder sessionId(final String sessionId) {
            this.sessionId = sessionId;
            return this;
        }

        @java.lang.SuppressWarnings("all")
        public FeatureUsageEvent.FeatureUsageEventBuilder entityType(final String entityType) {
            this.entityType = entityType;
            return this;
        }

        @java.lang.SuppressWarnings("all")
        public FeatureUsageEvent.FeatureUsageEventBuilder entityId(final String entityId) {
            this.entityId = entityId;
            return this;
        }

        @java.lang.SuppressWarnings("all")
        public FeatureUsageEvent.FeatureUsageEventBuilder source(final String source) {
            this.source = source;
            return this;
        }

        @java.lang.SuppressWarnings("all")
        public FeatureUsageEvent.FeatureUsageEventBuilder planAtEvent(final String planAtEvent) {
            this.planAtEvent = planAtEvent;
            return this;
        }

        @java.lang.SuppressWarnings("all")
        public FeatureUsageEvent.FeatureUsageEventBuilder metadataJson(final String metadataJson) {
            this.metadataJson = metadataJson;
            return this;
        }

        @java.lang.SuppressWarnings("all")
        public FeatureUsageEvent.FeatureUsageEventBuilder occurredAt(final LocalDateTime occurredAt) {
            this.occurredAt = occurredAt;
            return this;
        }

        /**
         * @return {@code this}.
         */
        @java.lang.SuppressWarnings("all")
        public FeatureUsageEvent.FeatureUsageEventBuilder createdAt(final LocalDateTime createdAt) {
            this.createdAt = createdAt;
            return this;
        }

        @java.lang.SuppressWarnings("all")
        public FeatureUsageEvent build() {
            return new FeatureUsageEvent(this.id, this.userId, this.feature, this.action, this.eventType, this.accountId, this.sessionId, this.entityType, this.entityId, this.source, this.planAtEvent, this.metadataJson, this.occurredAt, this.createdAt);
        }

        @java.lang.Override
        @java.lang.SuppressWarnings("all")
        public java.lang.String toString() {
            return "FeatureUsageEvent.FeatureUsageEventBuilder(id=" + this.id + ", userId=" + this.userId + ", feature=" + this.feature + ", createdAt=" + this.createdAt + ")";
        }
    }

    @java.lang.SuppressWarnings("all")
    public static FeatureUsageEvent.FeatureUsageEventBuilder builder() {
        return new FeatureUsageEvent.FeatureUsageEventBuilder();
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
        public FeatureKey getFeature() {
            return this.feature;
        }

    @java.lang.SuppressWarnings("all")
    public FeatureUsageAction getAction() {
        return this.action;
    }

    @java.lang.SuppressWarnings("all")
    public FeatureUsageEventType getEventType() {
        return this.eventType;
    }

    @java.lang.SuppressWarnings("all")
    public Long getAccountId() {
        return this.accountId;
    }

    @java.lang.SuppressWarnings("all")
    public String getSessionId() {
        return this.sessionId;
    }

    @java.lang.SuppressWarnings("all")
    public String getEntityType() {
        return this.entityType;
    }

    @java.lang.SuppressWarnings("all")
    public String getEntityId() {
        return this.entityId;
    }

    @java.lang.SuppressWarnings("all")
    public String getSource() {
        return this.source;
    }

    @java.lang.SuppressWarnings("all")
    public String getPlanAtEvent() {
        return this.planAtEvent;
    }

    @java.lang.SuppressWarnings("all")
    public String getMetadataJson() {
        return this.metadataJson;
    }

    @java.lang.SuppressWarnings("all")
    public LocalDateTime getOccurredAt() {
        return this.occurredAt;
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
    public void setFeature(final FeatureKey feature) {
        this.feature = feature;
    }

    @java.lang.SuppressWarnings("all")
    public void setAction(final FeatureUsageAction action) {
        this.action = action;
    }

    @java.lang.SuppressWarnings("all")
    public void setEventType(final FeatureUsageEventType eventType) {
        this.eventType = eventType;
    }

    @java.lang.SuppressWarnings("all")
    public void setAccountId(final Long accountId) {
        this.accountId = accountId;
    }

    @java.lang.SuppressWarnings("all")
    public void setSessionId(final String sessionId) {
        this.sessionId = sessionId;
    }

    @java.lang.SuppressWarnings("all")
    public void setEntityType(final String entityType) {
        this.entityType = entityType;
    }

    @java.lang.SuppressWarnings("all")
    public void setEntityId(final String entityId) {
        this.entityId = entityId;
    }

    @java.lang.SuppressWarnings("all")
    public void setSource(final String source) {
        this.source = source;
    }

    @java.lang.SuppressWarnings("all")
    public void setPlanAtEvent(final String planAtEvent) {
        this.planAtEvent = planAtEvent;
    }

    @java.lang.SuppressWarnings("all")
    public void setMetadataJson(final String metadataJson) {
        this.metadataJson = metadataJson;
    }

    @java.lang.SuppressWarnings("all")
    public void setOccurredAt(final LocalDateTime occurredAt) {
        this.occurredAt = occurredAt;
    }

    @java.lang.SuppressWarnings("all")
    public void setCreatedAt(final LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    @java.lang.SuppressWarnings("all")
    public FeatureUsageEvent() {
    }

    @java.lang.SuppressWarnings("all")
    public FeatureUsageEvent(final Long id, final Long userId, final FeatureKey feature, final LocalDateTime createdAt) {
        this(id, userId, feature, null, null, null, null, null, null, null, null, null, createdAt, createdAt);
    }

    @java.lang.SuppressWarnings("all")
    public FeatureUsageEvent(final Long id, final Long userId, final FeatureKey feature, final FeatureUsageAction action, final FeatureUsageEventType eventType, final Long accountId, final String sessionId, final String entityType, final String entityId, final String source, final String planAtEvent, final String metadataJson, final LocalDateTime occurredAt, final LocalDateTime createdAt) {
        this.id = id;
        this.userId = userId;
        this.feature = feature;
        this.action = action;
        this.eventType = eventType;
        this.accountId = accountId;
        this.sessionId = sessionId;
        this.entityType = entityType;
        this.entityId = entityId;
        this.source = source;
        this.planAtEvent = planAtEvent;
        this.metadataJson = metadataJson;
        this.occurredAt = occurredAt;
        this.createdAt = createdAt;
    }
}
