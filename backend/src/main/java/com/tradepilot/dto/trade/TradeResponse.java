package com.tradepilot.dto.trade;

import com.tradepilot.enums.*;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

public class TradeResponse {
    private Long id;
    private Long accountId;
    private String accountName;
    private String accountType;
    private String symbol;
    private String originalBrokerSymbol;
    private String contractSymbol;
    private String contractSymbolSource;
    private LocalDateTime contractSymbolConfirmedAt;
    private Market market;
    private LocalDate tradeDate;
    private LocalDateTime entryTime;
    private LocalDateTime exitTime;
    private Side side;
    private SetupType setup;
    private SessionType session;
    private BigDecimal entryPrice;
    private BigDecimal stopPrice;
    private BigDecimal targetPrice;
    private BigDecimal exitPrice;
    private Integer quantity;
    private BigDecimal riskAmount;
    private BigDecimal pnlAmount;
    private BigDecimal pnlR;
    private OutcomeType outcome;
    private Integer confidence;
    private Integer confidenceRating;
    private String emotion;
    private Integer executionRating;
    private Boolean followedPlan;
    private MistakeTag mistakeTag;
    private String notes;
    private String psychologyNotes;
    private String lessonLearned;
    private String screenshotUrl;

    @java.lang.SuppressWarnings("all")
    public TradeResponse() {
    }

    @java.lang.SuppressWarnings("all")
    public Long getId() {
        return this.id;
    }

    @java.lang.SuppressWarnings("all")
    public Long getAccountId() {
        return this.accountId;
    }

    @java.lang.SuppressWarnings("all")
    public String getAccountName() {
        return this.accountName;
    }

    @java.lang.SuppressWarnings("all")
    public String getAccountType() {
        return this.accountType;
    }

    @java.lang.SuppressWarnings("all")
    public String getSymbol() {
        return this.symbol;
    }

    public String getOriginalBrokerSymbol() {
        return this.originalBrokerSymbol;
    }

    public String getContractSymbol() {
        return this.contractSymbol;
    }

    public String getContractSymbolSource() {
        return this.contractSymbolSource;
    }

    public LocalDateTime getContractSymbolConfirmedAt() {
        return this.contractSymbolConfirmedAt;
    }

    @java.lang.SuppressWarnings("all")
    public Market getMarket() {
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
    public Side getSide() {
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
    public Integer getConfidence() {
        return this.confidence;
    }

    @java.lang.SuppressWarnings("all")
    public Integer getConfidenceRating() {
        return this.confidenceRating;
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
    public void setId(final Long id) {
        this.id = id;
    }

    @java.lang.SuppressWarnings("all")
    public void setAccountId(final Long accountId) {
        this.accountId = accountId;
    }

    @java.lang.SuppressWarnings("all")
    public void setAccountName(final String accountName) {
        this.accountName = accountName;
    }

    @java.lang.SuppressWarnings("all")
    public void setAccountType(final String accountType) {
        this.accountType = accountType;
    }

    @java.lang.SuppressWarnings("all")
    public void setSymbol(final String symbol) {
        this.symbol = symbol;
    }

    public void setOriginalBrokerSymbol(final String originalBrokerSymbol) {
        this.originalBrokerSymbol = originalBrokerSymbol;
    }

    public void setContractSymbol(final String contractSymbol) {
        this.contractSymbol = contractSymbol;
    }

    public void setContractSymbolSource(final String contractSymbolSource) {
        this.contractSymbolSource = contractSymbolSource;
    }

    public void setContractSymbolConfirmedAt(final LocalDateTime contractSymbolConfirmedAt) {
        this.contractSymbolConfirmedAt = contractSymbolConfirmedAt;
    }

    @java.lang.SuppressWarnings("all")
    public void setMarket(final Market market) {
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
    public void setSide(final Side side) {
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
    public void setConfidence(final Integer confidence) {
        this.confidence = confidence;
    }

    @java.lang.SuppressWarnings("all")
    public void setConfidenceRating(final Integer confidenceRating) {
        this.confidenceRating = confidenceRating;
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

    @java.lang.Override
    @java.lang.SuppressWarnings("all")
    public boolean equals(final java.lang.Object o) {
        if (o == this) return true;
        if (!(o instanceof TradeResponse)) return false;
        final TradeResponse other = (TradeResponse) o;
        if (!other.canEqual((java.lang.Object) this)) return false;
        final java.lang.Object this$id = this.getId();
        final java.lang.Object other$id = other.getId();
        if (this$id == null ? other$id != null : !this$id.equals(other$id)) return false;
        final java.lang.Object this$accountId = this.getAccountId();
        final java.lang.Object other$accountId = other.getAccountId();
        if (this$accountId == null ? other$accountId != null : !this$accountId.equals(other$accountId)) return false;
        final java.lang.Object this$quantity = this.getQuantity();
        final java.lang.Object other$quantity = other.getQuantity();
        if (this$quantity == null ? other$quantity != null : !this$quantity.equals(other$quantity)) return false;
        final java.lang.Object this$confidence = this.getConfidence();
        final java.lang.Object other$confidence = other.getConfidence();
        if (this$confidence == null ? other$confidence != null : !this$confidence.equals(other$confidence)) return false;
        final java.lang.Object this$confidenceRating = this.getConfidenceRating();
        final java.lang.Object other$confidenceRating = other.getConfidenceRating();
        if (this$confidenceRating == null ? other$confidenceRating != null : !this$confidenceRating.equals(other$confidenceRating)) return false;
        final java.lang.Object this$executionRating = this.getExecutionRating();
        final java.lang.Object other$executionRating = other.getExecutionRating();
        if (this$executionRating == null ? other$executionRating != null : !this$executionRating.equals(other$executionRating)) return false;
        final java.lang.Object this$followedPlan = this.getFollowedPlan();
        final java.lang.Object other$followedPlan = other.getFollowedPlan();
        if (this$followedPlan == null ? other$followedPlan != null : !this$followedPlan.equals(other$followedPlan)) return false;
        final java.lang.Object this$accountName = this.getAccountName();
        final java.lang.Object other$accountName = other.getAccountName();
        if (this$accountName == null ? other$accountName != null : !this$accountName.equals(other$accountName)) return false;
        final java.lang.Object this$accountType = this.getAccountType();
        final java.lang.Object other$accountType = other.getAccountType();
        if (this$accountType == null ? other$accountType != null : !this$accountType.equals(other$accountType)) return false;
        final java.lang.Object this$symbol = this.getSymbol();
        final java.lang.Object other$symbol = other.getSymbol();
        if (this$symbol == null ? other$symbol != null : !this$symbol.equals(other$symbol)) return false;
        final java.lang.Object this$market = this.getMarket();
        final java.lang.Object other$market = other.getMarket();
        if (this$market == null ? other$market != null : !this$market.equals(other$market)) return false;
        final java.lang.Object this$tradeDate = this.getTradeDate();
        final java.lang.Object other$tradeDate = other.getTradeDate();
        if (this$tradeDate == null ? other$tradeDate != null : !this$tradeDate.equals(other$tradeDate)) return false;
        final java.lang.Object this$entryTime = this.getEntryTime();
        final java.lang.Object other$entryTime = other.getEntryTime();
        if (this$entryTime == null ? other$entryTime != null : !this$entryTime.equals(other$entryTime)) return false;
        final java.lang.Object this$exitTime = this.getExitTime();
        final java.lang.Object other$exitTime = other.getExitTime();
        if (this$exitTime == null ? other$exitTime != null : !this$exitTime.equals(other$exitTime)) return false;
        final java.lang.Object this$side = this.getSide();
        final java.lang.Object other$side = other.getSide();
        if (this$side == null ? other$side != null : !this$side.equals(other$side)) return false;
        final java.lang.Object this$setup = this.getSetup();
        final java.lang.Object other$setup = other.getSetup();
        if (this$setup == null ? other$setup != null : !this$setup.equals(other$setup)) return false;
        final java.lang.Object this$session = this.getSession();
        final java.lang.Object other$session = other.getSession();
        if (this$session == null ? other$session != null : !this$session.equals(other$session)) return false;
        final java.lang.Object this$entryPrice = this.getEntryPrice();
        final java.lang.Object other$entryPrice = other.getEntryPrice();
        if (this$entryPrice == null ? other$entryPrice != null : !this$entryPrice.equals(other$entryPrice)) return false;
        final java.lang.Object this$stopPrice = this.getStopPrice();
        final java.lang.Object other$stopPrice = other.getStopPrice();
        if (this$stopPrice == null ? other$stopPrice != null : !this$stopPrice.equals(other$stopPrice)) return false;
        final java.lang.Object this$targetPrice = this.getTargetPrice();
        final java.lang.Object other$targetPrice = other.getTargetPrice();
        if (this$targetPrice == null ? other$targetPrice != null : !this$targetPrice.equals(other$targetPrice)) return false;
        final java.lang.Object this$exitPrice = this.getExitPrice();
        final java.lang.Object other$exitPrice = other.getExitPrice();
        if (this$exitPrice == null ? other$exitPrice != null : !this$exitPrice.equals(other$exitPrice)) return false;
        final java.lang.Object this$riskAmount = this.getRiskAmount();
        final java.lang.Object other$riskAmount = other.getRiskAmount();
        if (this$riskAmount == null ? other$riskAmount != null : !this$riskAmount.equals(other$riskAmount)) return false;
        final java.lang.Object this$pnlAmount = this.getPnlAmount();
        final java.lang.Object other$pnlAmount = other.getPnlAmount();
        if (this$pnlAmount == null ? other$pnlAmount != null : !this$pnlAmount.equals(other$pnlAmount)) return false;
        final java.lang.Object this$pnlR = this.getPnlR();
        final java.lang.Object other$pnlR = other.getPnlR();
        if (this$pnlR == null ? other$pnlR != null : !this$pnlR.equals(other$pnlR)) return false;
        final java.lang.Object this$outcome = this.getOutcome();
        final java.lang.Object other$outcome = other.getOutcome();
        if (this$outcome == null ? other$outcome != null : !this$outcome.equals(other$outcome)) return false;
        final java.lang.Object this$emotion = this.getEmotion();
        final java.lang.Object other$emotion = other.getEmotion();
        if (this$emotion == null ? other$emotion != null : !this$emotion.equals(other$emotion)) return false;
        final java.lang.Object this$mistakeTag = this.getMistakeTag();
        final java.lang.Object other$mistakeTag = other.getMistakeTag();
        if (this$mistakeTag == null ? other$mistakeTag != null : !this$mistakeTag.equals(other$mistakeTag)) return false;
        final java.lang.Object this$notes = this.getNotes();
        final java.lang.Object other$notes = other.getNotes();
        if (this$notes == null ? other$notes != null : !this$notes.equals(other$notes)) return false;
        final java.lang.Object this$psychologyNotes = this.getPsychologyNotes();
        final java.lang.Object other$psychologyNotes = other.getPsychologyNotes();
        if (this$psychologyNotes == null ? other$psychologyNotes != null : !this$psychologyNotes.equals(other$psychologyNotes)) return false;
        final java.lang.Object this$lessonLearned = this.getLessonLearned();
        final java.lang.Object other$lessonLearned = other.getLessonLearned();
        if (this$lessonLearned == null ? other$lessonLearned != null : !this$lessonLearned.equals(other$lessonLearned)) return false;
        final java.lang.Object this$screenshotUrl = this.getScreenshotUrl();
        final java.lang.Object other$screenshotUrl = other.getScreenshotUrl();
        if (this$screenshotUrl == null ? other$screenshotUrl != null : !this$screenshotUrl.equals(other$screenshotUrl)) return false;
        return true;
    }

    @java.lang.SuppressWarnings("all")
    protected boolean canEqual(final java.lang.Object other) {
        return other instanceof TradeResponse;
    }

    @java.lang.Override
    @java.lang.SuppressWarnings("all")
    public int hashCode() {
        final int PRIME = 59;
        int result = 1;
        final java.lang.Object $id = this.getId();
        result = result * PRIME + ($id == null ? 43 : $id.hashCode());
        final java.lang.Object $accountId = this.getAccountId();
        result = result * PRIME + ($accountId == null ? 43 : $accountId.hashCode());
        final java.lang.Object $quantity = this.getQuantity();
        result = result * PRIME + ($quantity == null ? 43 : $quantity.hashCode());
        final java.lang.Object $confidence = this.getConfidence();
        result = result * PRIME + ($confidence == null ? 43 : $confidence.hashCode());
        final java.lang.Object $confidenceRating = this.getConfidenceRating();
        result = result * PRIME + ($confidenceRating == null ? 43 : $confidenceRating.hashCode());
        final java.lang.Object $executionRating = this.getExecutionRating();
        result = result * PRIME + ($executionRating == null ? 43 : $executionRating.hashCode());
        final java.lang.Object $followedPlan = this.getFollowedPlan();
        result = result * PRIME + ($followedPlan == null ? 43 : $followedPlan.hashCode());
        final java.lang.Object $accountName = this.getAccountName();
        result = result * PRIME + ($accountName == null ? 43 : $accountName.hashCode());
        final java.lang.Object $accountType = this.getAccountType();
        result = result * PRIME + ($accountType == null ? 43 : $accountType.hashCode());
        final java.lang.Object $symbol = this.getSymbol();
        result = result * PRIME + ($symbol == null ? 43 : $symbol.hashCode());
        final java.lang.Object $market = this.getMarket();
        result = result * PRIME + ($market == null ? 43 : $market.hashCode());
        final java.lang.Object $tradeDate = this.getTradeDate();
        result = result * PRIME + ($tradeDate == null ? 43 : $tradeDate.hashCode());
        final java.lang.Object $entryTime = this.getEntryTime();
        result = result * PRIME + ($entryTime == null ? 43 : $entryTime.hashCode());
        final java.lang.Object $exitTime = this.getExitTime();
        result = result * PRIME + ($exitTime == null ? 43 : $exitTime.hashCode());
        final java.lang.Object $side = this.getSide();
        result = result * PRIME + ($side == null ? 43 : $side.hashCode());
        final java.lang.Object $setup = this.getSetup();
        result = result * PRIME + ($setup == null ? 43 : $setup.hashCode());
        final java.lang.Object $session = this.getSession();
        result = result * PRIME + ($session == null ? 43 : $session.hashCode());
        final java.lang.Object $entryPrice = this.getEntryPrice();
        result = result * PRIME + ($entryPrice == null ? 43 : $entryPrice.hashCode());
        final java.lang.Object $stopPrice = this.getStopPrice();
        result = result * PRIME + ($stopPrice == null ? 43 : $stopPrice.hashCode());
        final java.lang.Object $targetPrice = this.getTargetPrice();
        result = result * PRIME + ($targetPrice == null ? 43 : $targetPrice.hashCode());
        final java.lang.Object $exitPrice = this.getExitPrice();
        result = result * PRIME + ($exitPrice == null ? 43 : $exitPrice.hashCode());
        final java.lang.Object $riskAmount = this.getRiskAmount();
        result = result * PRIME + ($riskAmount == null ? 43 : $riskAmount.hashCode());
        final java.lang.Object $pnlAmount = this.getPnlAmount();
        result = result * PRIME + ($pnlAmount == null ? 43 : $pnlAmount.hashCode());
        final java.lang.Object $pnlR = this.getPnlR();
        result = result * PRIME + ($pnlR == null ? 43 : $pnlR.hashCode());
        final java.lang.Object $outcome = this.getOutcome();
        result = result * PRIME + ($outcome == null ? 43 : $outcome.hashCode());
        final java.lang.Object $emotion = this.getEmotion();
        result = result * PRIME + ($emotion == null ? 43 : $emotion.hashCode());
        final java.lang.Object $mistakeTag = this.getMistakeTag();
        result = result * PRIME + ($mistakeTag == null ? 43 : $mistakeTag.hashCode());
        final java.lang.Object $notes = this.getNotes();
        result = result * PRIME + ($notes == null ? 43 : $notes.hashCode());
        final java.lang.Object $psychologyNotes = this.getPsychologyNotes();
        result = result * PRIME + ($psychologyNotes == null ? 43 : $psychologyNotes.hashCode());
        final java.lang.Object $lessonLearned = this.getLessonLearned();
        result = result * PRIME + ($lessonLearned == null ? 43 : $lessonLearned.hashCode());
        final java.lang.Object $screenshotUrl = this.getScreenshotUrl();
        result = result * PRIME + ($screenshotUrl == null ? 43 : $screenshotUrl.hashCode());
        return result;
    }

    @java.lang.Override
    @java.lang.SuppressWarnings("all")
    public java.lang.String toString() {
        return "TradeResponse(id=" + this.getId() + ", accountId=" + this.getAccountId() + ", accountName=" + this.getAccountName() + ", accountType=" + this.getAccountType() + ", symbol=" + this.getSymbol() + ", market=" + this.getMarket() + ", tradeDate=" + this.getTradeDate() + ", entryTime=" + this.getEntryTime() + ", exitTime=" + this.getExitTime() + ", side=" + this.getSide() + ", setup=" + this.getSetup() + ", session=" + this.getSession() + ", entryPrice=" + this.getEntryPrice() + ", stopPrice=" + this.getStopPrice() + ", targetPrice=" + this.getTargetPrice() + ", exitPrice=" + this.getExitPrice() + ", quantity=" + this.getQuantity() + ", riskAmount=" + this.getRiskAmount() + ", pnlAmount=" + this.getPnlAmount() + ", pnlR=" + this.getPnlR() + ", outcome=" + this.getOutcome() + ", confidence=" + this.getConfidence() + ", confidenceRating=" + this.getConfidenceRating() + ", emotion=" + this.getEmotion() + ", executionRating=" + this.getExecutionRating() + ", followedPlan=" + this.getFollowedPlan() + ", mistakeTag=" + this.getMistakeTag() + ", notes=" + this.getNotes() + ", psychologyNotes=" + this.getPsychologyNotes() + ", lessonLearned=" + this.getLessonLearned() + ", screenshotUrl=" + this.getScreenshotUrl() + ")";
    }
}
