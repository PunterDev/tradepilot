package com.tradepilot.ai.memory.vector.model;

import java.time.LocalDate;
import java.util.List;

public record TradingMemorySearchRequest(
        Long userId,
        List<Double> queryVector,
        Long excludeTradeId,
        int limit,
        Long accountId,
        String setup,
        String instrument,
        String direction,
        String session,
        LocalDate tradeDateFrom
) {
}
