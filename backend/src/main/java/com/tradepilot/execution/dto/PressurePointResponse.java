package com.tradepilot.execution.dto;

import java.math.BigDecimal;

public record PressurePointResponse(
        boolean detected,
        BigDecimal thresholdR,
        Integer beforeAverageExecutionScore,
        Integer afterAverageExecutionScore,
        BigDecimal beforeAverageRiskR,
        BigDecimal afterAverageRiskR,
        BigDecimal tradeFrequencyChangePercent,
        int sampleSize,
        String message
) {
}
