package com.tradepilot.observability;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "query_performance_events", indexes = {@Index(name = "idx_query_performance_events_created_at", columnList = "created_at"), @Index(name = "idx_query_performance_events_query_name_created_at", columnList = "query_name, created_at")})
public class QueryPerformanceEvent {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @Column(name = "query_name", nullable = false, length = 140)
    private String queryName;
    @Column(name = "duration_ms", nullable = false)
    private Long durationMs;
    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;


    @java.lang.SuppressWarnings("all")
    public static class QueryPerformanceEventBuilder {
        @java.lang.SuppressWarnings("all")
        private Long id;
        @java.lang.SuppressWarnings("all")
        private String queryName;
        @java.lang.SuppressWarnings("all")
        private Long durationMs;
        @java.lang.SuppressWarnings("all")
        private LocalDateTime createdAt;

        @java.lang.SuppressWarnings("all")
        QueryPerformanceEventBuilder() {
        }

        /**
         * @return {@code this}.
         */
        @java.lang.SuppressWarnings("all")
        public QueryPerformanceEvent.QueryPerformanceEventBuilder id(final Long id) {
            this.id = id;
            return this;
        }

        /**
         * @return {@code this}.
         */
        @java.lang.SuppressWarnings("all")
        public QueryPerformanceEvent.QueryPerformanceEventBuilder queryName(final String queryName) {
            this.queryName = queryName;
            return this;
        }

        /**
         * @return {@code this}.
         */
        @java.lang.SuppressWarnings("all")
        public QueryPerformanceEvent.QueryPerformanceEventBuilder durationMs(final Long durationMs) {
            this.durationMs = durationMs;
            return this;
        }

        /**
         * @return {@code this}.
         */
        @java.lang.SuppressWarnings("all")
        public QueryPerformanceEvent.QueryPerformanceEventBuilder createdAt(final LocalDateTime createdAt) {
            this.createdAt = createdAt;
            return this;
        }

        @java.lang.SuppressWarnings("all")
        public QueryPerformanceEvent build() {
            return new QueryPerformanceEvent(this.id, this.queryName, this.durationMs, this.createdAt);
        }

        @java.lang.Override
        @java.lang.SuppressWarnings("all")
        public java.lang.String toString() {
            return "QueryPerformanceEvent.QueryPerformanceEventBuilder(id=" + this.id + ", queryName=" + this.queryName + ", durationMs=" + this.durationMs + ", createdAt=" + this.createdAt + ")";
        }
    }

    @java.lang.SuppressWarnings("all")
    public static QueryPerformanceEvent.QueryPerformanceEventBuilder builder() {
        return new QueryPerformanceEvent.QueryPerformanceEventBuilder();
    }

    @java.lang.SuppressWarnings("all")
    public Long getId() {
        return this.id;
    }

    @java.lang.SuppressWarnings("all")
    public String getQueryName() {
        return this.queryName;
    }

    @java.lang.SuppressWarnings("all")
    public Long getDurationMs() {
        return this.durationMs;
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
    public void setQueryName(final String queryName) {
        this.queryName = queryName;
    }

    @java.lang.SuppressWarnings("all")
    public void setDurationMs(final Long durationMs) {
        this.durationMs = durationMs;
    }

    @java.lang.SuppressWarnings("all")
    public void setCreatedAt(final LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    @java.lang.SuppressWarnings("all")
    public QueryPerformanceEvent() {
    }

    @java.lang.SuppressWarnings("all")
    public QueryPerformanceEvent(final Long id, final String queryName, final Long durationMs, final LocalDateTime createdAt) {
        this.id = id;
        this.queryName = queryName;
        this.durationMs = durationMs;
        this.createdAt = createdAt;
    }
}
