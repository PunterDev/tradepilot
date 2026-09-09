package com.tradepilot.activation;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Map;

public record ActivationStatusResponse(
        boolean hasTradingAccount,
        boolean hasImportedTrades,
        long accountCount,
        long tradeCount,
        boolean firstImportCompleted,
        LocalDateTime firstImportAt,
        boolean firstInsightAvailable,
        boolean firstInsightGenerated,
        LocalDateTime firstInsightAt,
        boolean activated,
        Long recommendedAccountId,
        String recommendedNextAction,
        int minimumTradesForLeakAnalysis,
        long additionalTradesNeededForLeakAnalysis,
        int aiCreditsRemaining,
        int aiCreditsMonthlyAllowance,
        int aiCreditsMonthlyUsed,
        int purchasedAiCreditsBalance,
        int totalAiCreditsAvailable,
        boolean aiAllowanceLow,
        Map<String, Integer> featureCosts,
        ProPreviewStatus proPreview,
        FirstInsightSummary firstInsight
) {
    public record ProPreviewStatus(
            String feature,
            boolean available,
            boolean consumed,
            boolean reserved,
            boolean eligible,
            LocalDateTime grantedAt,
            LocalDateTime consumedAt
    ) {
    }

    public record FirstInsightSummary(
            String sourceFeature,
            Long insightId,
            Long accountId,
            String title,
            BigDecimal historicalImpact,
            Integer tradeCount,
            BigDecimal averageImpact,
            String suggestedRule,
            LocalDateTime generatedAt
    ) {
    }
}
