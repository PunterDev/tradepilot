package com.tradepilot.coach.entity;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(name = "ai_coach_snapshot", indexes = {@Index(name = "idx_ai_coach_snapshot_user_id", columnList = "user_id"), @Index(name = "idx_ai_coach_snapshot_account_id", columnList = "account_id"), @Index(name = "idx_ai_coach_snapshot_user_account_type_period", columnList = "user_id, account_id, snapshot_type, period_start, period_end")}, uniqueConstraints = {@UniqueConstraint(name = "uk_ai_coach_snapshot", columnNames = {"user_id", "account_id", "snapshot_type", "period_start", "period_end", "review_hash"})})
public class AiCoachSnapshot {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @Column(name = "user_id", nullable = false)
    private Long userId;
    @Column(name = "account_id", nullable = false)
    private Long accountId;
    @Enumerated(EnumType.STRING)
    @Column(name = "snapshot_type", nullable = false, length = 20)
    private CoachSnapshotType snapshotType;
    @Column(name = "period_start", nullable = false)
    private LocalDate periodStart;
    @Column(name = "period_end", nullable = false)
    private LocalDate periodEnd;
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private CoachSnapshotStatus status;
    @Column(name = "total_trades")
    private Integer totalTrades;
    @Column(name = "net_pnl", precision = 12, scale = 2)
    private BigDecimal netPnl;
    @Column(name = "win_rate", precision = 6, scale = 2)
    private BigDecimal winRate;
    @Column(name = "profit_factor", precision = 8, scale = 2)
    private BigDecimal profitFactor;
    @Column(name = "best_setup", length = 100)
    private String bestSetup;
    @Column(name = "worst_setup", length = 100)
    private String worstSetup;
    @Column(name = "best_session", length = 50)
    private String bestSession;
    @Column(name = "worst_session", length = 50)
    private String worstSession;
    @Column(name = "biggest_mistake", length = 255)
    private String biggestMistake;
    @Column(name = "deterministic_summary_json", columnDefinition = "TEXT")
    private String deterministicSummaryJson;
    @Column(name = "ai_response_json", columnDefinition = "TEXT")
    private String aiResponseJson;
    @Column(name = "review_hash", length = 128)
    private String reviewHash;
    @Column(name = "error_message", length = 1000)
    private String errorMessage;
    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;
    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    @PrePersist
    void onCreate() {
        LocalDateTime now = LocalDateTime.now();
        createdAt = now;
        updatedAt = now;
        if (status == null) {
            status = CoachSnapshotStatus.PENDING;
        }
    }

    @PreUpdate
    void onUpdate() {
        updatedAt = LocalDateTime.now();
    }


    @java.lang.SuppressWarnings("all")
    public static class AiCoachSnapshotBuilder {
        @java.lang.SuppressWarnings("all")
        private Long id;
        @java.lang.SuppressWarnings("all")
        private Long userId;
        @java.lang.SuppressWarnings("all")
        private Long accountId;
        @java.lang.SuppressWarnings("all")
        private CoachSnapshotType snapshotType;
        @java.lang.SuppressWarnings("all")
        private LocalDate periodStart;
        @java.lang.SuppressWarnings("all")
        private LocalDate periodEnd;
        @java.lang.SuppressWarnings("all")
        private CoachSnapshotStatus status;
        @java.lang.SuppressWarnings("all")
        private Integer totalTrades;
        @java.lang.SuppressWarnings("all")
        private BigDecimal netPnl;
        @java.lang.SuppressWarnings("all")
        private BigDecimal winRate;
        @java.lang.SuppressWarnings("all")
        private BigDecimal profitFactor;
        @java.lang.SuppressWarnings("all")
        private String bestSetup;
        @java.lang.SuppressWarnings("all")
        private String worstSetup;
        @java.lang.SuppressWarnings("all")
        private String bestSession;
        @java.lang.SuppressWarnings("all")
        private String worstSession;
        @java.lang.SuppressWarnings("all")
        private String biggestMistake;
        @java.lang.SuppressWarnings("all")
        private String deterministicSummaryJson;
        @java.lang.SuppressWarnings("all")
        private String aiResponseJson;
        @java.lang.SuppressWarnings("all")
        private String reviewHash;
        @java.lang.SuppressWarnings("all")
        private String errorMessage;
        @java.lang.SuppressWarnings("all")
        private LocalDateTime createdAt;
        @java.lang.SuppressWarnings("all")
        private LocalDateTime updatedAt;

        @java.lang.SuppressWarnings("all")
        AiCoachSnapshotBuilder() {
        }

        /**
         * @return {@code this}.
         */
        @java.lang.SuppressWarnings("all")
        public AiCoachSnapshot.AiCoachSnapshotBuilder id(final Long id) {
            this.id = id;
            return this;
        }

        /**
         * @return {@code this}.
         */
        @java.lang.SuppressWarnings("all")
        public AiCoachSnapshot.AiCoachSnapshotBuilder userId(final Long userId) {
            this.userId = userId;
            return this;
        }

        /**
         * @return {@code this}.
         */
        @java.lang.SuppressWarnings("all")
        public AiCoachSnapshot.AiCoachSnapshotBuilder accountId(final Long accountId) {
            this.accountId = accountId;
            return this;
        }

        /**
         * @return {@code this}.
         */
        @java.lang.SuppressWarnings("all")
        public AiCoachSnapshot.AiCoachSnapshotBuilder snapshotType(final CoachSnapshotType snapshotType) {
            this.snapshotType = snapshotType;
            return this;
        }

        /**
         * @return {@code this}.
         */
        @java.lang.SuppressWarnings("all")
        public AiCoachSnapshot.AiCoachSnapshotBuilder periodStart(final LocalDate periodStart) {
            this.periodStart = periodStart;
            return this;
        }

        /**
         * @return {@code this}.
         */
        @java.lang.SuppressWarnings("all")
        public AiCoachSnapshot.AiCoachSnapshotBuilder periodEnd(final LocalDate periodEnd) {
            this.periodEnd = periodEnd;
            return this;
        }

        /**
         * @return {@code this}.
         */
        @java.lang.SuppressWarnings("all")
        public AiCoachSnapshot.AiCoachSnapshotBuilder status(final CoachSnapshotStatus status) {
            this.status = status;
            return this;
        }

        /**
         * @return {@code this}.
         */
        @java.lang.SuppressWarnings("all")
        public AiCoachSnapshot.AiCoachSnapshotBuilder totalTrades(final Integer totalTrades) {
            this.totalTrades = totalTrades;
            return this;
        }

        /**
         * @return {@code this}.
         */
        @java.lang.SuppressWarnings("all")
        public AiCoachSnapshot.AiCoachSnapshotBuilder netPnl(final BigDecimal netPnl) {
            this.netPnl = netPnl;
            return this;
        }

        /**
         * @return {@code this}.
         */
        @java.lang.SuppressWarnings("all")
        public AiCoachSnapshot.AiCoachSnapshotBuilder winRate(final BigDecimal winRate) {
            this.winRate = winRate;
            return this;
        }

        /**
         * @return {@code this}.
         */
        @java.lang.SuppressWarnings("all")
        public AiCoachSnapshot.AiCoachSnapshotBuilder profitFactor(final BigDecimal profitFactor) {
            this.profitFactor = profitFactor;
            return this;
        }

        /**
         * @return {@code this}.
         */
        @java.lang.SuppressWarnings("all")
        public AiCoachSnapshot.AiCoachSnapshotBuilder bestSetup(final String bestSetup) {
            this.bestSetup = bestSetup;
            return this;
        }

        /**
         * @return {@code this}.
         */
        @java.lang.SuppressWarnings("all")
        public AiCoachSnapshot.AiCoachSnapshotBuilder worstSetup(final String worstSetup) {
            this.worstSetup = worstSetup;
            return this;
        }

        /**
         * @return {@code this}.
         */
        @java.lang.SuppressWarnings("all")
        public AiCoachSnapshot.AiCoachSnapshotBuilder bestSession(final String bestSession) {
            this.bestSession = bestSession;
            return this;
        }

        /**
         * @return {@code this}.
         */
        @java.lang.SuppressWarnings("all")
        public AiCoachSnapshot.AiCoachSnapshotBuilder worstSession(final String worstSession) {
            this.worstSession = worstSession;
            return this;
        }

        /**
         * @return {@code this}.
         */
        @java.lang.SuppressWarnings("all")
        public AiCoachSnapshot.AiCoachSnapshotBuilder biggestMistake(final String biggestMistake) {
            this.biggestMistake = biggestMistake;
            return this;
        }

        /**
         * @return {@code this}.
         */
        @java.lang.SuppressWarnings("all")
        public AiCoachSnapshot.AiCoachSnapshotBuilder deterministicSummaryJson(final String deterministicSummaryJson) {
            this.deterministicSummaryJson = deterministicSummaryJson;
            return this;
        }

        /**
         * @return {@code this}.
         */
        @java.lang.SuppressWarnings("all")
        public AiCoachSnapshot.AiCoachSnapshotBuilder aiResponseJson(final String aiResponseJson) {
            this.aiResponseJson = aiResponseJson;
            return this;
        }

        /**
         * @return {@code this}.
         */
        @java.lang.SuppressWarnings("all")
        public AiCoachSnapshot.AiCoachSnapshotBuilder reviewHash(final String reviewHash) {
            this.reviewHash = reviewHash;
            return this;
        }

        /**
         * @return {@code this}.
         */
        @java.lang.SuppressWarnings("all")
        public AiCoachSnapshot.AiCoachSnapshotBuilder errorMessage(final String errorMessage) {
            this.errorMessage = errorMessage;
            return this;
        }

        /**
         * @return {@code this}.
         */
        @java.lang.SuppressWarnings("all")
        public AiCoachSnapshot.AiCoachSnapshotBuilder createdAt(final LocalDateTime createdAt) {
            this.createdAt = createdAt;
            return this;
        }

        /**
         * @return {@code this}.
         */
        @java.lang.SuppressWarnings("all")
        public AiCoachSnapshot.AiCoachSnapshotBuilder updatedAt(final LocalDateTime updatedAt) {
            this.updatedAt = updatedAt;
            return this;
        }

        @java.lang.SuppressWarnings("all")
        public AiCoachSnapshot build() {
            return new AiCoachSnapshot(this.id, this.userId, this.accountId, this.snapshotType, this.periodStart, this.periodEnd, this.status, this.totalTrades, this.netPnl, this.winRate, this.profitFactor, this.bestSetup, this.worstSetup, this.bestSession, this.worstSession, this.biggestMistake, this.deterministicSummaryJson, this.aiResponseJson, this.reviewHash, this.errorMessage, this.createdAt, this.updatedAt);
        }

        @java.lang.Override
        @java.lang.SuppressWarnings("all")
        public java.lang.String toString() {
            return "AiCoachSnapshot.AiCoachSnapshotBuilder(id=" + this.id + ", userId=" + this.userId + ", accountId=" + this.accountId + ", snapshotType=" + this.snapshotType + ", periodStart=" + this.periodStart + ", periodEnd=" + this.periodEnd + ", status=" + this.status + ", totalTrades=" + this.totalTrades + ", netPnl=" + this.netPnl + ", winRate=" + this.winRate + ", profitFactor=" + this.profitFactor + ", bestSetup=" + this.bestSetup + ", worstSetup=" + this.worstSetup + ", bestSession=" + this.bestSession + ", worstSession=" + this.worstSession + ", biggestMistake=" + this.biggestMistake + ", deterministicSummaryJson=" + this.deterministicSummaryJson + ", aiResponseJson=" + this.aiResponseJson + ", reviewHash=" + this.reviewHash + ", errorMessage=" + this.errorMessage + ", createdAt=" + this.createdAt + ", updatedAt=" + this.updatedAt + ")";
        }
    }

    @java.lang.SuppressWarnings("all")
    public static AiCoachSnapshot.AiCoachSnapshotBuilder builder() {
        return new AiCoachSnapshot.AiCoachSnapshotBuilder();
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
    public CoachSnapshotType getSnapshotType() {
        return this.snapshotType;
    }

    @java.lang.SuppressWarnings("all")
    public LocalDate getPeriodStart() {
        return this.periodStart;
    }

    @java.lang.SuppressWarnings("all")
    public LocalDate getPeriodEnd() {
        return this.periodEnd;
    }

    @java.lang.SuppressWarnings("all")
    public CoachSnapshotStatus getStatus() {
        return this.status;
    }

    @java.lang.SuppressWarnings("all")
    public Integer getTotalTrades() {
        return this.totalTrades;
    }

    @java.lang.SuppressWarnings("all")
    public BigDecimal getNetPnl() {
        return this.netPnl;
    }

    @java.lang.SuppressWarnings("all")
    public BigDecimal getWinRate() {
        return this.winRate;
    }

    @java.lang.SuppressWarnings("all")
    public BigDecimal getProfitFactor() {
        return this.profitFactor;
    }

    @java.lang.SuppressWarnings("all")
    public String getBestSetup() {
        return this.bestSetup;
    }

    @java.lang.SuppressWarnings("all")
    public String getWorstSetup() {
        return this.worstSetup;
    }

    @java.lang.SuppressWarnings("all")
    public String getBestSession() {
        return this.bestSession;
    }

    @java.lang.SuppressWarnings("all")
    public String getWorstSession() {
        return this.worstSession;
    }

    @java.lang.SuppressWarnings("all")
    public String getBiggestMistake() {
        return this.biggestMistake;
    }

    @java.lang.SuppressWarnings("all")
    public String getDeterministicSummaryJson() {
        return this.deterministicSummaryJson;
    }

    @java.lang.SuppressWarnings("all")
    public String getAiResponseJson() {
        return this.aiResponseJson;
    }

    @java.lang.SuppressWarnings("all")
    public String getReviewHash() {
        return this.reviewHash;
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
    public void setSnapshotType(final CoachSnapshotType snapshotType) {
        this.snapshotType = snapshotType;
    }

    @java.lang.SuppressWarnings("all")
    public void setPeriodStart(final LocalDate periodStart) {
        this.periodStart = periodStart;
    }

    @java.lang.SuppressWarnings("all")
    public void setPeriodEnd(final LocalDate periodEnd) {
        this.periodEnd = periodEnd;
    }

    @java.lang.SuppressWarnings("all")
    public void setStatus(final CoachSnapshotStatus status) {
        this.status = status;
    }

    @java.lang.SuppressWarnings("all")
    public void setTotalTrades(final Integer totalTrades) {
        this.totalTrades = totalTrades;
    }

    @java.lang.SuppressWarnings("all")
    public void setNetPnl(final BigDecimal netPnl) {
        this.netPnl = netPnl;
    }

    @java.lang.SuppressWarnings("all")
    public void setWinRate(final BigDecimal winRate) {
        this.winRate = winRate;
    }

    @java.lang.SuppressWarnings("all")
    public void setProfitFactor(final BigDecimal profitFactor) {
        this.profitFactor = profitFactor;
    }

    @java.lang.SuppressWarnings("all")
    public void setBestSetup(final String bestSetup) {
        this.bestSetup = bestSetup;
    }

    @java.lang.SuppressWarnings("all")
    public void setWorstSetup(final String worstSetup) {
        this.worstSetup = worstSetup;
    }

    @java.lang.SuppressWarnings("all")
    public void setBestSession(final String bestSession) {
        this.bestSession = bestSession;
    }

    @java.lang.SuppressWarnings("all")
    public void setWorstSession(final String worstSession) {
        this.worstSession = worstSession;
    }

    @java.lang.SuppressWarnings("all")
    public void setBiggestMistake(final String biggestMistake) {
        this.biggestMistake = biggestMistake;
    }

    @java.lang.SuppressWarnings("all")
    public void setDeterministicSummaryJson(final String deterministicSummaryJson) {
        this.deterministicSummaryJson = deterministicSummaryJson;
    }

    @java.lang.SuppressWarnings("all")
    public void setAiResponseJson(final String aiResponseJson) {
        this.aiResponseJson = aiResponseJson;
    }

    @java.lang.SuppressWarnings("all")
    public void setReviewHash(final String reviewHash) {
        this.reviewHash = reviewHash;
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
    public void setUpdatedAt(final LocalDateTime updatedAt) {
        this.updatedAt = updatedAt;
    }

    @java.lang.SuppressWarnings("all")
    public AiCoachSnapshot() {
    }

    @java.lang.SuppressWarnings("all")
    public AiCoachSnapshot(final Long id, final Long userId, final Long accountId, final CoachSnapshotType snapshotType, final LocalDate periodStart, final LocalDate periodEnd, final CoachSnapshotStatus status, final Integer totalTrades, final BigDecimal netPnl, final BigDecimal winRate, final BigDecimal profitFactor, final String bestSetup, final String worstSetup, final String bestSession, final String worstSession, final String biggestMistake, final String deterministicSummaryJson, final String aiResponseJson, final String reviewHash, final String errorMessage, final LocalDateTime createdAt, final LocalDateTime updatedAt) {
        this.id = id;
        this.userId = userId;
        this.accountId = accountId;
        this.snapshotType = snapshotType;
        this.periodStart = periodStart;
        this.periodEnd = periodEnd;
        this.status = status;
        this.totalTrades = totalTrades;
        this.netPnl = netPnl;
        this.winRate = winRate;
        this.profitFactor = profitFactor;
        this.bestSetup = bestSetup;
        this.worstSetup = worstSetup;
        this.bestSession = bestSession;
        this.worstSession = worstSession;
        this.biggestMistake = biggestMistake;
        this.deterministicSummaryJson = deterministicSummaryJson;
        this.aiResponseJson = aiResponseJson;
        this.reviewHash = reviewHash;
        this.errorMessage = errorMessage;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
    }
}
