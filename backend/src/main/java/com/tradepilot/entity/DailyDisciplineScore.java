package com.tradepilot.entity;

import jakarta.persistence.*;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(name = "daily_discipline_scores", indexes = {@Index(name = "idx_daily_discipline_scores_user_id", columnList = "user_id"), @Index(name = "idx_daily_discipline_scores_user_account_date", columnList = "user_id, account_id, trade_date")})
public class DailyDisciplineScore {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    private Long userId;
    private LocalDate tradeDate;
    private Long accountId;
    private Integer score;
    private Integer totalTrades;
    private Integer violationCount;
    private Integer majorViolationCount;
    private Integer criticalViolationCount;
    private Integer currentCleanDayStreak;
    private Boolean cleanDay;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    @PrePersist
    void onCreate() {
        createdAt = LocalDateTime.now();
        updatedAt = LocalDateTime.now();
    }

    @PreUpdate
    void onUpdate() {
        updatedAt = LocalDateTime.now();
    }


    @java.lang.SuppressWarnings("all")
    public static class DailyDisciplineScoreBuilder {
        @java.lang.SuppressWarnings("all")
        private Long id;
        @java.lang.SuppressWarnings("all")
        private Long userId;
        @java.lang.SuppressWarnings("all")
        private LocalDate tradeDate;
        @java.lang.SuppressWarnings("all")
        private Long accountId;
        @java.lang.SuppressWarnings("all")
        private Integer score;
        @java.lang.SuppressWarnings("all")
        private Integer totalTrades;
        @java.lang.SuppressWarnings("all")
        private Integer violationCount;
        @java.lang.SuppressWarnings("all")
        private Integer majorViolationCount;
        @java.lang.SuppressWarnings("all")
        private Integer criticalViolationCount;
        @java.lang.SuppressWarnings("all")
        private Integer currentCleanDayStreak;
        @java.lang.SuppressWarnings("all")
        private Boolean cleanDay;
        @java.lang.SuppressWarnings("all")
        private LocalDateTime createdAt;
        @java.lang.SuppressWarnings("all")
        private LocalDateTime updatedAt;

        @java.lang.SuppressWarnings("all")
        DailyDisciplineScoreBuilder() {
        }

        /**
         * @return {@code this}.
         */
        @java.lang.SuppressWarnings("all")
        public DailyDisciplineScore.DailyDisciplineScoreBuilder id(final Long id) {
            this.id = id;
            return this;
        }

        /**
         * @return {@code this}.
         */
        @java.lang.SuppressWarnings("all")
        public DailyDisciplineScore.DailyDisciplineScoreBuilder userId(final Long userId) {
            this.userId = userId;
            return this;
        }

        /**
         * @return {@code this}.
         */
        @java.lang.SuppressWarnings("all")
        public DailyDisciplineScore.DailyDisciplineScoreBuilder tradeDate(final LocalDate tradeDate) {
            this.tradeDate = tradeDate;
            return this;
        }

        /**
         * @return {@code this}.
         */
        @java.lang.SuppressWarnings("all")
        public DailyDisciplineScore.DailyDisciplineScoreBuilder accountId(final Long accountId) {
            this.accountId = accountId;
            return this;
        }

        /**
         * @return {@code this}.
         */
        @java.lang.SuppressWarnings("all")
        public DailyDisciplineScore.DailyDisciplineScoreBuilder score(final Integer score) {
            this.score = score;
            return this;
        }

        /**
         * @return {@code this}.
         */
        @java.lang.SuppressWarnings("all")
        public DailyDisciplineScore.DailyDisciplineScoreBuilder totalTrades(final Integer totalTrades) {
            this.totalTrades = totalTrades;
            return this;
        }

        /**
         * @return {@code this}.
         */
        @java.lang.SuppressWarnings("all")
        public DailyDisciplineScore.DailyDisciplineScoreBuilder violationCount(final Integer violationCount) {
            this.violationCount = violationCount;
            return this;
        }

        /**
         * @return {@code this}.
         */
        @java.lang.SuppressWarnings("all")
        public DailyDisciplineScore.DailyDisciplineScoreBuilder majorViolationCount(final Integer majorViolationCount) {
            this.majorViolationCount = majorViolationCount;
            return this;
        }

        /**
         * @return {@code this}.
         */
        @java.lang.SuppressWarnings("all")
        public DailyDisciplineScore.DailyDisciplineScoreBuilder criticalViolationCount(final Integer criticalViolationCount) {
            this.criticalViolationCount = criticalViolationCount;
            return this;
        }

        /**
         * @return {@code this}.
         */
        @java.lang.SuppressWarnings("all")
        public DailyDisciplineScore.DailyDisciplineScoreBuilder currentCleanDayStreak(final Integer currentCleanDayStreak) {
            this.currentCleanDayStreak = currentCleanDayStreak;
            return this;
        }

        /**
         * @return {@code this}.
         */
        @java.lang.SuppressWarnings("all")
        public DailyDisciplineScore.DailyDisciplineScoreBuilder cleanDay(final Boolean cleanDay) {
            this.cleanDay = cleanDay;
            return this;
        }

        /**
         * @return {@code this}.
         */
        @java.lang.SuppressWarnings("all")
        public DailyDisciplineScore.DailyDisciplineScoreBuilder createdAt(final LocalDateTime createdAt) {
            this.createdAt = createdAt;
            return this;
        }

        /**
         * @return {@code this}.
         */
        @java.lang.SuppressWarnings("all")
        public DailyDisciplineScore.DailyDisciplineScoreBuilder updatedAt(final LocalDateTime updatedAt) {
            this.updatedAt = updatedAt;
            return this;
        }

        @java.lang.SuppressWarnings("all")
        public DailyDisciplineScore build() {
            return new DailyDisciplineScore(this.id, this.userId, this.tradeDate, this.accountId, this.score, this.totalTrades, this.violationCount, this.majorViolationCount, this.criticalViolationCount, this.currentCleanDayStreak, this.cleanDay, this.createdAt, this.updatedAt);
        }

        @java.lang.Override
        @java.lang.SuppressWarnings("all")
        public java.lang.String toString() {
            return "DailyDisciplineScore.DailyDisciplineScoreBuilder(id=" + this.id + ", userId=" + this.userId + ", tradeDate=" + this.tradeDate + ", accountId=" + this.accountId + ", score=" + this.score + ", totalTrades=" + this.totalTrades + ", violationCount=" + this.violationCount + ", majorViolationCount=" + this.majorViolationCount + ", criticalViolationCount=" + this.criticalViolationCount + ", currentCleanDayStreak=" + this.currentCleanDayStreak + ", cleanDay=" + this.cleanDay + ", createdAt=" + this.createdAt + ", updatedAt=" + this.updatedAt + ")";
        }
    }

    @java.lang.SuppressWarnings("all")
    public static DailyDisciplineScore.DailyDisciplineScoreBuilder builder() {
        return new DailyDisciplineScore.DailyDisciplineScoreBuilder();
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
    public LocalDate getTradeDate() {
        return this.tradeDate;
    }

    @java.lang.SuppressWarnings("all")
    public Long getAccountId() {
        return this.accountId;
    }

    @java.lang.SuppressWarnings("all")
    public Integer getScore() {
        return this.score;
    }

    @java.lang.SuppressWarnings("all")
    public Integer getTotalTrades() {
        return this.totalTrades;
    }

    @java.lang.SuppressWarnings("all")
    public Integer getViolationCount() {
        return this.violationCount;
    }

    @java.lang.SuppressWarnings("all")
    public Integer getMajorViolationCount() {
        return this.majorViolationCount;
    }

    @java.lang.SuppressWarnings("all")
    public Integer getCriticalViolationCount() {
        return this.criticalViolationCount;
    }

    @java.lang.SuppressWarnings("all")
    public Integer getCurrentCleanDayStreak() {
        return this.currentCleanDayStreak;
    }

    @java.lang.SuppressWarnings("all")
    public Boolean getCleanDay() {
        return this.cleanDay;
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
    public void setTradeDate(final LocalDate tradeDate) {
        this.tradeDate = tradeDate;
    }

    @java.lang.SuppressWarnings("all")
    public void setAccountId(final Long accountId) {
        this.accountId = accountId;
    }

    @java.lang.SuppressWarnings("all")
    public void setScore(final Integer score) {
        this.score = score;
    }

    @java.lang.SuppressWarnings("all")
    public void setTotalTrades(final Integer totalTrades) {
        this.totalTrades = totalTrades;
    }

    @java.lang.SuppressWarnings("all")
    public void setViolationCount(final Integer violationCount) {
        this.violationCount = violationCount;
    }

    @java.lang.SuppressWarnings("all")
    public void setMajorViolationCount(final Integer majorViolationCount) {
        this.majorViolationCount = majorViolationCount;
    }

    @java.lang.SuppressWarnings("all")
    public void setCriticalViolationCount(final Integer criticalViolationCount) {
        this.criticalViolationCount = criticalViolationCount;
    }

    @java.lang.SuppressWarnings("all")
    public void setCurrentCleanDayStreak(final Integer currentCleanDayStreak) {
        this.currentCleanDayStreak = currentCleanDayStreak;
    }

    @java.lang.SuppressWarnings("all")
    public void setCleanDay(final Boolean cleanDay) {
        this.cleanDay = cleanDay;
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
    public DailyDisciplineScore() {
    }

    @java.lang.SuppressWarnings("all")
    public DailyDisciplineScore(final Long id, final Long userId, final LocalDate tradeDate, final Long accountId, final Integer score, final Integer totalTrades, final Integer violationCount, final Integer majorViolationCount, final Integer criticalViolationCount, final Integer currentCleanDayStreak, final Boolean cleanDay, final LocalDateTime createdAt, final LocalDateTime updatedAt) {
        this.id = id;
        this.userId = userId;
        this.tradeDate = tradeDate;
        this.accountId = accountId;
        this.score = score;
        this.totalTrades = totalTrades;
        this.violationCount = violationCount;
        this.majorViolationCount = majorViolationCount;
        this.criticalViolationCount = criticalViolationCount;
        this.currentCleanDayStreak = currentCleanDayStreak;
        this.cleanDay = cleanDay;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
    }
}
