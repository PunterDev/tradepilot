package com.tradepilot.featureusage;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "feature_usage_events", indexes = {@Index(name = "idx_feature_usage_user_id", columnList = "user_id"), @Index(name = "idx_feature_usage_created_at", columnList = "created_at"), @Index(name = "idx_feature_usage_feature_created", columnList = "feature, created_at"), @Index(name = "idx_feature_usage_user_feature_created", columnList = "user_id, feature, created_at")})
public class FeatureUsageEvent {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @Column(name = "user_id", nullable = false)
    private Long userId;
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 50)
    private FeatureKey feature;
    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    @PrePersist
    void onCreate() {
        if (createdAt == null) {
            createdAt = LocalDateTime.now();
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
            return new FeatureUsageEvent(this.id, this.userId, this.feature, this.createdAt);
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
    public void setCreatedAt(final LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    @java.lang.SuppressWarnings("all")
    public FeatureUsageEvent() {
    }

    @java.lang.SuppressWarnings("all")
    public FeatureUsageEvent(final Long id, final Long userId, final FeatureKey feature, final LocalDateTime createdAt) {
        this.id = id;
        this.userId = userId;
        this.feature = feature;
        this.createdAt = createdAt;
    }
}
