package com.tradepilot.marketdata.dto;

import java.time.Instant;
import java.time.LocalDate;

public record MarketDataDownloadResponse(
        String instrument,
        String continuousSymbol,
        LocalDate requestedStartDate,
        LocalDate requestedEndDate,
        long downloadedBars,
        long skippedExistingBars,
        long insertedBars,
        Instant coverageStart,
        Instant coverageEnd,
        String status
) {
}
