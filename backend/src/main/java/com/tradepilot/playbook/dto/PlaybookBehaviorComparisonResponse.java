package com.tradepilot.playbook.dto;

import java.math.BigDecimal;

public record PlaybookBehaviorComparisonResponse(
        Long checklistItemId,
        String rule,
        long metTradeCount,
        long notMetTradeCount,
        BigDecimal metAverageR,
        BigDecimal notMetAverageR,
        BigDecimal metWinRate,
        BigDecimal notMetWinRate,
        String confidence,
        boolean directionallyMeaningful,
        String interpretation
) {
}
