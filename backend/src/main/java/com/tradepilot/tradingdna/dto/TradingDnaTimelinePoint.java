package com.tradepilot.tradingdna.dto;

import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;

@Data
public class TradingDnaTimelinePoint {
    private LocalDate date;
    private Integer tradeCount;
    private BigDecimal winRate;
    private Integer consistencyScore;
    private Integer emotionalStabilityScore;
    private Integer disciplineScore;
}
