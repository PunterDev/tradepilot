package com.tradepilot.entity;

import jakarta.persistence.*;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(name = "trade_rule_violations", indexes = {@Index(name = "idx_trade_rule_violations_user_id", columnList = "user_id"), @Index(name = "idx_trade_rule_violations_trade_id", columnList = "trade_id"), @Index(name = "idx_trade_rule_violations_user_trade", columnList = "user_id, trade_id")})
public class TradeRuleViolation {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @Column(name = "user_id", nullable = false)
    private Long userId;
    @Column(nullable = false)
    private Long tradeId;
    @Column(nullable = false, length = 80)
    private String ruleCode;
    @Column(nullable = false)
    private String ruleTitle;
    @Column(nullable = false, length = 50)
    private String category;
    @Column(nullable = false, length = 20)
    private String severity;
    @Column(columnDefinition = "text")
    private String reason;
    @Column(nullable = false, length = 30)
    private String source;
    private LocalDate tradeDate;
    private LocalDateTime createdAt;

    @PrePersist
    void onCreate() {
        createdAt = LocalDateTime.now();
    }


    @java.lang.SuppressWarnings("all")
    public static class TradeRuleViolationBuilder {
        @java.lang.SuppressWarnings("all")
        private Long id;
        @java.lang.SuppressWarnings("all")
        private Long userId;
        @java.lang.SuppressWarnings("all")
        private Long tradeId;
        @java.lang.SuppressWarnings("all")
        private String ruleCode;
        @java.lang.SuppressWarnings("all")
        private String ruleTitle;
        @java.lang.SuppressWarnings("all")
        private String category;
        @java.lang.SuppressWarnings("all")
        private String severity;
        @java.lang.SuppressWarnings("all")
        private String reason;
        @java.lang.SuppressWarnings("all")
        private String source;
        @java.lang.SuppressWarnings("all")
        private LocalDate tradeDate;
        @java.lang.SuppressWarnings("all")
        private LocalDateTime createdAt;

        @java.lang.SuppressWarnings("all")
        TradeRuleViolationBuilder() {
        }

        /**
         * @return {@code this}.
         */
        @java.lang.SuppressWarnings("all")
        public TradeRuleViolation.TradeRuleViolationBuilder id(final Long id) {
            this.id = id;
            return this;
        }

        /**
         * @return {@code this}.
         */
        @java.lang.SuppressWarnings("all")
        public TradeRuleViolation.TradeRuleViolationBuilder userId(final Long userId) {
            this.userId = userId;
            return this;
        }

        /**
         * @return {@code this}.
         */
        @java.lang.SuppressWarnings("all")
        public TradeRuleViolation.TradeRuleViolationBuilder tradeId(final Long tradeId) {
            this.tradeId = tradeId;
            return this;
        }

        /**
         * @return {@code this}.
         */
        @java.lang.SuppressWarnings("all")
        public TradeRuleViolation.TradeRuleViolationBuilder ruleCode(final String ruleCode) {
            this.ruleCode = ruleCode;
            return this;
        }

        /**
         * @return {@code this}.
         */
        @java.lang.SuppressWarnings("all")
        public TradeRuleViolation.TradeRuleViolationBuilder ruleTitle(final String ruleTitle) {
            this.ruleTitle = ruleTitle;
            return this;
        }

        /**
         * @return {@code this}.
         */
        @java.lang.SuppressWarnings("all")
        public TradeRuleViolation.TradeRuleViolationBuilder category(final String category) {
            this.category = category;
            return this;
        }

        /**
         * @return {@code this}.
         */
        @java.lang.SuppressWarnings("all")
        public TradeRuleViolation.TradeRuleViolationBuilder severity(final String severity) {
            this.severity = severity;
            return this;
        }

        /**
         * @return {@code this}.
         */
        @java.lang.SuppressWarnings("all")
        public TradeRuleViolation.TradeRuleViolationBuilder reason(final String reason) {
            this.reason = reason;
            return this;
        }

        /**
         * @return {@code this}.
         */
        @java.lang.SuppressWarnings("all")
        public TradeRuleViolation.TradeRuleViolationBuilder source(final String source) {
            this.source = source;
            return this;
        }

        /**
         * @return {@code this}.
         */
        @java.lang.SuppressWarnings("all")
        public TradeRuleViolation.TradeRuleViolationBuilder tradeDate(final LocalDate tradeDate) {
            this.tradeDate = tradeDate;
            return this;
        }

        /**
         * @return {@code this}.
         */
        @java.lang.SuppressWarnings("all")
        public TradeRuleViolation.TradeRuleViolationBuilder createdAt(final LocalDateTime createdAt) {
            this.createdAt = createdAt;
            return this;
        }

        @java.lang.SuppressWarnings("all")
        public TradeRuleViolation build() {
            return new TradeRuleViolation(this.id, this.userId, this.tradeId, this.ruleCode, this.ruleTitle, this.category, this.severity, this.reason, this.source, this.tradeDate, this.createdAt);
        }

        @java.lang.Override
        @java.lang.SuppressWarnings("all")
        public java.lang.String toString() {
            return "TradeRuleViolation.TradeRuleViolationBuilder(id=" + this.id + ", userId=" + this.userId + ", tradeId=" + this.tradeId + ", ruleCode=" + this.ruleCode + ", ruleTitle=" + this.ruleTitle + ", category=" + this.category + ", severity=" + this.severity + ", reason=" + this.reason + ", source=" + this.source + ", tradeDate=" + this.tradeDate + ", createdAt=" + this.createdAt + ")";
        }
    }

    @java.lang.SuppressWarnings("all")
    public static TradeRuleViolation.TradeRuleViolationBuilder builder() {
        return new TradeRuleViolation.TradeRuleViolationBuilder();
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
    public String getRuleCode() {
        return this.ruleCode;
    }

    @java.lang.SuppressWarnings("all")
    public String getRuleTitle() {
        return this.ruleTitle;
    }

    @java.lang.SuppressWarnings("all")
    public String getCategory() {
        return this.category;
    }

    @java.lang.SuppressWarnings("all")
    public String getSeverity() {
        return this.severity;
    }

    @java.lang.SuppressWarnings("all")
    public String getReason() {
        return this.reason;
    }

    @java.lang.SuppressWarnings("all")
    public String getSource() {
        return this.source;
    }

    @java.lang.SuppressWarnings("all")
    public LocalDate getTradeDate() {
        return this.tradeDate;
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
    public void setTradeId(final Long tradeId) {
        this.tradeId = tradeId;
    }

    @java.lang.SuppressWarnings("all")
    public void setRuleCode(final String ruleCode) {
        this.ruleCode = ruleCode;
    }

    @java.lang.SuppressWarnings("all")
    public void setRuleTitle(final String ruleTitle) {
        this.ruleTitle = ruleTitle;
    }

    @java.lang.SuppressWarnings("all")
    public void setCategory(final String category) {
        this.category = category;
    }

    @java.lang.SuppressWarnings("all")
    public void setSeverity(final String severity) {
        this.severity = severity;
    }

    @java.lang.SuppressWarnings("all")
    public void setReason(final String reason) {
        this.reason = reason;
    }

    @java.lang.SuppressWarnings("all")
    public void setSource(final String source) {
        this.source = source;
    }

    @java.lang.SuppressWarnings("all")
    public void setTradeDate(final LocalDate tradeDate) {
        this.tradeDate = tradeDate;
    }

    @java.lang.SuppressWarnings("all")
    public void setCreatedAt(final LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    @java.lang.SuppressWarnings("all")
    public TradeRuleViolation() {
    }

    @java.lang.SuppressWarnings("all")
    public TradeRuleViolation(final Long id, final Long userId, final Long tradeId, final String ruleCode, final String ruleTitle, final String category, final String severity, final String reason, final String source, final LocalDate tradeDate, final LocalDateTime createdAt) {
        this.id = id;
        this.userId = userId;
        this.tradeId = tradeId;
        this.ruleCode = ruleCode;
        this.ruleTitle = ruleTitle;
        this.category = category;
        this.severity = severity;
        this.reason = reason;
        this.source = source;
        this.tradeDate = tradeDate;
        this.createdAt = createdAt;
    }
}
