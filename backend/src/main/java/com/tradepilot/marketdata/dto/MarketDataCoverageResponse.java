package com.tradepilot.marketdata.dto;

import java.time.Instant;

public record MarketDataCoverageResponse(
        String instrument,
        String continuousSymbol,
        String timeframe,
        Instant firstBar,
        Instant lastBar,
        long barCount
) {
}
