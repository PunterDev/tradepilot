package com.tradepilot.tradereplay.dto;

import com.tradepilot.tradereplay.entity.TradeReplayInsightType;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.Map;

public record ReplayInsightResponse(
        Long id,
        TradeReplayInsightType type,
        Instant eventTimestamp,
        BigDecimal eventPrice,
        String severity,
        String title,
        String explanation,
        Map<String, Object> supportingMetrics,
        String aiExplanation,
        int displayOrder,
        boolean primary
) {}
