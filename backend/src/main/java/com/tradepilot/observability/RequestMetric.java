package com.tradepilot.observability;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "api_request_metrics", indexes = {@Index(name = "idx_api_request_metrics_created_at", columnList = "created_at"), @Index(name = "idx_api_request_metrics_created_duration", columnList = "created_at, duration_ms"), @Index(name = "idx_api_request_metrics_endpoint_created_at", columnList = "endpoint, created_at"), @Index(name = "idx_api_request_metrics_status_created_at", columnList = "status, created_at"), @Index(name = "idx_api_request_metrics_user_id_created_at", columnList = "user_id, created_at")})
public class RequestMetric {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @Column(nullable = false, length = 220)
    private String endpoint;
    @Column(nullable = false, length = 16)
    private String method;
    @Column(nullable = false)
    private Integer status;
    @Column(name = "duration_ms", nullable = false)
    private Long durationMs;
    @Column(name = "user_id")
    private Long userId;
    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;


    @java.lang.SuppressWarnings("all")
    public static class RequestMetricBuilder {
        @java.lang.SuppressWarnings("all")
        private Long id;
        @java.lang.SuppressWarnings("all")
        private String endpoint;
        @java.lang.SuppressWarnings("all")
        private String method;
        @java.lang.SuppressWarnings("all")
        private Integer status;
        @java.lang.SuppressWarnings("all")
        private Long durationMs;
        @java.lang.SuppressWarnings("all")
        private Long userId;
        @java.lang.SuppressWarnings("all")
        private LocalDateTime createdAt;

        @java.lang.SuppressWarnings("all")
        RequestMetricBuilder() {
        }

        /**
         * @return {@code this}.
         */
        @java.lang.SuppressWarnings("all")
        public RequestMetric.RequestMetricBuilder id(final Long id) {
            this.id = id;
            return this;
        }

        /**
         * @return {@code this}.
         */
        @java.lang.SuppressWarnings("all")
        public RequestMetric.RequestMetricBuilder endpoint(final String endpoint) {
            this.endpoint = endpoint;
            return this;
        }

        /**
         * @return {@code this}.
         */
        @java.lang.SuppressWarnings("all")
        public RequestMetric.RequestMetricBuilder method(final String method) {
            this.method = method;
            return this;
        }

        /**
         * @return {@code this}.
         */
        @java.lang.SuppressWarnings("all")
        public RequestMetric.RequestMetricBuilder status(final Integer status) {
            this.status = status;
            return this;
        }

        /**
         * @return {@code this}.
         */
        @java.lang.SuppressWarnings("all")
        public RequestMetric.RequestMetricBuilder durationMs(final Long durationMs) {
            this.durationMs = durationMs;
            return this;
        }

        /**
         * @return {@code this}.
         */
        @java.lang.SuppressWarnings("all")
        public RequestMetric.RequestMetricBuilder userId(final Long userId) {
            this.userId = userId;
            return this;
        }

        /**
         * @return {@code this}.
         */
        @java.lang.SuppressWarnings("all")
        public RequestMetric.RequestMetricBuilder createdAt(final LocalDateTime createdAt) {
            this.createdAt = createdAt;
            return this;
        }

        @java.lang.SuppressWarnings("all")
        public RequestMetric build() {
            return new RequestMetric(this.id, this.endpoint, this.method, this.status, this.durationMs, this.userId, this.createdAt);
        }

        @java.lang.Override
        @java.lang.SuppressWarnings("all")
        public java.lang.String toString() {
            return "RequestMetric.RequestMetricBuilder(id=" + this.id + ", endpoint=" + this.endpoint + ", method=" + this.method + ", status=" + this.status + ", durationMs=" + this.durationMs + ", userId=" + this.userId + ", createdAt=" + this.createdAt + ")";
        }
    }

    @java.lang.SuppressWarnings("all")
    public static RequestMetric.RequestMetricBuilder builder() {
        return new RequestMetric.RequestMetricBuilder();
    }

    @java.lang.SuppressWarnings("all")
    public Long getId() {
        return this.id;
    }

    @java.lang.SuppressWarnings("all")
    public String getEndpoint() {
        return this.endpoint;
    }

    @java.lang.SuppressWarnings("all")
    public String getMethod() {
        return this.method;
    }

    @java.lang.SuppressWarnings("all")
    public Integer getStatus() {
        return this.status;
    }

    @java.lang.SuppressWarnings("all")
    public Long getDurationMs() {
        return this.durationMs;
    }

    @java.lang.SuppressWarnings("all")
    public Long getUserId() {
        return this.userId;
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
    public void setEndpoint(final String endpoint) {
        this.endpoint = endpoint;
    }

    @java.lang.SuppressWarnings("all")
    public void setMethod(final String method) {
        this.method = method;
    }

    @java.lang.SuppressWarnings("all")
    public void setStatus(final Integer status) {
        this.status = status;
    }

    @java.lang.SuppressWarnings("all")
    public void setDurationMs(final Long durationMs) {
        this.durationMs = durationMs;
    }

    @java.lang.SuppressWarnings("all")
    public void setUserId(final Long userId) {
        this.userId = userId;
    }

    @java.lang.SuppressWarnings("all")
    public void setCreatedAt(final LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    @java.lang.SuppressWarnings("all")
    public RequestMetric() {
    }

    @java.lang.SuppressWarnings("all")
    public RequestMetric(final Long id, final String endpoint, final String method, final Integer status, final Long durationMs, final Long userId, final LocalDateTime createdAt) {
        this.id = id;
        this.endpoint = endpoint;
        this.method = method;
        this.status = status;
        this.durationMs = durationMs;
        this.userId = userId;
        this.createdAt = createdAt;
    }
}
