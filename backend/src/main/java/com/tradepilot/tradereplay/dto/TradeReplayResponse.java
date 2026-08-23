package com.tradepilot.tradereplay.dto;

import com.tradepilot.tradereplay.entity.TradeReplayStatus;
import java.time.Instant;
import java.util.List;
import java.util.Map;

public record TradeReplayResponse(
        Long tradeId,
        Long replayId,
        TradeReplayStatus status,
        Instant eligibleAt,
        String originalBrokerSymbol,
        String resolvedProviderSymbol,
        String provider,
        String candleInterval,
        Instant windowStart,
        Instant windowEnd,
        Instant generatedAt,
        String failureCode,
        String message,
        List<ReplayMarkerResponse> markers,
        List<ReplayCandleResponse> candles,
        Map<String, Object> metrics,
        List<ReplayInsightResponse> insights,
        String aiSummary,
        String reflectionQuestion,
        List<String> dataQualityWarnings
) {}
