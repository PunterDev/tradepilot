package com.tradepilot.dev;

import java.util.List;

public record DemoDataReseedResult(
        boolean dryRun,
        int targetDemoDataVersion,
        List<Integer> existingDemoDataVersions,
        String demoTradeKeyPrefix,
        long existingDemoTrades,
        long tradesToDelete,
        int tradesToCreate,
        long aiReviewsToDelete,
        long aiAnalysesToDelete,
        long tradeReplaysToDelete,
        long tradeReplayInsightsToDelete,
        long tradeMemoriesToDelete,
        long ruleViolationsToDelete,
        long tradesDeleted,
        int tradesCreated,
        int aiReviewsCreated,
        int aiAnalysesCreated,
        int ruleViolationsCreated,
        int accountsCreated,
        String message
) {
}
