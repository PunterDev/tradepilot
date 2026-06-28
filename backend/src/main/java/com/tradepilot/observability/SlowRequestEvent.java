package com.tradepilot.observability;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "slow_request_events", indexes = {@Index(name = "idx_slow_request_events_created_at", columnList = "created_at"), @Index(name = "idx_slow_request_events_endpoint_created_at", columnList = "endpoint, created_at")})
public class SlowRequestEvent {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @Column(nullable = false, length = 220)
    private String endpoint;
    @Column(name = "duration_ms", nullable = false)
    private Long durationMs;
    @Column(nullable = false)
    private Integer status;
    @Column(name = "user_id")
    private Long userId;
    @Column(nullable = false, length = 16)
    private String severity;
    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;


    @java.lang.SuppressWarnings("all")
    public static class SlowRequestEventBuilder {
        @java.lang.SuppressWarnings("all")
        private Long id;
        @java.lang.SuppressWarnings("all")
        private String endpoint;
        @java.lang.SuppressWarnings("all")
        private Long durationMs;
        @java.lang.SuppressWarnings("all")
        private Integer status;
        @java.lang.SuppressWarnings("all")
        private Long userId;
        @java.lang.SuppressWarnings("all")
        private String severity;
        @java.lang.SuppressWarnings("all")
        private LocalDateTime createdAt;

        @java.lang.SuppressWarnings("all")
        SlowRequestEventBuilder() {
        }

        /**
         * @return {@code this}.
         */
        @java.lang.SuppressWarnings("all")
        public SlowRequestEvent.SlowRequestEventBuilder id(final Long id) {
            this.id = id;
            return this;
        }

        /**
         * @return {@code this}.
         */
        @java.lang.SuppressWarnings("all")
        public SlowRequestEvent.SlowRequestEventBuilder endpoint(final String endpoint) {
            this.endpoint = endpoint;
            return this;
        }

        /**
         * @return {@code this}.
         */
        @java.lang.SuppressWarnings("all")
        public SlowRequestEvent.SlowRequestEventBuilder durationMs(final Long durationMs) {
            this.durationMs = durationMs;
            return this;
        }

        /**
         * @return {@code this}.
         */
        @java.lang.SuppressWarnings("all")
        public SlowRequestEvent.SlowRequestEventBuilder status(final Integer status) {
            this.status = status;
            return this;
        }

        /**
         * @return {@code this}.
         */
        @java.lang.SuppressWarnings("all")
        public SlowRequestEvent.SlowRequestEventBuilder userId(final Long userId) {
            this.userId = userId;
            return this;
        }

        /**
         * @return {@code this}.
         */
        @java.lang.SuppressWarnings("all")
        public SlowRequestEvent.SlowRequestEventBuilder severity(final String severity) {
            this.severity = severity;
            return this;
        }

        /**
         * @return {@code this}.
         */
        @java.lang.SuppressWarnings("all")
        public SlowRequestEvent.SlowRequestEventBuilder createdAt(final LocalDateTime createdAt) {
            this.createdAt = createdAt;
            return this;
        }

        @java.lang.SuppressWarnings("all")
        public SlowRequestEvent build() {
            return new SlowRequestEvent(this.id, this.endpoint, this.durationMs, this.status, this.userId, this.severity, this.createdAt);
        }

        @java.lang.Override
        @java.lang.SuppressWarnings("all")
        public java.lang.String toString() {
            return "SlowRequestEvent.SlowRequestEventBuilder(id=" + this.id + ", endpoint=" + this.endpoint + ", durationMs=" + this.durationMs + ", status=" + this.status + ", userId=" + this.userId + ", severity=" + this.severity + ", createdAt=" + this.createdAt + ")";
        }
    }

    @java.lang.SuppressWarnings("all")
    public static SlowRequestEvent.SlowRequestEventBuilder builder() {
        return new SlowRequestEvent.SlowRequestEventBuilder();
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
    public Long getDurationMs() {
        return this.durationMs;
    }

    @java.lang.SuppressWarnings("all")
    public Integer getStatus() {
        return this.status;
    }

    @java.lang.SuppressWarnings("all")
    public Long getUserId() {
        return this.userId;
    }

    @java.lang.SuppressWarnings("all")
    public String getSeverity() {
        return this.severity;
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
    public void setDurationMs(final Long durationMs) {
        this.durationMs = durationMs;
    }

    @java.lang.SuppressWarnings("all")
    public void setStatus(final Integer status) {
        this.status = status;
    }

    @java.lang.SuppressWarnings("all")
    public void setUserId(final Long userId) {
        this.userId = userId;
    }

    @java.lang.SuppressWarnings("all")
    public void setSeverity(final String severity) {
        this.severity = severity;
    }

    @java.lang.SuppressWarnings("all")
    public void setCreatedAt(final LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    @java.lang.SuppressWarnings("all")
    public SlowRequestEvent() {
    }

    @java.lang.SuppressWarnings("all")
    public SlowRequestEvent(final Long id, final String endpoint, final Long durationMs, final Integer status, final Long userId, final String severity, final LocalDateTime createdAt) {
        this.id = id;
        this.endpoint = endpoint;
        this.durationMs = durationMs;
        this.status = status;
        this.userId = userId;
        this.severity = severity;
        this.createdAt = createdAt;
    }
}
