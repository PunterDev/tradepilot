package com.tradepilot.tradereplay.service;

import java.math.BigDecimal;
import java.time.Instant;

public record HistoricalCandle(
        Instant timestamp,
        BigDecimal open,
        BigDecimal high,
        BigDecimal low,
        BigDecimal close,
        BigDecimal volume
) {}
