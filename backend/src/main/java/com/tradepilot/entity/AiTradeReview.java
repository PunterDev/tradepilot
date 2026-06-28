package com.tradepilot.entity;

import jakarta.persistence.*;
import java.time.Instant;

@Entity
@Table(name = "ai_trade_reviews", indexes = {@Index(name = "idx_ai_trade_reviews_user_id", columnList = "user_id"), @Index(name = "idx_ai_trade_reviews_trade_id", columnList = "trade_id"), @Index(name = "idx_ai_trade_reviews_cache", columnList = "user_id, trade_id, review_version, review_hash")}, uniqueConstraints = {@UniqueConstraint(name = "uk_ai_trade_reviews_cache", columnNames = {"user_id", "trade_id", "review_version", "review_hash"})})
public class AiTradeReview {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @Column(name = "user_id", nullable = false)
    private Long userId;
    private Long tradeId;
    private Long accountId;
    private String reviewDate;
    private String symbol;
    private Double pnl;
    private String grade;
    private String reviewVersion;
    private String reviewHash;
    @Column(columnDefinition = "TEXT")
    private String summary;
    @Column(columnDefinition = "TEXT")
    private String mistakesJson;
    @Column(columnDefinition = "TEXT")
    private String strengthsJson;
    @Column(columnDefinition = "TEXT")
    private String improvementPlanJson;
    private Instant createdAt;
    private String status; // PENDING, COMPLETED, FAILED
    @Column(columnDefinition = "TEXT")
    private String errorMessage;
    @Column(columnDefinition = "TEXT")
    private String tagsJson;


    @java.lang.SuppressWarnings("all")
    public static class AiTradeReviewBuilder {
        @java.lang.SuppressWarnings("all")
        private Long id;
        @java.lang.SuppressWarnings("all")
        private Long userId;
        @java.lang.SuppressWarnings("all")
        private Long tradeId;
        @java.lang.SuppressWarnings("all")
        private Long accountId;
        @java.lang.SuppressWarnings("all")
        private String reviewDate;
        @java.lang.SuppressWarnings("all")
        private String symbol;
        @java.lang.SuppressWarnings("all")
        private Double pnl;
        @java.lang.SuppressWarnings("all")
        private String grade;
        @java.lang.SuppressWarnings("all")
        private String reviewVersion;
        @java.lang.SuppressWarnings("all")
        private String reviewHash;
        @java.lang.SuppressWarnings("all")
        private String summary;
        @java.lang.SuppressWarnings("all")
        private String mistakesJson;
        @java.lang.SuppressWarnings("all")
        private String strengthsJson;
        @java.lang.SuppressWarnings("all")
        private String improvementPlanJson;
        @java.lang.SuppressWarnings("all")
        private Instant createdAt;
        @java.lang.SuppressWarnings("all")
        private String status;
        @java.lang.SuppressWarnings("all")
        private String errorMessage;
        @java.lang.SuppressWarnings("all")
        private String tagsJson;

        @java.lang.SuppressWarnings("all")
        AiTradeReviewBuilder() {
        }

        /**
         * @return {@code this}.
         */
        @java.lang.SuppressWarnings("all")
        public AiTradeReview.AiTradeReviewBuilder id(final Long id) {
            this.id = id;
            return this;
        }

        /**
         * @return {@code this}.
         */
        @java.lang.SuppressWarnings("all")
        public AiTradeReview.AiTradeReviewBuilder userId(final Long userId) {
            this.userId = userId;
            return this;
        }

        /**
         * @return {@code this}.
         */
        @java.lang.SuppressWarnings("all")
        public AiTradeReview.AiTradeReviewBuilder tradeId(final Long tradeId) {
            this.tradeId = tradeId;
            return this;
        }

        /**
         * @return {@code this}.
         */
        @java.lang.SuppressWarnings("all")
        public AiTradeReview.AiTradeReviewBuilder accountId(final Long accountId) {
            this.accountId = accountId;
            return this;
        }

        /**
         * @return {@code this}.
         */
        @java.lang.SuppressWarnings("all")
        public AiTradeReview.AiTradeReviewBuilder reviewDate(final String reviewDate) {
            this.reviewDate = reviewDate;
            return this;
        }

        /**
         * @return {@code this}.
         */
        @java.lang.SuppressWarnings("all")
        public AiTradeReview.AiTradeReviewBuilder symbol(final String symbol) {
            this.symbol = symbol;
            return this;
        }

        /**
         * @return {@code this}.
         */
        @java.lang.SuppressWarnings("all")
        public AiTradeReview.AiTradeReviewBuilder pnl(final Double pnl) {
            this.pnl = pnl;
            return this;
        }

        /**
         * @return {@code this}.
         */
        @java.lang.SuppressWarnings("all")
        public AiTradeReview.AiTradeReviewBuilder grade(final String grade) {
            this.grade = grade;
            return this;
        }

        /**
         * @return {@code this}.
         */
        @java.lang.SuppressWarnings("all")
        public AiTradeReview.AiTradeReviewBuilder reviewVersion(final String reviewVersion) {
            this.reviewVersion = reviewVersion;
            return this;
        }

        /**
         * @return {@code this}.
         */
        @java.lang.SuppressWarnings("all")
        public AiTradeReview.AiTradeReviewBuilder reviewHash(final String reviewHash) {
            this.reviewHash = reviewHash;
            return this;
        }

        /**
         * @return {@code this}.
         */
        @java.lang.SuppressWarnings("all")
        public AiTradeReview.AiTradeReviewBuilder summary(final String summary) {
            this.summary = summary;
            return this;
        }

        /**
         * @return {@code this}.
         */
        @java.lang.SuppressWarnings("all")
        public AiTradeReview.AiTradeReviewBuilder mistakesJson(final String mistakesJson) {
            this.mistakesJson = mistakesJson;
            return this;
        }

        /**
         * @return {@code this}.
         */
        @java.lang.SuppressWarnings("all")
        public AiTradeReview.AiTradeReviewBuilder strengthsJson(final String strengthsJson) {
            this.strengthsJson = strengthsJson;
            return this;
        }

        /**
         * @return {@code this}.
         */
        @java.lang.SuppressWarnings("all")
        public AiTradeReview.AiTradeReviewBuilder improvementPlanJson(final String improvementPlanJson) {
            this.improvementPlanJson = improvementPlanJson;
            return this;
        }

        /**
         * @return {@code this}.
         */
        @java.lang.SuppressWarnings("all")
        public AiTradeReview.AiTradeReviewBuilder createdAt(final Instant createdAt) {
            this.createdAt = createdAt;
            return this;
        }

        /**
         * @return {@code this}.
         */
        @java.lang.SuppressWarnings("all")
        public AiTradeReview.AiTradeReviewBuilder status(final String status) {
            this.status = status;
            return this;
        }

        /**
         * @return {@code this}.
         */
        @java.lang.SuppressWarnings("all")
        public AiTradeReview.AiTradeReviewBuilder errorMessage(final String errorMessage) {
            this.errorMessage = errorMessage;
            return this;
        }

        /**
         * @return {@code this}.
         */
        @java.lang.SuppressWarnings("all")
        public AiTradeReview.AiTradeReviewBuilder tagsJson(final String tagsJson) {
            this.tagsJson = tagsJson;
            return this;
        }

        @java.lang.SuppressWarnings("all")
        public AiTradeReview build() {
            return new AiTradeReview(this.id, this.userId, this.tradeId, this.accountId, this.reviewDate, this.symbol, this.pnl, this.grade, this.reviewVersion, this.reviewHash, this.summary, this.mistakesJson, this.strengthsJson, this.improvementPlanJson, this.createdAt, this.status, this.errorMessage, this.tagsJson);
        }

        @java.lang.Override
        @java.lang.SuppressWarnings("all")
        public java.lang.String toString() {
            return "AiTradeReview.AiTradeReviewBuilder(id=" + this.id + ", userId=" + this.userId + ", tradeId=" + this.tradeId + ", accountId=" + this.accountId + ", reviewDate=" + this.reviewDate + ", symbol=" + this.symbol + ", pnl=" + this.pnl + ", grade=" + this.grade + ", reviewVersion=" + this.reviewVersion + ", reviewHash=" + this.reviewHash + ", summary=" + this.summary + ", mistakesJson=" + this.mistakesJson + ", strengthsJson=" + this.strengthsJson + ", improvementPlanJson=" + this.improvementPlanJson + ", createdAt=" + this.createdAt + ", status=" + this.status + ", errorMessage=" + this.errorMessage + ", tagsJson=" + this.tagsJson + ")";
        }
    }

    @java.lang.SuppressWarnings("all")
    public static AiTradeReview.AiTradeReviewBuilder builder() {
        return new AiTradeReview.AiTradeReviewBuilder();
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
    public Long getTradeId() {
        return this.tradeId;
    }

    @java.lang.SuppressWarnings("all")
    public Long getAccountId() {
        return this.accountId;
    }

    @java.lang.SuppressWarnings("all")
    public String getReviewDate() {
        return this.reviewDate;
    }

    @java.lang.SuppressWarnings("all")
    public String getSymbol() {
        return this.symbol;
    }

    @java.lang.SuppressWarnings("all")
    public Double getPnl() {
        return this.pnl;
    }

    @java.lang.SuppressWarnings("all")
    public String getGrade() {
        return this.grade;
    }

    @java.lang.SuppressWarnings("all")
    public String getReviewVersion() {
        return this.reviewVersion;
    }

    @java.lang.SuppressWarnings("all")
    public String getReviewHash() {
        return this.reviewHash;
    }

    @java.lang.SuppressWarnings("all")
    public String getSummary() {
        return this.summary;
    }

    @java.lang.SuppressWarnings("all")
    public String getMistakesJson() {
        return this.mistakesJson;
    }

    @java.lang.SuppressWarnings("all")
    public String getStrengthsJson() {
        return this.strengthsJson;
    }

    @java.lang.SuppressWarnings("all")
    public String getImprovementPlanJson() {
        return this.improvementPlanJson;
    }

    @java.lang.SuppressWarnings("all")
    public Instant getCreatedAt() {
        return this.createdAt;
    }

    @java.lang.SuppressWarnings("all")
    public String getStatus() {
        return this.status;
    }

    @java.lang.SuppressWarnings("all")
    public String getErrorMessage() {
        return this.errorMessage;
    }

    @java.lang.SuppressWarnings("all")
    public String getTagsJson() {
        return this.tagsJson;
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
    public void setTradeId(final Long tradeId) {
        this.tradeId = tradeId;
    }

    @java.lang.SuppressWarnings("all")
    public void setAccountId(final Long accountId) {
        this.accountId = accountId;
    }

    @java.lang.SuppressWarnings("all")
    public void setReviewDate(final String reviewDate) {
        this.reviewDate = reviewDate;
    }

    @java.lang.SuppressWarnings("all")
    public void setSymbol(final String symbol) {
        this.symbol = symbol;
    }

    @java.lang.SuppressWarnings("all")
    public void setPnl(final Double pnl) {
        this.pnl = pnl;
    }

    @java.lang.SuppressWarnings("all")
    public void setGrade(final String grade) {
        this.grade = grade;
    }

    @java.lang.SuppressWarnings("all")
    public void setReviewVersion(final String reviewVersion) {
        this.reviewVersion = reviewVersion;
    }

    @java.lang.SuppressWarnings("all")
    public void setReviewHash(final String reviewHash) {
        this.reviewHash = reviewHash;
    }

    @java.lang.SuppressWarnings("all")
    public void setSummary(final String summary) {
        this.summary = summary;
    }

    @java.lang.SuppressWarnings("all")
    public void setMistakesJson(final String mistakesJson) {
        this.mistakesJson = mistakesJson;
    }

    @java.lang.SuppressWarnings("all")
    public void setStrengthsJson(final String strengthsJson) {
        this.strengthsJson = strengthsJson;
    }

    @java.lang.SuppressWarnings("all")
    public void setImprovementPlanJson(final String improvementPlanJson) {
        this.improvementPlanJson = improvementPlanJson;
    }

    @java.lang.SuppressWarnings("all")
    public void setCreatedAt(final Instant createdAt) {
        this.createdAt = createdAt;
    }

    @java.lang.SuppressWarnings("all")
    public void setStatus(final String status) {
        this.status = status;
    }

    @java.lang.SuppressWarnings("all")
    public void setErrorMessage(final String errorMessage) {
        this.errorMessage = errorMessage;
    }

    @java.lang.SuppressWarnings("all")
    public void setTagsJson(final String tagsJson) {
        this.tagsJson = tagsJson;
    }

    @java.lang.SuppressWarnings("all")
    public AiTradeReview() {
    }

    @java.lang.SuppressWarnings("all")
    public AiTradeReview(final Long id, final Long userId, final Long tradeId, final Long accountId, final String reviewDate, final String symbol, final Double pnl, final String grade, final String reviewVersion, final String reviewHash, final String summary, final String mistakesJson, final String strengthsJson, final String improvementPlanJson, final Instant createdAt, final String status, final String errorMessage, final String tagsJson) {
        this.id = id;
        this.userId = userId;
        this.tradeId = tradeId;
        this.accountId = accountId;
        this.reviewDate = reviewDate;
        this.symbol = symbol;
        this.pnl = pnl;
        this.grade = grade;
        this.reviewVersion = reviewVersion;
        this.reviewHash = reviewHash;
        this.summary = summary;
        this.mistakesJson = mistakesJson;
        this.strengthsJson = strengthsJson;
        this.improvementPlanJson = improvementPlanJson;
        this.createdAt = createdAt;
        this.status = status;
        this.errorMessage = errorMessage;
        this.tagsJson = tagsJson;
    }
}
