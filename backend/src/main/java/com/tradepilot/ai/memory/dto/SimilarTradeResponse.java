package com.tradepilot.ai.memory.dto;

import java.math.BigDecimal;
import java.time.LocalDate;

public record SimilarTradeResponse(
        Long tradeId,
        LocalDate tradeDate,
        String instrument,
        String direction,
        String setup,
        BigDecimal resultR,
        BigDecimal maeR,
        BigDecimal mfeR,
        String mistake,
        String emotion,
        Double similarity,
        String summary
) {
}
