package com.tradepilot.execution.dto;

import java.math.BigDecimal;

public record SetupExecutionStatsResponse(
        String setup,
        long samples,
        long compliantSamples,
        long nonCompliantSamples,
        BigDecimal winRate,
        BigDecimal averageR,
        BigDecimal averageWinnerR,
        BigDecimal averageLoserR,
        BigDecimal expectancyR,
        BigDecimal averageExecutionScore,
        String recentExecutionTrend,
        SampleConfidenceLabel sampleConfidence
) {
}
