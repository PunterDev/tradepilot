package com.tradepilot.dto.ai;

public class AiTradeReviewRequest {
    private Long tradeId;
    private String journalText;
    private TradeSnapshot trade;
    private Boolean forceRefresh = false;

    @java.lang.SuppressWarnings("all")
    public AiTradeReviewRequest() {
    }

    @java.lang.SuppressWarnings("all")
    public Long getTradeId() {
        return this.tradeId;
    }

    @java.lang.SuppressWarnings("all")
    public String getJournalText() {
        return this.journalText;
    }

    @java.lang.SuppressWarnings("all")
    public TradeSnapshot getTrade() {
        return this.trade;
    }

    @java.lang.SuppressWarnings("all")
    public Boolean getForceRefresh() {
        return this.forceRefresh;
    }

    @java.lang.SuppressWarnings("all")
    public void setTradeId(final Long tradeId) {
        this.tradeId = tradeId;
    }

    @java.lang.SuppressWarnings("all")
    public void setJournalText(final String journalText) {
        this.journalText = journalText;
    }

    @java.lang.SuppressWarnings("all")
    public void setTrade(final TradeSnapshot trade) {
        this.trade = trade;
    }

    @java.lang.SuppressWarnings("all")
    public void setForceRefresh(final Boolean forceRefresh) {
        this.forceRefresh = forceRefresh;
    }

    @java.lang.Override
    @java.lang.SuppressWarnings("all")
    public boolean equals(final java.lang.Object o) {
        if (o == this) return true;
        if (!(o instanceof AiTradeReviewRequest)) return false;
        final AiTradeReviewRequest other = (AiTradeReviewRequest) o;
        if (!other.canEqual((java.lang.Object) this)) return false;
        final java.lang.Object this$tradeId = this.getTradeId();
        final java.lang.Object other$tradeId = other.getTradeId();
        if (this$tradeId == null ? other$tradeId != null : !this$tradeId.equals(other$tradeId)) return false;
        final java.lang.Object this$forceRefresh = this.getForceRefresh();
        final java.lang.Object other$forceRefresh = other.getForceRefresh();
        if (this$forceRefresh == null ? other$forceRefresh != null : !this$forceRefresh.equals(other$forceRefresh)) return false;
        final java.lang.Object this$journalText = this.getJournalText();
        final java.lang.Object other$journalText = other.getJournalText();
        if (this$journalText == null ? other$journalText != null : !this$journalText.equals(other$journalText)) return false;
        final java.lang.Object this$trade = this.getTrade();
        final java.lang.Object other$trade = other.getTrade();
        if (this$trade == null ? other$trade != null : !this$trade.equals(other$trade)) return false;
        return true;
    }

    @java.lang.SuppressWarnings("all")
    protected boolean canEqual(final java.lang.Object other) {
        return other instanceof AiTradeReviewRequest;
    }

    @java.lang.Override
    @java.lang.SuppressWarnings("all")
    public int hashCode() {
        final int PRIME = 59;
        int result = 1;
        final java.lang.Object $tradeId = this.getTradeId();
        result = result * PRIME + ($tradeId == null ? 43 : $tradeId.hashCode());
        final java.lang.Object $forceRefresh = this.getForceRefresh();
        result = result * PRIME + ($forceRefresh == null ? 43 : $forceRefresh.hashCode());
        final java.lang.Object $journalText = this.getJournalText();
        result = result * PRIME + ($journalText == null ? 43 : $journalText.hashCode());
        final java.lang.Object $trade = this.getTrade();
        result = result * PRIME + ($trade == null ? 43 : $trade.hashCode());
        return result;
    }

    @java.lang.Override
    @java.lang.SuppressWarnings("all")
    public java.lang.String toString() {
        return "AiTradeReviewRequest(tradeId=" + this.getTradeId() + ", journalText=" + this.getJournalText() + ", trade=" + this.getTrade() + ", forceRefresh=" + this.getForceRefresh() + ")";
    }
}
