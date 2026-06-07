package com.tradepilot.dto.trade;

import com.tradepilot.dto.ai.AiTradeReviewResponse;
import lombok.Data;

import java.util.List;

@Data
public class TradeDetailResponse {
    private TradeResponse trade;
    private Long previousTradeId;
    private Long nextTradeId;
    private AiTradeReviewResponse aiReview;
    private Boolean aiReviewCached;
    private List<String> relatedBehavioralInsights;
    private List<TradeViolationResponse> disciplineViolations;

    public static TradeDetailResponse of(
            TradeResponse trade,
            Long previousTradeId,
            Long nextTradeId,
            AiTradeReviewResponse aiReview,
            Boolean aiReviewCached,
            List<String> relatedBehavioralInsights,
            List<TradeViolationResponse> disciplineViolations
    ) {
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
}
