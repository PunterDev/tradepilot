package com.tradepilot.ai.memory.dto;

import java.time.OffsetDateTime;
import java.util.List;

public record TradingMemoryResponse(
        Long tradeId,
        TradingMemoryStatus status,
        OffsetDateTime indexedAt,
        Integer similarTradeCount,
        TradingMemoryMetricsResponse metrics,
        CommonBehaviorResponse commonBehavior,
        RecurringPatternResponse recurringPattern,
        List<SimilarTradeResponse> similarTrades,
        TradingMemoryAiInsightResponse aiInsight,
        String message,
        Boolean cached
) {
}
