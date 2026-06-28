package com.tradepilot.entity;

import com.tradepilot.enums.*;
import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(name = "trades", indexes = {@Index(name = "idx_trades_user_id", columnList = "user_id"), @Index(name = "idx_trades_account_id", columnList = "account_id"), @Index(name = "idx_trades_user_account_date", columnList = "user_id, account_id, trade_date"), @Index(name = "idx_trades_user_account_date_entry", columnList = "user_id, account_id, trade_date, entry_time"), @Index(name = "idx_trades_user_entry_time", columnList = "user_id, entry_time"), @Index(name = "idx_trades_account_entry_time", columnList = "account_id, entry_time"), @Index(name = "idx_trades_user_account_entry_time", columnList = "user_id, account_id, entry_time")}, uniqueConstraints = {@UniqueConstraint(name = "uk_trades_user_account_external_key", columnNames = {"user_id", "account_id", "external_trade_key"})})
public class Trade {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;
    @Column(nullable = false, length = 20)
    private String symbol;
    @Column(nullable = false, length = 20)
    private String market;
    @Column(name = "trade_date", nullable = false)
    private LocalDate tradeDate;
    @Column(name = "entry_time")
    private LocalDateTime entryTime;
    @Column(name = "exit_time")
    private LocalDateTime exitTime;
    @Column(nullable = false, length = 20)
    private String side;
    @Enumerated(EnumType.STRING)
    @Column(length = 50)
    private SetupType setup;
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private SessionType session;
    @Column(name = "entry_price", precision = 18, scale = 6)
    private BigDecimal entryPrice;
    @Column(name = "stop_price", precision = 18, scale = 6)
    private BigDecimal stopPrice;
    @Column(name = "target_price", precision = 18, scale = 6)
    private BigDecimal targetPrice;
    @Column(name = "exit_price", precision = 18, scale = 6)
    private BigDecimal exitPrice;
    private Integer quantity;
    @Column(name = "risk_amount", precision = 18, scale = 2)
    private BigDecimal riskAmount;
    @Column(name = "pnl_amount", precision = 18, scale = 2)
    private BigDecimal pnlAmount;
    @Column(name = "pnl_r", precision = 10, scale = 2)
    private BigDecimal pnlR;
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private OutcomeType outcome;
    @Column(name = "confidence_rating")
    private Integer confidenceRating;
    @Column(name = "confidence")
    private Integer confidence;
    @Column(name = "emotion", length = 50)
    private String emotion;
    @Column(name = "execution_rating")
    private Integer executionRating;
    @Column(name = "followed_plan")
    private Boolean followedPlan;
    @Enumerated(EnumType.STRING)
    @Column(name = "mistake_tag", length = 50)
    private MistakeTag mistakeTag;
    @Column(columnDefinition = "text")
    private String notes;
    @Column(name = "psychology_notes", columnDefinition = "text")
    private String psychologyNotes;
    @Column(name = "lesson_learned", columnDefinition = "text")
    private String lessonLearned;
    @Column(name = "screenshot_url", length = 500)
    private String screenshotUrl;
    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;
    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    @PrePersist
    public void onCreate() {
        createdAt = LocalDateTime.now();
        updatedAt = LocalDateTime.now();
    }

    @PreUpdate
    public void onUpdate() {
        updatedAt = LocalDateTime.now();
    }

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "account_id")
    private TradingAccount account;
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "import_batch_id")
    private ImportBatch importBatch;
    @Column(name = "external_trade_key")
    private String externalTradeKey;


    @java.lang.SuppressWarnings("all")
    public static class TradeBuilder {
        @java.lang.SuppressWarnings("all")
        private Long id;
        @java.lang.SuppressWarnings("all")
        private User user;
        @java.lang.SuppressWarnings("all")
        private String symbol;
        @java.lang.SuppressWarnings("all")
        private String market;
        @java.lang.SuppressWarnings("all")
        private LocalDate tradeDate;
        @java.lang.SuppressWarnings("all")
        private LocalDateTime entryTime;
        @java.lang.SuppressWarnings("all")
        private LocalDateTime exitTime;
        @java.lang.SuppressWarnings("all")
        private String side;
        @java.lang.SuppressWarnings("all")
        private SetupType setup;
        @java.lang.SuppressWarnings("all")
        private SessionType session;
        @java.lang.SuppressWarnings("all")
        private BigDecimal entryPrice;
        @java.lang.SuppressWarnings("all")
        private BigDecimal stopPrice;
        @java.lang.SuppressWarnings("all")
        private BigDecimal targetPrice;
        @java.lang.SuppressWarnings("all")
        private BigDecimal exitPrice;
        @java.lang.SuppressWarnings("all")
        private Integer quantity;
        @java.lang.SuppressWarnings("all")
        private BigDecimal riskAmount;
        @java.lang.SuppressWarnings("all")
        private BigDecimal pnlAmount;
        @java.lang.SuppressWarnings("all")
        private BigDecimal pnlR;
        @java.lang.SuppressWarnings("all")
        private OutcomeType outcome;
        @java.lang.SuppressWarnings("all")
        private Integer confidenceRating;
        @java.lang.SuppressWarnings("all")
        private Integer confidence;
        @java.lang.SuppressWarnings("all")
        private String emotion;
        @java.lang.SuppressWarnings("all")
        private Integer executionRating;
        @java.lang.SuppressWarnings("all")
        private Boolean followedPlan;
        @java.lang.SuppressWarnings("all")
        private MistakeTag mistakeTag;
        @java.lang.SuppressWarnings("all")
        private String notes;
        @java.lang.SuppressWarnings("all")
        private String psychologyNotes;
        @java.lang.SuppressWarnings("all")
        private String lessonLearned;
        @java.lang.SuppressWarnings("all")
        private String screenshotUrl;
        @java.lang.SuppressWarnings("all")
        private LocalDateTime createdAt;
        @java.lang.SuppressWarnings("all")
        private LocalDateTime updatedAt;
        @java.lang.SuppressWarnings("all")
        private TradingAccount account;
        @java.lang.SuppressWarnings("all")
        private ImportBatch importBatch;
        @java.lang.SuppressWarnings("all")
        private String externalTradeKey;

        @java.lang.SuppressWarnings("all")
        TradeBuilder() {
        }

        /**
         * @return {@code this}.
         */
        @java.lang.SuppressWarnings("all")
        public Trade.TradeBuilder id(final Long id) {
            this.id = id;
            return this;
        }

        /**
         * @return {@code this}.
         */
        @java.lang.SuppressWarnings("all")
        public Trade.TradeBuilder user(final User user) {
            this.user = user;
            return this;
        }

        /**
         * @return {@code this}.
         */
        @java.lang.SuppressWarnings("all")
        public Trade.TradeBuilder symbol(final String symbol) {
            this.symbol = symbol;
            return this;
        }

        /**
         * @return {@code this}.
         */
        @java.lang.SuppressWarnings("all")
        public Trade.TradeBuilder market(final String market) {
            this.market = market;
            return this;
        }

        /**
         * @return {@code this}.
         */
        @java.lang.SuppressWarnings("all")
        public Trade.TradeBuilder tradeDate(final LocalDate tradeDate) {
            this.tradeDate = tradeDate;
            return this;
        }

        /**
         * @return {@code this}.
         */
        @java.lang.SuppressWarnings("all")
        public Trade.TradeBuilder entryTime(final LocalDateTime entryTime) {
            this.entryTime = entryTime;
            return this;
        }

        /**
         * @return {@code this}.
         */
        @java.lang.SuppressWarnings("all")
        public Trade.TradeBuilder exitTime(final LocalDateTime exitTime) {
            this.exitTime = exitTime;
            return this;
        }

        /**
         * @return {@code this}.
         */
        @java.lang.SuppressWarnings("all")
        public Trade.TradeBuilder side(final String side) {
            this.side = side;
            return this;
        }

        /**
         * @return {@code this}.
         */
        @java.lang.SuppressWarnings("all")
        public Trade.TradeBuilder setup(final SetupType setup) {
            this.setup = setup;
            return this;
        }

        /**
         * @return {@code this}.
         */
        @java.lang.SuppressWarnings("all")
        public Trade.TradeBuilder session(final SessionType session) {
            this.session = session;
            return this;
        }

        /**
         * @return {@code this}.
         */
        @java.lang.SuppressWarnings("all")
        public Trade.TradeBuilder entryPrice(final BigDecimal entryPrice) {
            this.entryPrice = entryPrice;
            return this;
        }

        /**
         * @return {@code this}.
         */
        @java.lang.SuppressWarnings("all")
        public Trade.TradeBuilder stopPrice(final BigDecimal stopPrice) {
            this.stopPrice = stopPrice;
            return this;
        }

        /**
         * @return {@code this}.
         */
        @java.lang.SuppressWarnings("all")
        public Trade.TradeBuilder targetPrice(final BigDecimal targetPrice) {
            this.targetPrice = targetPrice;
            return this;
        }

        /**
         * @return {@code this}.
         */
        @java.lang.SuppressWarnings("all")
        public Trade.TradeBuilder exitPrice(final BigDecimal exitPrice) {
            this.exitPrice = exitPrice;
            return this;
        }

        /**
         * @return {@code this}.
         */
        @java.lang.SuppressWarnings("all")
        public Trade.TradeBuilder quantity(final Integer quantity) {
            this.quantity = quantity;
            return this;
        }

        /**
         * @return {@code this}.
         */
        @java.lang.SuppressWarnings("all")
        public Trade.TradeBuilder riskAmount(final BigDecimal riskAmount) {
            this.riskAmount = riskAmount;
            return this;
        }

        /**
         * @return {@code this}.
         */
        @java.lang.SuppressWarnings("all")
        public Trade.TradeBuilder pnlAmount(final BigDecimal pnlAmount) {
            this.pnlAmount = pnlAmount;
            return this;
        }

        /**
         * @return {@code this}.
         */
        @java.lang.SuppressWarnings("all")
        public Trade.TradeBuilder pnlR(final BigDecimal pnlR) {
            this.pnlR = pnlR;
            return this;
        }

        /**
         * @return {@code this}.
         */
        @java.lang.SuppressWarnings("all")
        public Trade.TradeBuilder outcome(final OutcomeType outcome) {
            this.outcome = outcome;
            return this;
        }

        /**
         * @return {@code this}.
         */
        @java.lang.SuppressWarnings("all")
        public Trade.TradeBuilder confidenceRating(final Integer confidenceRating) {
            this.confidenceRating = confidenceRating;
            return this;
        }

        /**
         * @return {@code this}.
         */
        @java.lang.SuppressWarnings("all")
        public Trade.TradeBuilder confidence(final Integer confidence) {
            this.confidence = confidence;
            return this;
        }

        /**
         * @return {@code this}.
         */
        @java.lang.SuppressWarnings("all")
        public Trade.TradeBuilder emotion(final String emotion) {
            this.emotion = emotion;
            return this;
        }

        /**
         * @return {@code this}.
         */
        @java.lang.SuppressWarnings("all")
        public Trade.TradeBuilder executionRating(final Integer executionRating) {
            this.executionRating = executionRating;
            return this;
        }

        /**
         * @return {@code this}.
         */
        @java.lang.SuppressWarnings("all")
        public Trade.TradeBuilder followedPlan(final Boolean followedPlan) {
            this.followedPlan = followedPlan;
            return this;
        }

        /**
         * @return {@code this}.
         */
        @java.lang.SuppressWarnings("all")
        public Trade.TradeBuilder mistakeTag(final MistakeTag mistakeTag) {
            this.mistakeTag = mistakeTag;
            return this;
        }

        /**
         * @return {@code this}.
         */
        @java.lang.SuppressWarnings("all")
        public Trade.TradeBuilder notes(final String notes) {
            this.notes = notes;
            return this;
        }

        /**
         * @return {@code this}.
         */
        @java.lang.SuppressWarnings("all")
        public Trade.TradeBuilder psychologyNotes(final String psychologyNotes) {
            this.psychologyNotes = psychologyNotes;
            return this;
        }

        /**
         * @return {@code this}.
         */
        @java.lang.SuppressWarnings("all")
        public Trade.TradeBuilder lessonLearned(final String lessonLearned) {
            this.lessonLearned = lessonLearned;
            return this;
        }

        /**
         * @return {@code this}.
         */
        @java.lang.SuppressWarnings("all")
        public Trade.TradeBuilder screenshotUrl(final String screenshotUrl) {
            this.screenshotUrl = screenshotUrl;
            return this;
        }

        /**
         * @return {@code this}.
         */
        @java.lang.SuppressWarnings("all")
        public Trade.TradeBuilder createdAt(final LocalDateTime createdAt) {
            this.createdAt = createdAt;
            return this;
        }

        /**
         * @return {@code this}.
         */
        @java.lang.SuppressWarnings("all")
        public Trade.TradeBuilder updatedAt(final LocalDateTime updatedAt) {
            this.updatedAt = updatedAt;
            return this;
        }

        /**
         * @return {@code this}.
         */
        @java.lang.SuppressWarnings("all")
        public Trade.TradeBuilder account(final TradingAccount account) {
            this.account = account;
            return this;
        }

        /**
         * @return {@code this}.
         */
        @java.lang.SuppressWarnings("all")
        public Trade.TradeBuilder importBatch(final ImportBatch importBatch) {
            this.importBatch = importBatch;
            return this;
        }

        /**
         * @return {@code this}.
         */
        @java.lang.SuppressWarnings("all")
        public Trade.TradeBuilder externalTradeKey(final String externalTradeKey) {
            this.externalTradeKey = externalTradeKey;
            return this;
        }

        @java.lang.SuppressWarnings("all")
        public Trade build() {
            return new Trade(this.id, this.user, this.symbol, this.market, this.tradeDate, this.entryTime, this.exitTime, this.side, this.setup, this.session, this.entryPrice, this.stopPrice, this.targetPrice, this.exitPrice, this.quantity, this.riskAmount, this.pnlAmount, this.pnlR, this.outcome, this.confidenceRating, this.confidence, this.emotion, this.executionRating, this.followedPlan, this.mistakeTag, this.notes, this.psychologyNotes, this.lessonLearned, this.screenshotUrl, this.createdAt, this.updatedAt, this.account, this.importBatch, this.externalTradeKey);
        }

        @java.lang.Override
        @java.lang.SuppressWarnings("all")
        public java.lang.String toString() {
            return "Trade.TradeBuilder(id=" + this.id + ", user=" + this.user + ", symbol=" + this.symbol + ", market=" + this.market + ", tradeDate=" + this.tradeDate + ", entryTime=" + this.entryTime + ", exitTime=" + this.exitTime + ", side=" + this.side + ", setup=" + this.setup + ", session=" + this.session + ", entryPrice=" + this.entryPrice + ", stopPrice=" + this.stopPrice + ", targetPrice=" + this.targetPrice + ", exitPrice=" + this.exitPrice + ", quantity=" + this.quantity + ", riskAmount=" + this.riskAmount + ", pnlAmount=" + this.pnlAmount + ", pnlR=" + this.pnlR + ", outcome=" + this.outcome + ", confidenceRating=" + this.confidenceRating + ", confidence=" + this.confidence + ", emotion=" + this.emotion + ", executionRating=" + this.executionRating + ", followedPlan=" + this.followedPlan + ", mistakeTag=" + this.mistakeTag + ", notes=" + this.notes + ", psychologyNotes=" + this.psychologyNotes + ", lessonLearned=" + this.lessonLearned + ", screenshotUrl=" + this.screenshotUrl + ", createdAt=" + this.createdAt + ", updatedAt=" + this.updatedAt + ", account=" + this.account + ", importBatch=" + this.importBatch + ", externalTradeKey=" + this.externalTradeKey + ")";
        }
    }

    @java.lang.SuppressWarnings("all")
    public static Trade.TradeBuilder builder() {
        return new Trade.TradeBuilder();
    }

    @java.lang.SuppressWarnings("all")
    public Long getId() {
        return this.id;
    }

    @java.lang.SuppressWarnings("all")
    public User getUser() {
        return this.user;
    }

    @java.lang.SuppressWarnings("all")
    public String getSymbol() {
        return this.symbol;
    }

    @java.lang.SuppressWarnings("all")
    public String getMarket() {
        return this.market;
    }

    @java.lang.SuppressWarnings("all")
    public LocalDate getTradeDate() {
        return this.tradeDate;
    }

    @java.lang.SuppressWarnings("all")
    public LocalDateTime getEntryTime() {
        return this.entryTime;
    }

    @java.lang.SuppressWarnings("all")
    public LocalDateTime getExitTime() {
        return this.exitTime;
    }

    @java.lang.SuppressWarnings("all")
    public String getSide() {
        return this.side;
    }

    @java.lang.SuppressWarnings("all")
    public SetupType getSetup() {
        return this.setup;
    }

    @java.lang.SuppressWarnings("all")
    public SessionType getSession() {
        return this.session;
    }

    @java.lang.SuppressWarnings("all")
    public BigDecimal getEntryPrice() {
        return this.entryPrice;
    }

    @java.lang.SuppressWarnings("all")
    public BigDecimal getStopPrice() {
        return this.stopPrice;
    }

    @java.lang.SuppressWarnings("all")
    public BigDecimal getTargetPrice() {
        return this.targetPrice;
    }

    @java.lang.SuppressWarnings("all")
    public BigDecimal getExitPrice() {
        return this.exitPrice;
    }

    @java.lang.SuppressWarnings("all")
    public Integer getQuantity() {
        return this.quantity;
    }

    @java.lang.SuppressWarnings("all")
    public BigDecimal getRiskAmount() {
        return this.riskAmount;
    }

    @java.lang.SuppressWarnings("all")
    public BigDecimal getPnlAmount() {
        return this.pnlAmount;
    }

    @java.lang.SuppressWarnings("all")
    public BigDecimal getPnlR() {
        return this.pnlR;
    }

    @java.lang.SuppressWarnings("all")
    public OutcomeType getOutcome() {
        return this.outcome;
    }

    @java.lang.SuppressWarnings("all")
    public Integer getConfidenceRating() {
        return this.confidenceRating;
    }

    @java.lang.SuppressWarnings("all")
    public Integer getConfidence() {
        return this.confidence;
    }

    @java.lang.SuppressWarnings("all")
    public String getEmotion() {
        return this.emotion;
    }

    @java.lang.SuppressWarnings("all")
    public Integer getExecutionRating() {
        return this.executionRating;
    }

    @java.lang.SuppressWarnings("all")
    public Boolean getFollowedPlan() {
        return this.followedPlan;
    }

    @java.lang.SuppressWarnings("all")
    public MistakeTag getMistakeTag() {
        return this.mistakeTag;
    }

    @java.lang.SuppressWarnings("all")
    public String getNotes() {
        return this.notes;
    }

    @java.lang.SuppressWarnings("all")
    public String getPsychologyNotes() {
        return this.psychologyNotes;
    }

    @java.lang.SuppressWarnings("all")
    public String getLessonLearned() {
        return this.lessonLearned;
    }

    @java.lang.SuppressWarnings("all")
    public String getScreenshotUrl() {
        return this.screenshotUrl;
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
    public TradingAccount getAccount() {
        return this.account;
    }

    @java.lang.SuppressWarnings("all")
    public ImportBatch getImportBatch() {
        return this.importBatch;
    }

    @java.lang.SuppressWarnings("all")
    public String getExternalTradeKey() {
        return this.externalTradeKey;
    }

    @java.lang.SuppressWarnings("all")
    public void setId(final Long id) {
        this.id = id;
    }

    @java.lang.SuppressWarnings("all")
    public void setUser(final User user) {
        this.user = user;
    }

    @java.lang.SuppressWarnings("all")
    public void setSymbol(final String symbol) {
        this.symbol = symbol;
    }

    @java.lang.SuppressWarnings("all")
    public void setMarket(final String market) {
        this.market = market;
    }

    @java.lang.SuppressWarnings("all")
    public void setTradeDate(final LocalDate tradeDate) {
        this.tradeDate = tradeDate;
    }

    @java.lang.SuppressWarnings("all")
    public void setEntryTime(final LocalDateTime entryTime) {
        this.entryTime = entryTime;
    }

    @java.lang.SuppressWarnings("all")
    public void setExitTime(final LocalDateTime exitTime) {
        this.exitTime = exitTime;
    }

    @java.lang.SuppressWarnings("all")
    public void setSide(final String side) {
        this.side = side;
    }

    @java.lang.SuppressWarnings("all")
    public void setSetup(final SetupType setup) {
        this.setup = setup;
    }

    @java.lang.SuppressWarnings("all")
    public void setSession(final SessionType session) {
        this.session = session;
    }

    @java.lang.SuppressWarnings("all")
    public void setEntryPrice(final BigDecimal entryPrice) {
        this.entryPrice = entryPrice;
    }

    @java.lang.SuppressWarnings("all")
    public void setStopPrice(final BigDecimal stopPrice) {
        this.stopPrice = stopPrice;
    }

    @java.lang.SuppressWarnings("all")
    public void setTargetPrice(final BigDecimal targetPrice) {
        this.targetPrice = targetPrice;
    }

    @java.lang.SuppressWarnings("all")
    public void setExitPrice(final BigDecimal exitPrice) {
        this.exitPrice = exitPrice;
    }

    @java.lang.SuppressWarnings("all")
    public void setQuantity(final Integer quantity) {
        this.quantity = quantity;
    }

    @java.lang.SuppressWarnings("all")
    public void setRiskAmount(final BigDecimal riskAmount) {
        this.riskAmount = riskAmount;
    }

    @java.lang.SuppressWarnings("all")
    public void setPnlAmount(final BigDecimal pnlAmount) {
        this.pnlAmount = pnlAmount;
    }

    @java.lang.SuppressWarnings("all")
    public void setPnlR(final BigDecimal pnlR) {
        this.pnlR = pnlR;
    }

    @java.lang.SuppressWarnings("all")
    public void setOutcome(final OutcomeType outcome) {
        this.outcome = outcome;
    }

    @java.lang.SuppressWarnings("all")
    public void setConfidenceRating(final Integer confidenceRating) {
        this.confidenceRating = confidenceRating;
    }

    @java.lang.SuppressWarnings("all")
    public void setConfidence(final Integer confidence) {
        this.confidence = confidence;
    }

    @java.lang.SuppressWarnings("all")
    public void setEmotion(final String emotion) {
        this.emotion = emotion;
    }

    @java.lang.SuppressWarnings("all")
    public void setExecutionRating(final Integer executionRating) {
        this.executionRating = executionRating;
    }

    @java.lang.SuppressWarnings("all")
    public void setFollowedPlan(final Boolean followedPlan) {
        this.followedPlan = followedPlan;
    }

    @java.lang.SuppressWarnings("all")
    public void setMistakeTag(final MistakeTag mistakeTag) {
        this.mistakeTag = mistakeTag;
    }

    @java.lang.SuppressWarnings("all")
    public void setNotes(final String notes) {
        this.notes = notes;
    }

    @java.lang.SuppressWarnings("all")
    public void setPsychologyNotes(final String psychologyNotes) {
        this.psychologyNotes = psychologyNotes;
    }

    @java.lang.SuppressWarnings("all")
    public void setLessonLearned(final String lessonLearned) {
        this.lessonLearned = lessonLearned;
    }

    @java.lang.SuppressWarnings("all")
    public void setScreenshotUrl(final String screenshotUrl) {
        this.screenshotUrl = screenshotUrl;
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
    public void setAccount(final TradingAccount account) {
        this.account = account;
    }

    @java.lang.SuppressWarnings("all")
    public void setImportBatch(final ImportBatch importBatch) {
        this.importBatch = importBatch;
    }

    @java.lang.SuppressWarnings("all")
    public void setExternalTradeKey(final String externalTradeKey) {
        this.externalTradeKey = externalTradeKey;
    }

    @java.lang.SuppressWarnings("all")
    public Trade() {
    }

    @java.lang.SuppressWarnings("all")
    public Trade(final Long id, final User user, final String symbol, final String market, final LocalDate tradeDate, final LocalDateTime entryTime, final LocalDateTime exitTime, final String side, final SetupType setup, final SessionType session, final BigDecimal entryPrice, final BigDecimal stopPrice, final BigDecimal targetPrice, final BigDecimal exitPrice, final Integer quantity, final BigDecimal riskAmount, final BigDecimal pnlAmount, final BigDecimal pnlR, final OutcomeType outcome, final Integer confidenceRating, final Integer confidence, final String emotion, final Integer executionRating, final Boolean followedPlan, final MistakeTag mistakeTag, final String notes, final String psychologyNotes, final String lessonLearned, final String screenshotUrl, final LocalDateTime createdAt, final LocalDateTime updatedAt, final TradingAccount account, final ImportBatch importBatch, final String externalTradeKey) {
        this.id = id;
        this.user = user;
        this.symbol = symbol;
        this.market = market;
        this.tradeDate = tradeDate;
        this.entryTime = entryTime;
        this.exitTime = exitTime;
        this.side = side;
        this.setup = setup;
        this.session = session;
        this.entryPrice = entryPrice;
        this.stopPrice = stopPrice;
        this.targetPrice = targetPrice;
        this.exitPrice = exitPrice;
        this.quantity = quantity;
        this.riskAmount = riskAmount;
        this.pnlAmount = pnlAmount;
        this.pnlR = pnlR;
        this.outcome = outcome;
        this.confidenceRating = confidenceRating;
        this.confidence = confidence;
        this.emotion = emotion;
        this.executionRating = executionRating;
        this.followedPlan = followedPlan;
        this.mistakeTag = mistakeTag;
        this.notes = notes;
        this.psychologyNotes = psychologyNotes;
        this.lessonLearned = lessonLearned;
        this.screenshotUrl = screenshotUrl;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
        this.account = account;
        this.importBatch = importBatch;
        this.externalTradeKey = externalTradeKey;
    }
}
