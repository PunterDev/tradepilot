package com.tradepilot.tradereplay.dto;

public record TradeReplayResetResult(
        Long tradeId,
        long replaysDeleted,
        long insightsDeleted,
        int cacheEntriesEvicted,
        boolean clearedTradeReplayCache
) {
}
