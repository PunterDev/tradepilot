package com.tradepilot.execution.dto;

import java.math.BigDecimal;

public record BehaviorPatternResponse(
        String patternKey,
        String label,
        long occurrences,
        BigDecimal netR,
        BigDecimal winRate,
        BigDecimal averageExecutionScore,
        BigDecimal averageTimeBetweenTradesMinutes,
        BigDecimal averageRiskChangePercent,
        String insight
) {
}
