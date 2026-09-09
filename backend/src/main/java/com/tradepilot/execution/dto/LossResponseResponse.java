package com.tradepilot.execution.dto;

import java.math.BigDecimal;
import java.util.List;

public record LossResponseResponse(
        Long losingTradeId,
        Long nextTradeId,
        Integer score,
        LossResponseClassification classification,
        Long timeToNextTradeMinutes,
        Long baselineTimeBetweenTradesMinutes,
        BigDecimal riskChangePercent,
        BigDecimal sizeChangePercent,
        Integer nextTradeExecutionScore,
        List<String> flags,
        String message
) {
}
