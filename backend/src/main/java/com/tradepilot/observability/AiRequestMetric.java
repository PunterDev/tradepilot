package com.tradepilot.observability;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "ai_request_metrics", indexes = {@Index(name = "idx_ai_request_metrics_created_at", columnList = "created_at"), @Index(name = "idx_ai_request_metrics_user_id_created_at", columnList = "user_id, created_at"), @Index(name = "idx_ai_request_metrics_feature_created_at", columnList = "feature, created_at")})
public class AiRequestMetric {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @Column(name = "user_id")
    private Long userId;
    @Column(nullable = false, length = 64)
    private String feature;
    @Column(nullable = false, length = 80)
    private String model;
    @Column(name = "prompt_tokens")
    private Integer promptTokens;
    @Column(name = "completion_tokens")
    private Integer completionTokens;
    @Column(name = "total_tokens")
    private Integer totalTokens;
    @Column(name = "estimated_cost", precision = 12, scale = 6)
    private BigDecimal estimatedCost;
    @Column(name = "duration_ms", nullable = false)
    private Long durationMs;
    @Column(nullable = false)
    private Boolean success;
    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;


    @java.lang.SuppressWarnings("all")
    public static class AiRequestMetricBuilder {
        @java.lang.SuppressWarnings("all")
        private Long id;
        @java.lang.SuppressWarnings("all")
        private Long userId;
        @java.lang.SuppressWarnings("all")
        private String feature;
        @java.lang.SuppressWarnings("all")
        private String model;
        @java.lang.SuppressWarnings("all")
        private Integer promptTokens;
        @java.lang.SuppressWarnings("all")
        private Integer completionTokens;
        @java.lang.SuppressWarnings("all")
        private Integer totalTokens;
        @java.lang.SuppressWarnings("all")
        private BigDecimal estimatedCost;
        @java.lang.SuppressWarnings("all")
        private Long durationMs;
        @java.lang.SuppressWarnings("all")
        private Boolean success;
        @java.lang.SuppressWarnings("all")
        private LocalDateTime createdAt;

        @java.lang.SuppressWarnings("all")
        AiRequestMetricBuilder() {
        }

        /**
         * @return {@code this}.
         */
        @java.lang.SuppressWarnings("all")
        public AiRequestMetric.AiRequestMetricBuilder id(final Long id) {
            this.id = id;
            return this;
        }

        /**
         * @return {@code this}.
         */
        @java.lang.SuppressWarnings("all")
        public AiRequestMetric.AiRequestMetricBuilder userId(final Long userId) {
            this.userId = userId;
            return this;
        }

        /**
         * @return {@code this}.
         */
        @java.lang.SuppressWarnings("all")
        public AiRequestMetric.AiRequestMetricBuilder feature(final String feature) {
            this.feature = feature;
            return this;
        }

        /**
         * @return {@code this}.
         */
        @java.lang.SuppressWarnings("all")
        public AiRequestMetric.AiRequestMetricBuilder model(final String model) {
            this.model = model;
            return this;
        }

        /**
         * @return {@code this}.
         */
        @java.lang.SuppressWarnings("all")
        public AiRequestMetric.AiRequestMetricBuilder promptTokens(final Integer promptTokens) {
            this.promptTokens = promptTokens;
            return this;
        }

        /**
         * @return {@code this}.
         */
        @java.lang.SuppressWarnings("all")
        public AiRequestMetric.AiRequestMetricBuilder completionTokens(final Integer completionTokens) {
            this.completionTokens = completionTokens;
            return this;
        }

        /**
         * @return {@code this}.
         */
        @java.lang.SuppressWarnings("all")
        public AiRequestMetric.AiRequestMetricBuilder totalTokens(final Integer totalTokens) {
            this.totalTokens = totalTokens;
            return this;
        }

        /**
         * @return {@code this}.
         */
        @java.lang.SuppressWarnings("all")
        public AiRequestMetric.AiRequestMetricBuilder estimatedCost(final BigDecimal estimatedCost) {
            this.estimatedCost = estimatedCost;
            return this;
        }

        /**
         * @return {@code this}.
         */
        @java.lang.SuppressWarnings("all")
        public AiRequestMetric.AiRequestMetricBuilder durationMs(final Long durationMs) {
            this.durationMs = durationMs;
            return this;
        }

        /**
         * @return {@code this}.
         */
        @java.lang.SuppressWarnings("all")
        public AiRequestMetric.AiRequestMetricBuilder success(final Boolean success) {
            this.success = success;
            return this;
        }

        /**
         * @return {@code this}.
         */
        @java.lang.SuppressWarnings("all")
        public AiRequestMetric.AiRequestMetricBuilder createdAt(final LocalDateTime createdAt) {
            this.createdAt = createdAt;
            return this;
        }

        @java.lang.SuppressWarnings("all")
        public AiRequestMetric build() {
            return new AiRequestMetric(this.id, this.userId, this.feature, this.model, this.promptTokens, this.completionTokens, this.totalTokens, this.estimatedCost, this.durationMs, this.success, this.createdAt);
        }

        @java.lang.Override
        @java.lang.SuppressWarnings("all")
        public java.lang.String toString() {
            return "AiRequestMetric.AiRequestMetricBuilder(id=" + this.id + ", userId=" + this.userId + ", feature=" + this.feature + ", model=" + this.model + ", promptTokens=" + this.promptTokens + ", completionTokens=" + this.completionTokens + ", totalTokens=" + this.totalTokens + ", estimatedCost=" + this.estimatedCost + ", durationMs=" + this.durationMs + ", success=" + this.success + ", createdAt=" + this.createdAt + ")";
        }
    }

    @java.lang.SuppressWarnings("all")
    public static AiRequestMetric.AiRequestMetricBuilder builder() {
        return new AiRequestMetric.AiRequestMetricBuilder();
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
    public String getFeature() {
        return this.feature;
    }

    @java.lang.SuppressWarnings("all")
    public String getModel() {
        return this.model;
    }

    @java.lang.SuppressWarnings("all")
    public Integer getPromptTokens() {
        return this.promptTokens;
    }

    @java.lang.SuppressWarnings("all")
    public Integer getCompletionTokens() {
        return this.completionTokens;
    }

    @java.lang.SuppressWarnings("all")
    public Integer getTotalTokens() {
        return this.totalTokens;
    }

    @java.lang.SuppressWarnings("all")
    public BigDecimal getEstimatedCost() {
        return this.estimatedCost;
    }

    @java.lang.SuppressWarnings("all")
    public Long getDurationMs() {
        return this.durationMs;
    }

    @java.lang.SuppressWarnings("all")
    public Boolean getSuccess() {
        return this.success;
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
    public void setFeature(final String feature) {
        this.feature = feature;
    }

    @java.lang.SuppressWarnings("all")
    public void setModel(final String model) {
        this.model = model;
    }

    @java.lang.SuppressWarnings("all")
    public void setPromptTokens(final Integer promptTokens) {
        this.promptTokens = promptTokens;
    }

    @java.lang.SuppressWarnings("all")
    public void setCompletionTokens(final Integer completionTokens) {
        this.completionTokens = completionTokens;
    }

    @java.lang.SuppressWarnings("all")
    public void setTotalTokens(final Integer totalTokens) {
        this.totalTokens = totalTokens;
    }

    @java.lang.SuppressWarnings("all")
    public void setEstimatedCost(final BigDecimal estimatedCost) {
        this.estimatedCost = estimatedCost;
    }

    @java.lang.SuppressWarnings("all")
    public void setDurationMs(final Long durationMs) {
        this.durationMs = durationMs;
    }

    @java.lang.SuppressWarnings("all")
    public void setSuccess(final Boolean success) {
        this.success = success;
    }

    @java.lang.SuppressWarnings("all")
    public void setCreatedAt(final LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    @java.lang.SuppressWarnings("all")
    public AiRequestMetric() {
    }

    @java.lang.SuppressWarnings("all")
    public AiRequestMetric(final Long id, final Long userId, final String feature, final String model, final Integer promptTokens, final Integer completionTokens, final Integer totalTokens, final BigDecimal estimatedCost, final Long durationMs, final Boolean success, final LocalDateTime createdAt) {
        this.id = id;
        this.userId = userId;
        this.feature = feature;
        this.model = model;
        this.promptTokens = promptTokens;
        this.completionTokens = completionTokens;
        this.totalTokens = totalTokens;
        this.estimatedCost = estimatedCost;
        this.durationMs = durationMs;
        this.success = success;
        this.createdAt = createdAt;
    }
}
