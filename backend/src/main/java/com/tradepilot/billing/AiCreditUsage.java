package com.tradepilot.billing;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "ai_credit_usage", indexes = {@Index(name = "idx_ai_credit_usage_user_id", columnList = "user_id"), @Index(name = "idx_ai_credit_usage_created_at", columnList = "created_at"), @Index(name = "idx_ai_credit_usage_user_created", columnList = "user_id, created_at"), @Index(name = "idx_ai_credit_usage_user_feature_created", columnList = "user_id, feature, created_at")})
public class AiCreditUsage {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @Column(name = "user_id", nullable = false)
    private Long userId;
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 40)
    private Feature feature;
    @Column(nullable = false)
    private Integer credits;
    @Column(name = "monthly_credits")
    private Integer monthlyCredits;
    @Column(name = "purchased_credits")
    private Integer purchasedCredits;
    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;
    @Column(name = "request_id")
    private Long requestId;
    @Column(columnDefinition = "TEXT")
    private String metadata;

    @PrePersist
    void onCreate() {
        if (createdAt == null) {
            createdAt = LocalDateTime.now();
        }
    }


    @java.lang.SuppressWarnings("all")
    public static class AiCreditUsageBuilder {
        @java.lang.SuppressWarnings("all")
        private Long id;
        @java.lang.SuppressWarnings("all")
        private Long userId;
        @java.lang.SuppressWarnings("all")
        private Feature feature;
        @java.lang.SuppressWarnings("all")
        private Integer credits;
        @java.lang.SuppressWarnings("all")
        private Integer monthlyCredits;
        @java.lang.SuppressWarnings("all")
        private Integer purchasedCredits;
        @java.lang.SuppressWarnings("all")
        private LocalDateTime createdAt;
        @java.lang.SuppressWarnings("all")
        private Long requestId;
        @java.lang.SuppressWarnings("all")
        private String metadata;

        @java.lang.SuppressWarnings("all")
        AiCreditUsageBuilder() {
        }

        /**
         * @return {@code this}.
         */
        @java.lang.SuppressWarnings("all")
        public AiCreditUsage.AiCreditUsageBuilder id(final Long id) {
            this.id = id;
            return this;
        }

        /**
         * @return {@code this}.
         */
        @java.lang.SuppressWarnings("all")
        public AiCreditUsage.AiCreditUsageBuilder userId(final Long userId) {
            this.userId = userId;
            return this;
        }

        /**
         * @return {@code this}.
         */
        @java.lang.SuppressWarnings("all")
        public AiCreditUsage.AiCreditUsageBuilder feature(final Feature feature) {
            this.feature = feature;
            return this;
        }

        /**
         * @return {@code this}.
         */
        @java.lang.SuppressWarnings("all")
        public AiCreditUsage.AiCreditUsageBuilder credits(final Integer credits) {
            this.credits = credits;
            return this;
        }

        /**
         * @return {@code this}.
         */
        @java.lang.SuppressWarnings("all")
        public AiCreditUsage.AiCreditUsageBuilder monthlyCredits(final Integer monthlyCredits) {
            this.monthlyCredits = monthlyCredits;
            return this;
        }

        /**
         * @return {@code this}.
         */
        @java.lang.SuppressWarnings("all")
        public AiCreditUsage.AiCreditUsageBuilder purchasedCredits(final Integer purchasedCredits) {
            this.purchasedCredits = purchasedCredits;
            return this;
        }

        /**
         * @return {@code this}.
         */
        @java.lang.SuppressWarnings("all")
        public AiCreditUsage.AiCreditUsageBuilder createdAt(final LocalDateTime createdAt) {
            this.createdAt = createdAt;
            return this;
        }

        /**
         * @return {@code this}.
         */
        @java.lang.SuppressWarnings("all")
        public AiCreditUsage.AiCreditUsageBuilder requestId(final Long requestId) {
            this.requestId = requestId;
            return this;
        }

        /**
         * @return {@code this}.
         */
        @java.lang.SuppressWarnings("all")
        public AiCreditUsage.AiCreditUsageBuilder metadata(final String metadata) {
            this.metadata = metadata;
            return this;
        }

        @java.lang.SuppressWarnings("all")
        public AiCreditUsage build() {
            return new AiCreditUsage(this.id, this.userId, this.feature, this.credits, this.monthlyCredits, this.purchasedCredits, this.createdAt, this.requestId, this.metadata);
        }

        @java.lang.Override
        @java.lang.SuppressWarnings("all")
        public java.lang.String toString() {
            return "AiCreditUsage.AiCreditUsageBuilder(id=" + this.id + ", userId=" + this.userId + ", feature=" + this.feature + ", credits=" + this.credits + ", monthlyCredits=" + this.monthlyCredits + ", purchasedCredits=" + this.purchasedCredits + ", createdAt=" + this.createdAt + ", requestId=" + this.requestId + ", metadata=" + this.metadata + ")";
        }
    }

    @java.lang.SuppressWarnings("all")
    public static AiCreditUsage.AiCreditUsageBuilder builder() {
        return new AiCreditUsage.AiCreditUsageBuilder();
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
    public Feature getFeature() {
        return this.feature;
    }

    @java.lang.SuppressWarnings("all")
    public Integer getCredits() {
        return this.credits;
    }

    @java.lang.SuppressWarnings("all")
    public Integer getMonthlyCredits() {
        return this.monthlyCredits;
    }

    @java.lang.SuppressWarnings("all")
    public Integer getPurchasedCredits() {
        return this.purchasedCredits;
    }

    @java.lang.SuppressWarnings("all")
    public LocalDateTime getCreatedAt() {
        return this.createdAt;
    }

    @java.lang.SuppressWarnings("all")
    public Long getRequestId() {
        return this.requestId;
    }

    @java.lang.SuppressWarnings("all")
    public String getMetadata() {
        return this.metadata;
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
    public void setFeature(final Feature feature) {
        this.feature = feature;
    }

    @java.lang.SuppressWarnings("all")
    public void setCredits(final Integer credits) {
        this.credits = credits;
    }

    @java.lang.SuppressWarnings("all")
    public void setMonthlyCredits(final Integer monthlyCredits) {
        this.monthlyCredits = monthlyCredits;
    }

    @java.lang.SuppressWarnings("all")
    public void setPurchasedCredits(final Integer purchasedCredits) {
        this.purchasedCredits = purchasedCredits;
    }

    @java.lang.SuppressWarnings("all")
    public void setCreatedAt(final LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    @java.lang.SuppressWarnings("all")
    public void setRequestId(final Long requestId) {
        this.requestId = requestId;
    }

    @java.lang.SuppressWarnings("all")
    public void setMetadata(final String metadata) {
        this.metadata = metadata;
    }

    @java.lang.SuppressWarnings("all")
    public AiCreditUsage() {
    }

    @java.lang.SuppressWarnings("all")
    public AiCreditUsage(final Long id, final Long userId, final Feature feature, final Integer credits, final Integer monthlyCredits, final Integer purchasedCredits, final LocalDateTime createdAt, final Long requestId, final String metadata) {
        this.id = id;
        this.userId = userId;
        this.feature = feature;
        this.credits = credits;
        this.monthlyCredits = monthlyCredits;
        this.purchasedCredits = purchasedCredits;
        this.createdAt = createdAt;
        this.requestId = requestId;
        this.metadata = metadata;
    }
}
