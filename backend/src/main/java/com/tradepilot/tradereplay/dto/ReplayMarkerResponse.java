package com.tradepilot.tradereplay.dto;

import java.math.BigDecimal;
import java.time.Instant;

public record ReplayMarkerResponse(
        String type,
        Instant timestamp,
        BigDecimal price,
        Integer quantity,
        String label
) {}
