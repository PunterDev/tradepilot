package com.tradepilot.tradereplay.service;

import com.tradepilot.tradereplay.entity.TradeReplayInsightType;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.Map;

public record TradeReplayAnalysis(
        Map<String, Object> metrics,
        List<InsightDraft> insights,
        String summary,
        String reflectionQuestion
) {
    public record InsightDraft(
            TradeReplayInsightType type,
            Instant timestamp,
            BigDecimal price,
            String severity,
            String title,
            String explanation,
            Map<String, Object> supportingMetrics,
            int order
    ) {}
}
