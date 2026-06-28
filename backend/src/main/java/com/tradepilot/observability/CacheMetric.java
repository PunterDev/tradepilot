package com.tradepilot.observability;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "cache_metrics", indexes = {@Index(name = "idx_cache_metrics_cache_name_created_at", columnList = "cache_name, created_at"), @Index(name = "idx_cache_metrics_created_at", columnList = "created_at")})
public class CacheMetric {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @Column(name = "cache_name", nullable = false, length = 120)
    private String cacheName;
    @Column(nullable = false)
    private Boolean hit;
    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;


    @java.lang.SuppressWarnings("all")
    public static class CacheMetricBuilder {
        @java.lang.SuppressWarnings("all")
        private Long id;
        @java.lang.SuppressWarnings("all")
        private String cacheName;
        @java.lang.SuppressWarnings("all")
        private Boolean hit;
        @java.lang.SuppressWarnings("all")
        private LocalDateTime createdAt;

        @java.lang.SuppressWarnings("all")
        CacheMetricBuilder() {
        }

        /**
         * @return {@code this}.
         */
        @java.lang.SuppressWarnings("all")
        public CacheMetric.CacheMetricBuilder id(final Long id) {
            this.id = id;
            return this;
        }

        /**
         * @return {@code this}.
         */
        @java.lang.SuppressWarnings("all")
        public CacheMetric.CacheMetricBuilder cacheName(final String cacheName) {
            this.cacheName = cacheName;
            return this;
        }

        /**
         * @return {@code this}.
         */
        @java.lang.SuppressWarnings("all")
        public CacheMetric.CacheMetricBuilder hit(final Boolean hit) {
            this.hit = hit;
            return this;
        }

        /**
         * @return {@code this}.
         */
        @java.lang.SuppressWarnings("all")
        public CacheMetric.CacheMetricBuilder createdAt(final LocalDateTime createdAt) {
            this.createdAt = createdAt;
            return this;
        }

        @java.lang.SuppressWarnings("all")
        public CacheMetric build() {
            return new CacheMetric(this.id, this.cacheName, this.hit, this.createdAt);
        }

        @java.lang.Override
        @java.lang.SuppressWarnings("all")
        public java.lang.String toString() {
            return "CacheMetric.CacheMetricBuilder(id=" + this.id + ", cacheName=" + this.cacheName + ", hit=" + this.hit + ", createdAt=" + this.createdAt + ")";
        }
    }

    @java.lang.SuppressWarnings("all")
    public static CacheMetric.CacheMetricBuilder builder() {
        return new CacheMetric.CacheMetricBuilder();
    }

    @java.lang.SuppressWarnings("all")
    public Long getId() {
        return this.id;
    }

    @java.lang.SuppressWarnings("all")
    public String getCacheName() {
        return this.cacheName;
    }

    @java.lang.SuppressWarnings("all")
    public Boolean getHit() {
        return this.hit;
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
    public void setCacheName(final String cacheName) {
        this.cacheName = cacheName;
    }

    @java.lang.SuppressWarnings("all")
    public void setHit(final Boolean hit) {
        this.hit = hit;
    }

    @java.lang.SuppressWarnings("all")
    public void setCreatedAt(final LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    @java.lang.SuppressWarnings("all")
    public CacheMetric() {
    }

    @java.lang.SuppressWarnings("all")
    public CacheMetric(final Long id, final String cacheName, final Boolean hit, final LocalDateTime createdAt) {
        this.id = id;
        this.cacheName = cacheName;
        this.hit = hit;
        this.createdAt = createdAt;
    }
}
