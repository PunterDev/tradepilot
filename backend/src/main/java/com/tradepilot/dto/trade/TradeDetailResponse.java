package com.tradepilot.dto.trade;

import com.tradepilot.dto.ai.AiTradeReviewResponse;
import java.util.List;

public class TradeDetailResponse {
    private TradeResponse trade;
    private Long previousTradeId;
    private Long nextTradeId;
    private AiTradeReviewResponse aiReview;
    private Boolean aiReviewCached;
    private List<String> relatedBehavioralInsights;
    private List<TradeViolationResponse> disciplineViolations;

    public static TradeDetailResponse of(TradeResponse trade, Long previousTradeId, Long nextTradeId, AiTradeReviewResponse aiReview, Boolean aiReviewCached, List<String> relatedBehavioralInsights, List<TradeViolationResponse> disciplineViolations) {
        TradeDetailResponse response = new TradeDetailResponse();
        response.setTrade(trade);
        response.setPreviousTradeId(previousTradeId);
        response.setNextTradeId(nextTradeId);
        response.setAiReview(aiReview);
        response.setAiReviewCached(aiReviewCached);
        response.setRelatedBehavioralInsights(relatedBehavioralInsights);
        response.setDisciplineViolations(disciplineViolations);
        return response;
    }

    @java.lang.SuppressWarnings("all")
    public TradeDetailResponse() {
    }

    @java.lang.SuppressWarnings("all")
    public TradeResponse getTrade() {
        return this.trade;
    }

    @java.lang.SuppressWarnings("all")
    public Long getPreviousTradeId() {
        return this.previousTradeId;
    }

    @java.lang.SuppressWarnings("all")
    public Long getNextTradeId() {
        return this.nextTradeId;
    }

    @java.lang.SuppressWarnings("all")
    public AiTradeReviewResponse getAiReview() {
        return this.aiReview;
    }

    @java.lang.SuppressWarnings("all")
    public Boolean getAiReviewCached() {
        return this.aiReviewCached;
    }

    @java.lang.SuppressWarnings("all")
    public List<String> getRelatedBehavioralInsights() {
        return this.relatedBehavioralInsights;
    }

    @java.lang.SuppressWarnings("all")
    public List<TradeViolationResponse> getDisciplineViolations() {
        return this.disciplineViolations;
    }

    @java.lang.SuppressWarnings("all")
    public void setTrade(final TradeResponse trade) {
        this.trade = trade;
    }

    @java.lang.SuppressWarnings("all")
    public void setPreviousTradeId(final Long previousTradeId) {
        this.previousTradeId = previousTradeId;
    }

    @java.lang.SuppressWarnings("all")
    public void setNextTradeId(final Long nextTradeId) {
        this.nextTradeId = nextTradeId;
    }

    @java.lang.SuppressWarnings("all")
    public void setAiReview(final AiTradeReviewResponse aiReview) {
        this.aiReview = aiReview;
    }

    @java.lang.SuppressWarnings("all")
    public void setAiReviewCached(final Boolean aiReviewCached) {
        this.aiReviewCached = aiReviewCached;
    }

    @java.lang.SuppressWarnings("all")
    public void setRelatedBehavioralInsights(final List<String> relatedBehavioralInsights) {
        this.relatedBehavioralInsights = relatedBehavioralInsights;
    }

    @java.lang.SuppressWarnings("all")
    public void setDisciplineViolations(final List<TradeViolationResponse> disciplineViolations) {
        this.disciplineViolations = disciplineViolations;
    }

    @java.lang.Override
    @java.lang.SuppressWarnings("all")
    public boolean equals(final java.lang.Object o) {
        if (o == this) return true;
        if (!(o instanceof TradeDetailResponse)) return false;
        final TradeDetailResponse other = (TradeDetailResponse) o;
        if (!other.canEqual((java.lang.Object) this)) return false;
        final java.lang.Object this$previousTradeId = this.getPreviousTradeId();
        final java.lang.Object other$previousTradeId = other.getPreviousTradeId();
        if (this$previousTradeId == null ? other$previousTradeId != null : !this$previousTradeId.equals(other$previousTradeId)) return false;
        final java.lang.Object this$nextTradeId = this.getNextTradeId();
        final java.lang.Object other$nextTradeId = other.getNextTradeId();
        if (this$nextTradeId == null ? other$nextTradeId != null : !this$nextTradeId.equals(other$nextTradeId)) return false;
        final java.lang.Object this$aiReviewCached = this.getAiReviewCached();
        final java.lang.Object other$aiReviewCached = other.getAiReviewCached();
        if (this$aiReviewCached == null ? other$aiReviewCached != null : !this$aiReviewCached.equals(other$aiReviewCached)) return false;
        final java.lang.Object this$trade = this.getTrade();
        final java.lang.Object other$trade = other.getTrade();
        if (this$trade == null ? other$trade != null : !this$trade.equals(other$trade)) return false;
        final java.lang.Object this$aiReview = this.getAiReview();
        final java.lang.Object other$aiReview = other.getAiReview();
        if (this$aiReview == null ? other$aiReview != null : !this$aiReview.equals(other$aiReview)) return false;
        final java.lang.Object this$relatedBehavioralInsights = this.getRelatedBehavioralInsights();
        final java.lang.Object other$relatedBehavioralInsights = other.getRelatedBehavioralInsights();
        if (this$relatedBehavioralInsights == null ? other$relatedBehavioralInsights != null : !this$relatedBehavioralInsights.equals(other$relatedBehavioralInsights)) return false;
        final java.lang.Object this$disciplineViolations = this.getDisciplineViolations();
        final java.lang.Object other$disciplineViolations = other.getDisciplineViolations();
        if (this$disciplineViolations == null ? other$disciplineViolations != null : !this$disciplineViolations.equals(other$disciplineViolations)) return false;
        return true;
    }

    @java.lang.SuppressWarnings("all")
    protected boolean canEqual(final java.lang.Object other) {
        return other instanceof TradeDetailResponse;
    }

    @java.lang.Override
    @java.lang.SuppressWarnings("all")
    public int hashCode() {
        final int PRIME = 59;
        int result = 1;
        final java.lang.Object $previousTradeId = this.getPreviousTradeId();
        result = result * PRIME + ($previousTradeId == null ? 43 : $previousTradeId.hashCode());
        final java.lang.Object $nextTradeId = this.getNextTradeId();
        result = result * PRIME + ($nextTradeId == null ? 43 : $nextTradeId.hashCode());
        final java.lang.Object $aiReviewCached = this.getAiReviewCached();
        result = result * PRIME + ($aiReviewCached == null ? 43 : $aiReviewCached.hashCode());
        final java.lang.Object $trade = this.getTrade();
        result = result * PRIME + ($trade == null ? 43 : $trade.hashCode());
        final java.lang.Object $aiReview = this.getAiReview();
        result = result * PRIME + ($aiReview == null ? 43 : $aiReview.hashCode());
        final java.lang.Object $relatedBehavioralInsights = this.getRelatedBehavioralInsights();
        result = result * PRIME + ($relatedBehavioralInsights == null ? 43 : $relatedBehavioralInsights.hashCode());
        final java.lang.Object $disciplineViolations = this.getDisciplineViolations();
        result = result * PRIME + ($disciplineViolations == null ? 43 : $disciplineViolations.hashCode());
        return result;
    }

    @java.lang.Override
    @java.lang.SuppressWarnings("all")
    public java.lang.String toString() {
        return "TradeDetailResponse(trade=" + this.getTrade() + ", previousTradeId=" + this.getPreviousTradeId() + ", nextTradeId=" + this.getNextTradeId() + ", aiReview=" + this.getAiReview() + ", aiReviewCached=" + this.getAiReviewCached() + ", relatedBehavioralInsights=" + this.getRelatedBehavioralInsights() + ", disciplineViolations=" + this.getDisciplineViolations() + ")";
    }
}
