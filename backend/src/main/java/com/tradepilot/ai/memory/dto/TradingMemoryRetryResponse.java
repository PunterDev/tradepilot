package com.tradepilot.ai.memory.dto;

public record TradingMemoryRetryResponse(
        boolean dryRun,
        long eligible,
        long retried,
        long succeeded,
        long failed,
        long skipped
) {
}
