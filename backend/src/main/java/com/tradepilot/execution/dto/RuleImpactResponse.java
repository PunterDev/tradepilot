package com.tradepilot.execution.dto;

import java.math.BigDecimal;

public record RuleImpactResponse(
        String ruleCode,
        String ruleTitle,
        long occurrences,
        BigDecimal netR,
        BigDecimal averageR,
        BigDecimal winRate,
        BigDecimal averageExecutionScore,
        BigDecimal compliantAverageR,
        BigDecimal estimatedImpactR
) {
}
