package com.tradepilot.leakdetector.entity;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "ai_leak_detector_snapshots", indexes = {@Index(name = "idx_ai_leak_detector_user_id", columnList = "user_id"), @Index(name = "idx_ai_leak_detector_user_account_hash", columnList = "user_id, account_id, data_hash"), @Index(name = "idx_ai_leak_detector_user_account_updated", columnList = "user_id, account_id, updated_at")})
public class AiLeakDetectorSnapshot {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @Column(name = "user_id", nullable = false)
    private Long userId;
    @Column(name = "account_id", nullable = false)
    private Long accountId;
    @Column(name = "data_hash", nullable = false, length = 128)
    private String dataHash;
    @Column(name = "total_trades", nullable = false)
    private Integer totalTrades;
    @Column(name = "result_json", columnDefinition = "TEXT", nullable = false)
    private String resultJson;
    @Column(name = "model", length = 80)
    private String model;
    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;
    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    @PrePersist
    void onCreate() {
        LocalDateTime now = LocalDateTime.now();
        createdAt = now;
        updatedAt = now;
    }

    @PreUpdate
    void onUpdate() {
        updatedAt = LocalDateTime.now();
    }


    @java.lang.SuppressWarnings("all")
    public static class AiLeakDetectorSnapshotBuilder {
        @java.lang.SuppressWarnings("all")
        private Long id;
        @java.lang.SuppressWarnings("all")
        private Long userId;
        @java.lang.SuppressWarnings("all")
        private Long accountId;
        @java.lang.SuppressWarnings("all")
        private String dataHash;
        @java.lang.SuppressWarnings("all")
        private Integer totalTrades;
        @java.lang.SuppressWarnings("all")
        private String resultJson;
        @java.lang.SuppressWarnings("all")
        private String model;
        @java.lang.SuppressWarnings("all")
        private LocalDateTime createdAt;
        @java.lang.SuppressWarnings("all")
        private LocalDateTime updatedAt;

        @java.lang.SuppressWarnings("all")
        AiLeakDetectorSnapshotBuilder() {
        }

        /**
         * @return {@code this}.
         */
        @java.lang.SuppressWarnings("all")
        public AiLeakDetectorSnapshot.AiLeakDetectorSnapshotBuilder id(final Long id) {
            this.id = id;
            return this;
        }

        /**
         * @return {@code this}.
         */
        @java.lang.SuppressWarnings("all")
        public AiLeakDetectorSnapshot.AiLeakDetectorSnapshotBuilder userId(final Long userId) {
            this.userId = userId;
            return this;
        }

        /**
         * @return {@code this}.
         */
        @java.lang.SuppressWarnings("all")
        public AiLeakDetectorSnapshot.AiLeakDetectorSnapshotBuilder accountId(final Long accountId) {
            this.accountId = accountId;
            return this;
        }

        /**
         * @return {@code this}.
         */
        @java.lang.SuppressWarnings("all")
        public AiLeakDetectorSnapshot.AiLeakDetectorSnapshotBuilder dataHash(final String dataHash) {
            this.dataHash = dataHash;
            return this;
        }

        /**
         * @return {@code this}.
         */
        @java.lang.SuppressWarnings("all")
        public AiLeakDetectorSnapshot.AiLeakDetectorSnapshotBuilder totalTrades(final Integer totalTrades) {
            this.totalTrades = totalTrades;
            return this;
        }

        /**
         * @return {@code this}.
         */
        @java.lang.SuppressWarnings("all")
        public AiLeakDetectorSnapshot.AiLeakDetectorSnapshotBuilder resultJson(final String resultJson) {
            this.resultJson = resultJson;
            return this;
        }

        /**
         * @return {@code this}.
         */
        @java.lang.SuppressWarnings("all")
        public AiLeakDetectorSnapshot.AiLeakDetectorSnapshotBuilder model(final String model) {
            this.model = model;
            return this;
        }

        /**
         * @return {@code this}.
         */
        @java.lang.SuppressWarnings("all")
        public AiLeakDetectorSnapshot.AiLeakDetectorSnapshotBuilder createdAt(final LocalDateTime createdAt) {
            this.createdAt = createdAt;
            return this;
        }

        /**
         * @return {@code this}.
         */
        @java.lang.SuppressWarnings("all")
        public AiLeakDetectorSnapshot.AiLeakDetectorSnapshotBuilder updatedAt(final LocalDateTime updatedAt) {
            this.updatedAt = updatedAt;
            return this;
        }

        @java.lang.SuppressWarnings("all")
        public AiLeakDetectorSnapshot build() {
            return new AiLeakDetectorSnapshot(this.id, this.userId, this.accountId, this.dataHash, this.totalTrades, this.resultJson, this.model, this.createdAt, this.updatedAt);
        }

        @java.lang.Override
        @java.lang.SuppressWarnings("all")
        public java.lang.String toString() {
            return "AiLeakDetectorSnapshot.AiLeakDetectorSnapshotBuilder(id=" + this.id + ", userId=" + this.userId + ", accountId=" + this.accountId + ", dataHash=" + this.dataHash + ", totalTrades=" + this.totalTrades + ", resultJson=" + this.resultJson + ", model=" + this.model + ", createdAt=" + this.createdAt + ", updatedAt=" + this.updatedAt + ")";
        }
    }

    @java.lang.SuppressWarnings("all")
    public static AiLeakDetectorSnapshot.AiLeakDetectorSnapshotBuilder builder() {
        return new AiLeakDetectorSnapshot.AiLeakDetectorSnapshotBuilder();
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
    public Long getAccountId() {
        return this.accountId;
    }

    @java.lang.SuppressWarnings("all")
    public String getDataHash() {
        return this.dataHash;
    }

    @java.lang.SuppressWarnings("all")
    public Integer getTotalTrades() {
        return this.totalTrades;
    }

    @java.lang.SuppressWarnings("all")
    public String getResultJson() {
        return this.resultJson;
    }

    @java.lang.SuppressWarnings("all")
    public String getModel() {
        return this.model;
    }

    @java.lang.SuppressWarnings("all")
    public LocalDateTime getCreatedAt() {
        return this.createdAt;
    }

    @java.lang.SuppressWarnings("all")
    public LocalDateTime getUpdatedAt() {
        return this.updatedAt;
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
    public void setAccountId(final Long accountId) {
        this.accountId = accountId;
    }

    @java.lang.SuppressWarnings("all")
    public void setDataHash(final String dataHash) {
        this.dataHash = dataHash;
    }

    @java.lang.SuppressWarnings("all")
    public void setTotalTrades(final Integer totalTrades) {
        this.totalTrades = totalTrades;
    }

    @java.lang.SuppressWarnings("all")
    public void setResultJson(final String resultJson) {
        this.resultJson = resultJson;
    }

    @java.lang.SuppressWarnings("all")
    public void setModel(final String model) {
        this.model = model;
    }

    @java.lang.SuppressWarnings("all")
    public void setCreatedAt(final LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    @java.lang.SuppressWarnings("all")
    public void setUpdatedAt(final LocalDateTime updatedAt) {
        this.updatedAt = updatedAt;
    }

    @java.lang.SuppressWarnings("all")
    public AiLeakDetectorSnapshot() {
    }

    @java.lang.SuppressWarnings("all")
    public AiLeakDetectorSnapshot(final Long id, final Long userId, final Long accountId, final String dataHash, final Integer totalTrades, final String resultJson, final String model, final LocalDateTime createdAt, final LocalDateTime updatedAt) {
        this.id = id;
        this.userId = userId;
        this.accountId = accountId;
        this.dataHash = dataHash;
        this.totalTrades = totalTrades;
        this.resultJson = resultJson;
        this.model = model;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
    }
}
