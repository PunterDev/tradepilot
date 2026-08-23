package com.tradepilot.playbook.dto;

import java.math.BigDecimal;
import java.time.LocalDate;

public record PlaybookMetricsResponse(
        long totalLinkedTrades,
        BigDecimal totalR,
        BigDecimal netPnl,
        BigDecimal winRate,
        BigDecimal lossRate,
        BigDecimal scratchRate,
        BigDecimal profitFactor,
        BigDecimal averageR,
        BigDecimal expectancy,
        BigDecimal averageWinnerR,
        BigDecimal averageLoserR,
        BigDecimal largestWinnerR,
        BigDecimal largestLoserR,
        BigDecimal maxDrawdownR,
        int consecutiveWins,
        int consecutiveLosses,
        BigDecimal winningDayPercentage,
        int activeTradingDays,
        int compliantTrades,
        int noncompliantTrades,
        BigDecimal compliantAverageR,
        BigDecimal noncompliantAverageR,
        BigDecimal compliantWinRate,
        BigDecimal noncompliantWinRate,
        BigDecimal totalRDifference,
        BigDecimal dataCompletenessScore,
        LocalDate lastUsedDate
) {
}
