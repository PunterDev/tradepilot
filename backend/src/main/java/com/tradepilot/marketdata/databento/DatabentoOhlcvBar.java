package com.tradepilot.marketdata.databento;

import java.math.BigDecimal;
import java.time.Instant;

public record DatabentoOhlcvBar(
        Instant tsEvent,
        long instrumentId,
        BigDecimal open,
        BigDecimal high,
        BigDecimal low,
        BigDecimal close,
        long volume
) {
}
