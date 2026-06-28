package com.tradepilot.entity;

import jakarta.persistence.*;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(name = "ai_session_reviews", uniqueConstraints = {@UniqueConstraint(name = "uk_ai_session_reviews_user_account_date", columnNames = {"user_id", "account_id", "session_date"})}, indexes = {@Index(name = "idx_ai_session_reviews_user_id", columnList = "user_id"), @Index(name = "idx_ai_session_reviews_user_account_date", columnList = "user_id, account_id, session_date"), @Index(name = "idx_ai_session_reviews_user_account_date_hash", columnList = "user_id, account_id, session_date, review_hash")})
public class AiSessionReview {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @Column(name = "user_id", nullable = false)
    private Long userId;
    @Column(name = "account_id", nullable = false)
    private Long accountId;
    @Column(name = "session_date", nullable = false)
    private LocalDate sessionDate;
    @Column(name = "review_hash", nullable = false, length = 64)
    private String reviewHash;
    @Column(columnDefinition = "text")
    private String summary;
    @Column(name = "what_went_well", columnDefinition = "text")
    private String whatWentWell;
    @Column(name = "what_hurt_performance", columnDefinition = "text")
    private String whatHurtPerformance;
    @Column(name = "biggest_mistake", columnDefinition = "text")
    private String biggestMistake;
    @Column(name = "best_setup", columnDefinition = "text")
    private String bestSetup;
    @Column(name = "worst_decision", columnDefinition = "text")
    private String worstDecision;
    @Column(name = "emotional_pattern", columnDefinition = "text")
    private String emotionalPattern;
    @Column(name = "execution_pattern", columnDefinition = "text")
    private String executionPattern;
    @Column(name = "risk_management_pattern", columnDefinition = "text")
    private String riskManagementPattern;
    @Column(name = "one_rule_for_tomorrow", columnDefinition = "text")
    private String oneRuleForTomorrow;
    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    @PrePersist
    void onCreate() {
        if (createdAt == null) {
            createdAt = LocalDateTime.now();
        }
    }


    @java.lang.SuppressWarnings("all")
    public static class AiSessionReviewBuilder {
        @java.lang.SuppressWarnings("all")
        private Long id;
        @java.lang.SuppressWarnings("all")
        private Long userId;
        @java.lang.SuppressWarnings("all")
        private Long accountId;
        @java.lang.SuppressWarnings("all")
        private LocalDate sessionDate;
        @java.lang.SuppressWarnings("all")
        private String reviewHash;
        @java.lang.SuppressWarnings("all")
        private String summary;
        @java.lang.SuppressWarnings("all")
        private String whatWentWell;
        @java.lang.SuppressWarnings("all")
        private String whatHurtPerformance;
        @java.lang.SuppressWarnings("all")
        private String biggestMistake;
        @java.lang.SuppressWarnings("all")
        private String bestSetup;
        @java.lang.SuppressWarnings("all")
        private String worstDecision;
        @java.lang.SuppressWarnings("all")
        private String emotionalPattern;
        @java.lang.SuppressWarnings("all")
        private String executionPattern;
        @java.lang.SuppressWarnings("all")
        private String riskManagementPattern;
        @java.lang.SuppressWarnings("all")
        private String oneRuleForTomorrow;
        @java.lang.SuppressWarnings("all")
        private LocalDateTime createdAt;

        @java.lang.SuppressWarnings("all")
        AiSessionReviewBuilder() {
        }

        /**
         * @return {@code this}.
         */
        @java.lang.SuppressWarnings("all")
        public AiSessionReview.AiSessionReviewBuilder id(final Long id) {
            this.id = id;
            return this;
        }

        /**
         * @return {@code this}.
         */
        @java.lang.SuppressWarnings("all")
        public AiSessionReview.AiSessionReviewBuilder userId(final Long userId) {
            this.userId = userId;
            return this;
        }

        /**
         * @return {@code this}.
         */
        @java.lang.SuppressWarnings("all")
        public AiSessionReview.AiSessionReviewBuilder accountId(final Long accountId) {
            this.accountId = accountId;
            return this;
        }

        /**
         * @return {@code this}.
         */
        @java.lang.SuppressWarnings("all")
        public AiSessionReview.AiSessionReviewBuilder sessionDate(final LocalDate sessionDate) {
            this.sessionDate = sessionDate;
            return this;
        }

        /**
         * @return {@code this}.
         */
        @java.lang.SuppressWarnings("all")
        public AiSessionReview.AiSessionReviewBuilder reviewHash(final String reviewHash) {
            this.reviewHash = reviewHash;
            return this;
        }

        /**
         * @return {@code this}.
         */
        @java.lang.SuppressWarnings("all")
        public AiSessionReview.AiSessionReviewBuilder summary(final String summary) {
            this.summary = summary;
            return this;
        }

        /**
         * @return {@code this}.
         */
        @java.lang.SuppressWarnings("all")
        public AiSessionReview.AiSessionReviewBuilder whatWentWell(final String whatWentWell) {
            this.whatWentWell = whatWentWell;
            return this;
        }

        /**
         * @return {@code this}.
         */
        @java.lang.SuppressWarnings("all")
        public AiSessionReview.AiSessionReviewBuilder whatHurtPerformance(final String whatHurtPerformance) {
            this.whatHurtPerformance = whatHurtPerformance;
            return this;
        }

        /**
         * @return {@code this}.
         */
        @java.lang.SuppressWarnings("all")
        public AiSessionReview.AiSessionReviewBuilder biggestMistake(final String biggestMistake) {
            this.biggestMistake = biggestMistake;
            return this;
        }

        /**
         * @return {@code this}.
         */
        @java.lang.SuppressWarnings("all")
        public AiSessionReview.AiSessionReviewBuilder bestSetup(final String bestSetup) {
            this.bestSetup = bestSetup;
            return this;
        }

        /**
         * @return {@code this}.
         */
        @java.lang.SuppressWarnings("all")
        public AiSessionReview.AiSessionReviewBuilder worstDecision(final String worstDecision) {
            this.worstDecision = worstDecision;
            return this;
        }

        /**
         * @return {@code this}.
         */
        @java.lang.SuppressWarnings("all")
        public AiSessionReview.AiSessionReviewBuilder emotionalPattern(final String emotionalPattern) {
            this.emotionalPattern = emotionalPattern;
            return this;
        }

        /**
         * @return {@code this}.
         */
        @java.lang.SuppressWarnings("all")
        public AiSessionReview.AiSessionReviewBuilder executionPattern(final String executionPattern) {
            this.executionPattern = executionPattern;
            return this;
        }

        /**
         * @return {@code this}.
         */
        @java.lang.SuppressWarnings("all")
        public AiSessionReview.AiSessionReviewBuilder riskManagementPattern(final String riskManagementPattern) {
            this.riskManagementPattern = riskManagementPattern;
            return this;
        }

        /**
         * @return {@code this}.
         */
        @java.lang.SuppressWarnings("all")
        public AiSessionReview.AiSessionReviewBuilder oneRuleForTomorrow(final String oneRuleForTomorrow) {
            this.oneRuleForTomorrow = oneRuleForTomorrow;
            return this;
        }

        /**
         * @return {@code this}.
         */
        @java.lang.SuppressWarnings("all")
        public AiSessionReview.AiSessionReviewBuilder createdAt(final LocalDateTime createdAt) {
            this.createdAt = createdAt;
            return this;
        }

        @java.lang.SuppressWarnings("all")
        public AiSessionReview build() {
            return new AiSessionReview(this.id, this.userId, this.accountId, this.sessionDate, this.reviewHash, this.summary, this.whatWentWell, this.whatHurtPerformance, this.biggestMistake, this.bestSetup, this.worstDecision, this.emotionalPattern, this.executionPattern, this.riskManagementPattern, this.oneRuleForTomorrow, this.createdAt);
        }

        @java.lang.Override
        @java.lang.SuppressWarnings("all")
        public java.lang.String toString() {
            return "AiSessionReview.AiSessionReviewBuilder(id=" + this.id + ", userId=" + this.userId + ", accountId=" + this.accountId + ", sessionDate=" + this.sessionDate + ", reviewHash=" + this.reviewHash + ", summary=" + this.summary + ", whatWentWell=" + this.whatWentWell + ", whatHurtPerformance=" + this.whatHurtPerformance + ", biggestMistake=" + this.biggestMistake + ", bestSetup=" + this.bestSetup + ", worstDecision=" + this.worstDecision + ", emotionalPattern=" + this.emotionalPattern + ", executionPattern=" + this.executionPattern + ", riskManagementPattern=" + this.riskManagementPattern + ", oneRuleForTomorrow=" + this.oneRuleForTomorrow + ", createdAt=" + this.createdAt + ")";
        }
    }

    @java.lang.SuppressWarnings("all")
    public static AiSessionReview.AiSessionReviewBuilder builder() {
        return new AiSessionReview.AiSessionReviewBuilder();
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
    public LocalDate getSessionDate() {
        return this.sessionDate;
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
    public String getWhatWentWell() {
        return this.whatWentWell;
    }

    @java.lang.SuppressWarnings("all")
    public String getWhatHurtPerformance() {
        return this.whatHurtPerformance;
    }

    @java.lang.SuppressWarnings("all")
    public String getBiggestMistake() {
        return this.biggestMistake;
    }

    @java.lang.SuppressWarnings("all")
    public String getBestSetup() {
        return this.bestSetup;
    }

    @java.lang.SuppressWarnings("all")
    public String getWorstDecision() {
        return this.worstDecision;
    }

    @java.lang.SuppressWarnings("all")
    public String getEmotionalPattern() {
        return this.emotionalPattern;
    }

    @java.lang.SuppressWarnings("all")
    public String getExecutionPattern() {
        return this.executionPattern;
    }

    @java.lang.SuppressWarnings("all")
    public String getRiskManagementPattern() {
        return this.riskManagementPattern;
    }

    @java.lang.SuppressWarnings("all")
    public String getOneRuleForTomorrow() {
        return this.oneRuleForTomorrow;
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
    public void setAccountId(final Long accountId) {
        this.accountId = accountId;
    }

    @java.lang.SuppressWarnings("all")
    public void setSessionDate(final LocalDate sessionDate) {
        this.sessionDate = sessionDate;
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
    public void setWhatWentWell(final String whatWentWell) {
        this.whatWentWell = whatWentWell;
    }

    @java.lang.SuppressWarnings("all")
    public void setWhatHurtPerformance(final String whatHurtPerformance) {
        this.whatHurtPerformance = whatHurtPerformance;
    }

    @java.lang.SuppressWarnings("all")
    public void setBiggestMistake(final String biggestMistake) {
        this.biggestMistake = biggestMistake;
    }

    @java.lang.SuppressWarnings("all")
    public void setBestSetup(final String bestSetup) {
        this.bestSetup = bestSetup;
    }

    @java.lang.SuppressWarnings("all")
    public void setWorstDecision(final String worstDecision) {
        this.worstDecision = worstDecision;
    }

    @java.lang.SuppressWarnings("all")
    public void setEmotionalPattern(final String emotionalPattern) {
        this.emotionalPattern = emotionalPattern;
    }

    @java.lang.SuppressWarnings("all")
    public void setExecutionPattern(final String executionPattern) {
        this.executionPattern = executionPattern;
    }

    @java.lang.SuppressWarnings("all")
    public void setRiskManagementPattern(final String riskManagementPattern) {
        this.riskManagementPattern = riskManagementPattern;
    }

    @java.lang.SuppressWarnings("all")
    public void setOneRuleForTomorrow(final String oneRuleForTomorrow) {
        this.oneRuleForTomorrow = oneRuleForTomorrow;
    }

    @java.lang.SuppressWarnings("all")
    public void setCreatedAt(final LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    @java.lang.SuppressWarnings("all")
    public AiSessionReview() {
    }

    @java.lang.SuppressWarnings("all")
    public AiSessionReview(final Long id, final Long userId, final Long accountId, final LocalDate sessionDate, final String reviewHash, final String summary, final String whatWentWell, final String whatHurtPerformance, final String biggestMistake, final String bestSetup, final String worstDecision, final String emotionalPattern, final String executionPattern, final String riskManagementPattern, final String oneRuleForTomorrow, final LocalDateTime createdAt) {
        this.id = id;
        this.userId = userId;
        this.accountId = accountId;
        this.sessionDate = sessionDate;
        this.reviewHash = reviewHash;
        this.summary = summary;
        this.whatWentWell = whatWentWell;
        this.whatHurtPerformance = whatHurtPerformance;
        this.biggestMistake = biggestMistake;
        this.bestSetup = bestSetup;
        this.worstDecision = worstDecision;
        this.emotionalPattern = emotionalPattern;
        this.executionPattern = executionPattern;
        this.riskManagementPattern = riskManagementPattern;
        this.oneRuleForTomorrow = oneRuleForTomorrow;
        this.createdAt = createdAt;
    }
}
