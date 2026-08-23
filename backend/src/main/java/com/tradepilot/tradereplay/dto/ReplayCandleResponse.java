package com.tradepilot.tradereplay.dto;

import java.math.BigDecimal;
import java.time.Instant;

public record ReplayCandleResponse(
        Instant timestamp,
        BigDecimal open,
        BigDecimal high,
        BigDecimal low,
        BigDecimal close,
        BigDecimal volume
) {}
