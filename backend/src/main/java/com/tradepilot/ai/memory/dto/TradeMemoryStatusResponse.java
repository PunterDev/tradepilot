package com.tradepilot.ai.memory.dto;

import java.time.OffsetDateTime;

public record TradeMemoryStatusResponse(
        Long tradeId,
        TradingMemoryStatus status,
        OffsetDateTime indexedAt,
        String message
) {
}
