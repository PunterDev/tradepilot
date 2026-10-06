package com.tradepilot.leakdetector.dto;

import java.math.BigDecimal;

public record BehavioralCounterfactualDto(
        boolean applicable,
        boolean supported,
        int skippedTradeCount,
        int skippedWinnerCount,
        int skippedLoserCount,
        BigDecimal lossesAvoided,
        BigDecimal winnersForgone,
        BigDecimal actualPnl,
        BigDecimal counterfactualPnl,
        BigDecimal netHistoricalImpact
) {}
