package com.tradepilot.ai.memory.dto;

public record TradingMemoryBackfillResponse(
        long eligible,
        long alreadyIndexed,
        long stale,
        long queued,
        long failed,
        long failedToQueue,
        String nextCursor,
        boolean dryRun
) {
}
