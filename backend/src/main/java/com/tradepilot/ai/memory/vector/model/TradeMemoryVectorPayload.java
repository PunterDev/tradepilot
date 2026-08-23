package com.tradepilot.ai.memory.vector.model;

import java.time.LocalDate;
import java.util.UUID;

public record TradeMemoryVectorPayload(
        UUID pointId,
        Long tradeId,
        Long userId,
        String instrument,
        String direction,
        String setup,
        String mistake,
        String emotion,
        String session,
        LocalDate tradeDate,
        Integer memoryVersion
) {
}
