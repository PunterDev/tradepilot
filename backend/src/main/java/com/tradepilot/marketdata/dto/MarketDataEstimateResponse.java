package com.tradepilot.marketdata.dto;

import java.math.BigDecimal;
import java.time.LocalDate;

public record MarketDataEstimateResponse(
        String instrument,
        String continuousSymbol,
        LocalDate startDate,
        LocalDate endDate,
        BigDecimal estimatedCostUsd,
        long requestedDays,
        int missingChunks
) {
}
