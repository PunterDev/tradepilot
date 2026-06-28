package com.tradepilot.dto.session;

import com.tradepilot.enums.OutcomeType;
import com.tradepilot.enums.SetupType;
import java.math.BigDecimal;
import java.time.LocalDateTime;

public class SessionTimelineTradeResponse {
    private Long tradeId;
    private String symbol;
    private String side;
    private LocalDateTime entryTime;
    private LocalDateTime exitTime;
    private Integer quantity;
    private BigDecimal pnl;
    private SetupType setup;
    private Integer confidence;
    private String emotion;
    private String notes;
    private OutcomeType outcome;

    @java.lang.SuppressWarnings("all")
    public SessionTimelineTradeResponse() {
    }

    @java.lang.SuppressWarnings("all")
    public Long getTradeId() {
        return this.tradeId;
    }

    @java.lang.SuppressWarnings("all")
    public String getSymbol() {
        return this.symbol;
    }

    @java.lang.SuppressWarnings("all")
    public String getSide() {
        return this.side;
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
    public Integer getQuantity() {
        return this.quantity;
    }

    @java.lang.SuppressWarnings("all")
    public BigDecimal getPnl() {
        return this.pnl;
    }

    @java.lang.SuppressWarnings("all")
    public SetupType getSetup() {
        return this.setup;
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
    public String getNotes() {
        return this.notes;
    }

    @java.lang.SuppressWarnings("all")
    public OutcomeType getOutcome() {
        return this.outcome;
    }

    @java.lang.SuppressWarnings("all")
    public void setTradeId(final Long tradeId) {
        this.tradeId = tradeId;
    }

    @java.lang.SuppressWarnings("all")
    public void setSymbol(final String symbol) {
        this.symbol = symbol;
    }

    @java.lang.SuppressWarnings("all")
    public void setSide(final String side) {
        this.side = side;
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
    public void setQuantity(final Integer quantity) {
        this.quantity = quantity;
    }

    @java.lang.SuppressWarnings("all")
    public void setPnl(final BigDecimal pnl) {
        this.pnl = pnl;
    }

    @java.lang.SuppressWarnings("all")
    public void setSetup(final SetupType setup) {
        this.setup = setup;
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
    public void setNotes(final String notes) {
        this.notes = notes;
    }

    @java.lang.SuppressWarnings("all")
    public void setOutcome(final OutcomeType outcome) {
        this.outcome = outcome;
    }

    @java.lang.Override
    @java.lang.SuppressWarnings("all")
    public boolean equals(final java.lang.Object o) {
        if (o == this) return true;
        if (!(o instanceof SessionTimelineTradeResponse)) return false;
        final SessionTimelineTradeResponse other = (SessionTimelineTradeResponse) o;
        if (!other.canEqual((java.lang.Object) this)) return false;
        final java.lang.Object this$tradeId = this.getTradeId();
        final java.lang.Object other$tradeId = other.getTradeId();
        if (this$tradeId == null ? other$tradeId != null : !this$tradeId.equals(other$tradeId)) return false;
        final java.lang.Object this$quantity = this.getQuantity();
        final java.lang.Object other$quantity = other.getQuantity();
        if (this$quantity == null ? other$quantity != null : !this$quantity.equals(other$quantity)) return false;
        final java.lang.Object this$confidence = this.getConfidence();
        final java.lang.Object other$confidence = other.getConfidence();
        if (this$confidence == null ? other$confidence != null : !this$confidence.equals(other$confidence)) return false;
        final java.lang.Object this$symbol = this.getSymbol();
        final java.lang.Object other$symbol = other.getSymbol();
        if (this$symbol == null ? other$symbol != null : !this$symbol.equals(other$symbol)) return false;
        final java.lang.Object this$side = this.getSide();
        final java.lang.Object other$side = other.getSide();
        if (this$side == null ? other$side != null : !this$side.equals(other$side)) return false;
        final java.lang.Object this$entryTime = this.getEntryTime();
        final java.lang.Object other$entryTime = other.getEntryTime();
        if (this$entryTime == null ? other$entryTime != null : !this$entryTime.equals(other$entryTime)) return false;
        final java.lang.Object this$exitTime = this.getExitTime();
        final java.lang.Object other$exitTime = other.getExitTime();
        if (this$exitTime == null ? other$exitTime != null : !this$exitTime.equals(other$exitTime)) return false;
        final java.lang.Object this$pnl = this.getPnl();
        final java.lang.Object other$pnl = other.getPnl();
        if (this$pnl == null ? other$pnl != null : !this$pnl.equals(other$pnl)) return false;
        final java.lang.Object this$setup = this.getSetup();
        final java.lang.Object other$setup = other.getSetup();
        if (this$setup == null ? other$setup != null : !this$setup.equals(other$setup)) return false;
        final java.lang.Object this$emotion = this.getEmotion();
        final java.lang.Object other$emotion = other.getEmotion();
        if (this$emotion == null ? other$emotion != null : !this$emotion.equals(other$emotion)) return false;
        final java.lang.Object this$notes = this.getNotes();
        final java.lang.Object other$notes = other.getNotes();
        if (this$notes == null ? other$notes != null : !this$notes.equals(other$notes)) return false;
        final java.lang.Object this$outcome = this.getOutcome();
        final java.lang.Object other$outcome = other.getOutcome();
        if (this$outcome == null ? other$outcome != null : !this$outcome.equals(other$outcome)) return false;
        return true;
    }

    @java.lang.SuppressWarnings("all")
    protected boolean canEqual(final java.lang.Object other) {
        return other instanceof SessionTimelineTradeResponse;
    }

    @java.lang.Override
    @java.lang.SuppressWarnings("all")
    public int hashCode() {
        final int PRIME = 59;
        int result = 1;
        final java.lang.Object $tradeId = this.getTradeId();
        result = result * PRIME + ($tradeId == null ? 43 : $tradeId.hashCode());
        final java.lang.Object $quantity = this.getQuantity();
        result = result * PRIME + ($quantity == null ? 43 : $quantity.hashCode());
        final java.lang.Object $confidence = this.getConfidence();
        result = result * PRIME + ($confidence == null ? 43 : $confidence.hashCode());
        final java.lang.Object $symbol = this.getSymbol();
        result = result * PRIME + ($symbol == null ? 43 : $symbol.hashCode());
        final java.lang.Object $side = this.getSide();
        result = result * PRIME + ($side == null ? 43 : $side.hashCode());
        final java.lang.Object $entryTime = this.getEntryTime();
        result = result * PRIME + ($entryTime == null ? 43 : $entryTime.hashCode());
        final java.lang.Object $exitTime = this.getExitTime();
        result = result * PRIME + ($exitTime == null ? 43 : $exitTime.hashCode());
        final java.lang.Object $pnl = this.getPnl();
        result = result * PRIME + ($pnl == null ? 43 : $pnl.hashCode());
        final java.lang.Object $setup = this.getSetup();
        result = result * PRIME + ($setup == null ? 43 : $setup.hashCode());
        final java.lang.Object $emotion = this.getEmotion();
        result = result * PRIME + ($emotion == null ? 43 : $emotion.hashCode());
        final java.lang.Object $notes = this.getNotes();
        result = result * PRIME + ($notes == null ? 43 : $notes.hashCode());
        final java.lang.Object $outcome = this.getOutcome();
        result = result * PRIME + ($outcome == null ? 43 : $outcome.hashCode());
        return result;
    }

    @java.lang.Override
    @java.lang.SuppressWarnings("all")
    public java.lang.String toString() {
        return "SessionTimelineTradeResponse(tradeId=" + this.getTradeId() + ", symbol=" + this.getSymbol() + ", side=" + this.getSide() + ", entryTime=" + this.getEntryTime() + ", exitTime=" + this.getExitTime() + ", quantity=" + this.getQuantity() + ", pnl=" + this.getPnl() + ", setup=" + this.getSetup() + ", confidence=" + this.getConfidence() + ", emotion=" + this.getEmotion() + ", notes=" + this.getNotes() + ", outcome=" + this.getOutcome() + ")";
    }
}
