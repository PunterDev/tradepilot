package com.tradepilot.ai.memory.dto;

import java.math.BigDecimal;
import java.util.List;

public record RecurringPatternResponse(
        boolean detected,
        String patternType,
        String title,
        Integer occurrences,
        Integer sampleSize,
        BigDecimal occurrenceRate,
        BigDecimal averageRealizedR,
        BigDecimal averageMfeR,
        BigDecimal averageOpportunityR,
        BigDecimal totalEstimatedOpportunityR,
        String message,
        List<Long> supportingTradeIds
) {
    public static RecurringPatternResponse none(int sampleSize) {
        return new RecurringPatternResponse(false, null, null, 0, sampleSize, null, null, null, null, null, null, List.of());
    }
}
