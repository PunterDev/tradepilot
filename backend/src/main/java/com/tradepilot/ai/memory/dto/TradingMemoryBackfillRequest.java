package com.tradepilot.ai.memory.dto;

public record TradingMemoryBackfillRequest(
        Boolean dryRun,
        Integer limit,
        Boolean onlyReviewedTrades
) {
}
