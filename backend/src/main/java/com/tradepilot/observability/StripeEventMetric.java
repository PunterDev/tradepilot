package com.tradepilot.observability;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "stripe_events", indexes = {@Index(name = "idx_stripe_events_created_at", columnList = "created_at"), @Index(name = "idx_stripe_events_event_type_created_at", columnList = "event_type, created_at"), @Index(name = "idx_stripe_events_success_created_at", columnList = "success, created_at")})
public class StripeEventMetric {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @Column(name = "event_type", nullable = false, length = 120)
    private String eventType;
    @Column(nullable = false)
    private Boolean success;
    @Column(name = "duration_ms", nullable = false)
    private Long durationMs;
    @Column(name = "error_message", length = 700)
    private String errorMessage;
    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;


    @java.lang.SuppressWarnings("all")
    public static class StripeEventMetricBuilder {
        @java.lang.SuppressWarnings("all")
        private Long id;
        @java.lang.SuppressWarnings("all")
        private String eventType;
        @java.lang.SuppressWarnings("all")
        private Boolean success;
        @java.lang.SuppressWarnings("all")
        private Long durationMs;
        @java.lang.SuppressWarnings("all")
        private String errorMessage;
        @java.lang.SuppressWarnings("all")
        private LocalDateTime createdAt;

        @java.lang.SuppressWarnings("all")
        StripeEventMetricBuilder() {
        }

        /**
         * @return {@code this}.
         */
        @java.lang.SuppressWarnings("all")
        public StripeEventMetric.StripeEventMetricBuilder id(final Long id) {
            this.id = id;
            return this;
        }

        /**
         * @return {@code this}.
         */
        @java.lang.SuppressWarnings("all")
        public StripeEventMetric.StripeEventMetricBuilder eventType(final String eventType) {
            this.eventType = eventType;
            return this;
        }

        /**
         * @return {@code this}.
         */
        @java.lang.SuppressWarnings("all")
        public StripeEventMetric.StripeEventMetricBuilder success(final Boolean success) {
            this.success = success;
            return this;
        }

        /**
         * @return {@code this}.
         */
        @java.lang.SuppressWarnings("all")
        public StripeEventMetric.StripeEventMetricBuilder durationMs(final Long durationMs) {
            this.durationMs = durationMs;
            return this;
        }

        /**
         * @return {@code this}.
         */
        @java.lang.SuppressWarnings("all")
        public StripeEventMetric.StripeEventMetricBuilder errorMessage(final String errorMessage) {
            this.errorMessage = errorMessage;
            return this;
        }

        /**
         * @return {@code this}.
         */
        @java.lang.SuppressWarnings("all")
        public StripeEventMetric.StripeEventMetricBuilder createdAt(final LocalDateTime createdAt) {
            this.createdAt = createdAt;
            return this;
        }

        @java.lang.SuppressWarnings("all")
        public StripeEventMetric build() {
            return new StripeEventMetric(this.id, this.eventType, this.success, this.durationMs, this.errorMessage, this.createdAt);
        }

        @java.lang.Override
        @java.lang.SuppressWarnings("all")
        public java.lang.String toString() {
            return "StripeEventMetric.StripeEventMetricBuilder(id=" + this.id + ", eventType=" + this.eventType + ", success=" + this.success + ", durationMs=" + this.durationMs + ", errorMessage=" + this.errorMessage + ", createdAt=" + this.createdAt + ")";
        }
    }

    @java.lang.SuppressWarnings("all")
    public static StripeEventMetric.StripeEventMetricBuilder builder() {
        return new StripeEventMetric.StripeEventMetricBuilder();
    }

    @java.lang.SuppressWarnings("all")
    public Long getId() {
        return this.id;
    }

    @java.lang.SuppressWarnings("all")
    public String getEventType() {
        return this.eventType;
    }

    @java.lang.SuppressWarnings("all")
    public Boolean getSuccess() {
        return this.success;
    }

    @java.lang.SuppressWarnings("all")
    public Long getDurationMs() {
        return this.durationMs;
    }

    @java.lang.SuppressWarnings("all")
    public String getErrorMessage() {
        return this.errorMessage;
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
    public void setEventType(final String eventType) {
        this.eventType = eventType;
    }

    @java.lang.SuppressWarnings("all")
    public void setSuccess(final Boolean success) {
        this.success = success;
    }

    @java.lang.SuppressWarnings("all")
    public void setDurationMs(final Long durationMs) {
        this.durationMs = durationMs;
    }

    @java.lang.SuppressWarnings("all")
    public void setErrorMessage(final String errorMessage) {
        this.errorMessage = errorMessage;
    }

    @java.lang.SuppressWarnings("all")
    public void setCreatedAt(final LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    @java.lang.SuppressWarnings("all")
    public StripeEventMetric() {
    }

    @java.lang.SuppressWarnings("all")
    public StripeEventMetric(final Long id, final String eventType, final Boolean success, final Long durationMs, final String errorMessage, final LocalDateTime createdAt) {
        this.id = id;
        this.eventType = eventType;
        this.success = success;
        this.durationMs = durationMs;
        this.errorMessage = errorMessage;
        this.createdAt = createdAt;
    }
}
