package com.tradepilot.dto.stats;

import lombok.*;

import java.math.BigDecimal;

@Data
public class DashboardSummaryResponse {
    private long totalTrades;
    private long wins;
    private long losses;
    private long breakevens;
    private BigDecimal totalPnl;
    private BigDecimal averageR;
    private BigDecimal winRate;
}
