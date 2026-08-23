package com.tradepilot.ai.memory.dto;

import java.util.List;

public record TradingMemoryRetryRequest(
        Boolean dryRun,
        Integer limit,
        List<String> errorCodes
) {
}
