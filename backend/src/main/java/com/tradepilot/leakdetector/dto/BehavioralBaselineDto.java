package com.tradepilot.leakdetector.dto;

import java.math.BigDecimal;

public record BehavioralBaselineDto(
        int sampleSize,
        BigDecimal winRate,
        BigDecimal expectancyR,
        BigDecimal averagePnl
) {}
