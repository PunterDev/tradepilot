package com.tradepilot.tradingdna.entity;

import jakarta.persistence.*;
import java.time.LocalDateTime;
import java.math.BigDecimal;

@Entity
@Table(name = "trading_dna_snapshots", indexes = {@Index(name = "idx_trading_dna_user_id", columnList = "user_id"), @Index(name = "idx_trading_dna_user_account_timeframe", columnList = "user_id, account_id, days"), @Index(name = "idx_trading_dna_data_hash", columnList = "data_hash"), @Index(name = "idx_trading_dna_user_account_hash", columnList = "user_id, account_id, data_hash"), @Index(name = "idx_trading_dna_user_account_days_hash", columnList = "user_id, account_id, days, include_all_accounts, data_hash"), @Index(name = "idx_trading_dna_user_created_at", columnList = "user_id, created_at")})
public class TradingDnaSnapshot {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @Column(name = "user_id", nullable = false)
    private Long userId;
    @Column(name = "account_id")
    private Long accountId;
    @Column(nullable = false)
    private Integer days;
    @Column(name = "include_all_accounts", nullable = false)
    private Boolean includeAllAccounts;
    @Column(name = "data_hash", nullable = false, length = 128)
    private String dataHash;
    @Column(nullable = false, length = 32)
    private String status;
    @Column(name = "total_trades")
    private Integer totalTrades;
    @Column(name = "ai_response_json", columnDefinition = "TEXT")
    private String aiResponseJson;
    @Column(name = "context_json", columnDefinition = "TEXT")
    private String contextJson;
    @Column(name = "timeline_json", columnDefinition = "TEXT")
    private String timelineJson;
    @Column(length = 80)
    private String model;
    @Column(name = "prompt_tokens")
    private Integer promptTokens;
    @Column(name = "completion_tokens")
    private Integer completionTokens;
    @Column(name = "total_tokens")
    private Integer totalTokens;
    @Column(name = "estimated_cost", precision = 12, scale = 6)
    private BigDecimal estimatedCost;
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
    public static class TradingDnaSnapshotBuilder {
        @java.lang.SuppressWarnings("all")
        private Long id;
        @java.lang.SuppressWarnings("all")
        private Long userId;
        @java.lang.SuppressWarnings("all")
        private Long accountId;
        @java.lang.SuppressWarnings("all")
        private Integer days;
        @java.lang.SuppressWarnings("all")
        private Boolean includeAllAccounts;
        @java.lang.SuppressWarnings("all")
        private String dataHash;
        @java.lang.SuppressWarnings("all")
        private String status;
        @java.lang.SuppressWarnings("all")
        private Integer totalTrades;
        @java.lang.SuppressWarnings("all")
        private String aiResponseJson;
        @java.lang.SuppressWarnings("all")
        private String contextJson;
        @java.lang.SuppressWarnings("all")
        private String timelineJson;
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
        private LocalDateTime createdAt;
        @java.lang.SuppressWarnings("all")
        private LocalDateTime updatedAt;

        @java.lang.SuppressWarnings("all")
        TradingDnaSnapshotBuilder() {
        }

        /**
         * @return {@code this}.
         */
        @java.lang.SuppressWarnings("all")
        public TradingDnaSnapshot.TradingDnaSnapshotBuilder id(final Long id) {
            this.id = id;
            return this;
        }

        /**
         * @return {@code this}.
         */
        @java.lang.SuppressWarnings("all")
        public TradingDnaSnapshot.TradingDnaSnapshotBuilder userId(final Long userId) {
            this.userId = userId;
            return this;
        }

        /**
         * @return {@code this}.
         */
        @java.lang.SuppressWarnings("all")
        public TradingDnaSnapshot.TradingDnaSnapshotBuilder accountId(final Long accountId) {
            this.accountId = accountId;
            return this;
        }

        /**
         * @return {@code this}.
         */
        @java.lang.SuppressWarnings("all")
        public TradingDnaSnapshot.TradingDnaSnapshotBuilder days(final Integer days) {
            this.days = days;
            return this;
        }

        /**
         * @return {@code this}.
         */
        @java.lang.SuppressWarnings("all")
        public TradingDnaSnapshot.TradingDnaSnapshotBuilder includeAllAccounts(final Boolean includeAllAccounts) {
            this.includeAllAccounts = includeAllAccounts;
            return this;
        }

        /**
         * @return {@code this}.
         */
        @java.lang.SuppressWarnings("all")
        public TradingDnaSnapshot.TradingDnaSnapshotBuilder dataHash(final String dataHash) {
            this.dataHash = dataHash;
            return this;
        }

        /**
         * @return {@code this}.
         */
        @java.lang.SuppressWarnings("all")
        public TradingDnaSnapshot.TradingDnaSnapshotBuilder status(final String status) {
            this.status = status;
            return this;
        }

        /**
         * @return {@code this}.
         */
        @java.lang.SuppressWarnings("all")
        public TradingDnaSnapshot.TradingDnaSnapshotBuilder totalTrades(final Integer totalTrades) {
            this.totalTrades = totalTrades;
            return this;
        }

        /**
         * @return {@code this}.
         */
        @java.lang.SuppressWarnings("all")
        public TradingDnaSnapshot.TradingDnaSnapshotBuilder aiResponseJson(final String aiResponseJson) {
            this.aiResponseJson = aiResponseJson;
            return this;
        }

        /**
         * @return {@code this}.
         */
        @java.lang.SuppressWarnings("all")
        public TradingDnaSnapshot.TradingDnaSnapshotBuilder contextJson(final String contextJson) {
            this.contextJson = contextJson;
            return this;
        }

        /**
         * @return {@code this}.
         */
        @java.lang.SuppressWarnings("all")
        public TradingDnaSnapshot.TradingDnaSnapshotBuilder timelineJson(final String timelineJson) {
            this.timelineJson = timelineJson;
            return this;
        }

        /**
         * @return {@code this}.
         */
        @java.lang.SuppressWarnings("all")
        public TradingDnaSnapshot.TradingDnaSnapshotBuilder model(final String model) {
            this.model = model;
            return this;
        }

        /**
         * @return {@code this}.
         */
        @java.lang.SuppressWarnings("all")
        public TradingDnaSnapshot.TradingDnaSnapshotBuilder promptTokens(final Integer promptTokens) {
            this.promptTokens = promptTokens;
            return this;
        }

        /**
         * @return {@code this}.
         */
        @java.lang.SuppressWarnings("all")
        public TradingDnaSnapshot.TradingDnaSnapshotBuilder completionTokens(final Integer completionTokens) {
            this.completionTokens = completionTokens;
            return this;
        }

        /**
         * @return {@code this}.
         */
        @java.lang.SuppressWarnings("all")
        public TradingDnaSnapshot.TradingDnaSnapshotBuilder totalTokens(final Integer totalTokens) {
            this.totalTokens = totalTokens;
            return this;
        }

        /**
         * @return {@code this}.
         */
        @java.lang.SuppressWarnings("all")
        public TradingDnaSnapshot.TradingDnaSnapshotBuilder estimatedCost(final BigDecimal estimatedCost) {
            this.estimatedCost = estimatedCost;
            return this;
        }

        /**
         * @return {@code this}.
         */
        @java.lang.SuppressWarnings("all")
        public TradingDnaSnapshot.TradingDnaSnapshotBuilder createdAt(final LocalDateTime createdAt) {
            this.createdAt = createdAt;
            return this;
        }

        /**
         * @return {@code this}.
         */
        @java.lang.SuppressWarnings("all")
        public TradingDnaSnapshot.TradingDnaSnapshotBuilder updatedAt(final LocalDateTime updatedAt) {
            this.updatedAt = updatedAt;
            return this;
        }

        @java.lang.SuppressWarnings("all")
        public TradingDnaSnapshot build() {
            return new TradingDnaSnapshot(this.id, this.userId, this.accountId, this.days, this.includeAllAccounts, this.dataHash, this.status, this.totalTrades, this.aiResponseJson, this.contextJson, this.timelineJson, this.model, this.promptTokens, this.completionTokens, this.totalTokens, this.estimatedCost, this.createdAt, this.updatedAt);
        }

        @java.lang.Override
        @java.lang.SuppressWarnings("all")
        public java.lang.String toString() {
            return "TradingDnaSnapshot.TradingDnaSnapshotBuilder(id=" + this.id + ", userId=" + this.userId + ", accountId=" + this.accountId + ", days=" + this.days + ", includeAllAccounts=" + this.includeAllAccounts + ", dataHash=" + this.dataHash + ", status=" + this.status + ", totalTrades=" + this.totalTrades + ", aiResponseJson=" + this.aiResponseJson + ", contextJson=" + this.contextJson + ", timelineJson=" + this.timelineJson + ", model=" + this.model + ", promptTokens=" + this.promptTokens + ", completionTokens=" + this.completionTokens + ", totalTokens=" + this.totalTokens + ", estimatedCost=" + this.estimatedCost + ", createdAt=" + this.createdAt + ", updatedAt=" + this.updatedAt + ")";
        }
    }

    @java.lang.SuppressWarnings("all")
    public static TradingDnaSnapshot.TradingDnaSnapshotBuilder builder() {
        return new TradingDnaSnapshot.TradingDnaSnapshotBuilder();
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
    public Integer getDays() {
        return this.days;
    }

    @java.lang.SuppressWarnings("all")
    public Boolean getIncludeAllAccounts() {
        return this.includeAllAccounts;
    }

    @java.lang.SuppressWarnings("all")
    public String getDataHash() {
        return this.dataHash;
    }

    @java.lang.SuppressWarnings("all")
    public String getStatus() {
        return this.status;
    }

    @java.lang.SuppressWarnings("all")
    public Integer getTotalTrades() {
        return this.totalTrades;
    }

    @java.lang.SuppressWarnings("all")
    public String getAiResponseJson() {
        return this.aiResponseJson;
    }

    @java.lang.SuppressWarnings("all")
    public String getContextJson() {
        return this.contextJson;
    }

    @java.lang.SuppressWarnings("all")
    public String getTimelineJson() {
        return this.timelineJson;
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
    public void setDays(final Integer days) {
        this.days = days;
    }

    @java.lang.SuppressWarnings("all")
    public void setIncludeAllAccounts(final Boolean includeAllAccounts) {
        this.includeAllAccounts = includeAllAccounts;
    }

    @java.lang.SuppressWarnings("all")
    public void setDataHash(final String dataHash) {
        this.dataHash = dataHash;
    }

    @java.lang.SuppressWarnings("all")
    public void setStatus(final String status) {
        this.status = status;
    }

    @java.lang.SuppressWarnings("all")
    public void setTotalTrades(final Integer totalTrades) {
        this.totalTrades = totalTrades;
    }

    @java.lang.SuppressWarnings("all")
    public void setAiResponseJson(final String aiResponseJson) {
        this.aiResponseJson = aiResponseJson;
    }

    @java.lang.SuppressWarnings("all")
    public void setContextJson(final String contextJson) {
        this.contextJson = contextJson;
    }

    @java.lang.SuppressWarnings("all")
    public void setTimelineJson(final String timelineJson) {
        this.timelineJson = timelineJson;
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
    public void setCreatedAt(final LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    @java.lang.SuppressWarnings("all")
    public void setUpdatedAt(final LocalDateTime updatedAt) {
        this.updatedAt = updatedAt;
    }

    @java.lang.SuppressWarnings("all")
    public TradingDnaSnapshot() {
    }

    @java.lang.SuppressWarnings("all")
    public TradingDnaSnapshot(final Long id, final Long userId, final Long accountId, final Integer days, final Boolean includeAllAccounts, final String dataHash, final String status, final Integer totalTrades, final String aiResponseJson, final String contextJson, final String timelineJson, final String model, final Integer promptTokens, final Integer completionTokens, final Integer totalTokens, final BigDecimal estimatedCost, final LocalDateTime createdAt, final LocalDateTime updatedAt) {
        this.id = id;
        this.userId = userId;
        this.accountId = accountId;
        this.days = days;
        this.includeAllAccounts = includeAllAccounts;
        this.dataHash = dataHash;
        this.status = status;
        this.totalTrades = totalTrades;
        this.aiResponseJson = aiResponseJson;
        this.contextJson = contextJson;
        this.timelineJson = timelineJson;
        this.model = model;
        this.promptTokens = promptTokens;
        this.completionTokens = completionTokens;
        this.totalTokens = totalTokens;
        this.estimatedCost = estimatedCost;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
    }
}
