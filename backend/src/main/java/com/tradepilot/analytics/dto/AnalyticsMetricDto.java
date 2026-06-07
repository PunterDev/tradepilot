package com.tradepilot.analytics.dto;

import java.math.BigDecimal;

public class AnalyticsMetricDto {
    private Integer tradeCount;
    private BigDecimal totalPnl;
    private BigDecimal winRate;
    private BigDecimal avgPnl;
    private BigDecimal avgWin;
    private BigDecimal avgLoss;

    public AnalyticsMetricDto() {
    }

    public AnalyticsMetricDto(Integer tradeCount, BigDecimal totalPnl, BigDecimal winRate, BigDecimal avgPnl, BigDecimal avgWin, BigDecimal avgLoss) {
        this.tradeCount = tradeCount;
        this.totalPnl = totalPnl;
        this.winRate = winRate;
        this.avgPnl = avgPnl;
        this.avgWin = avgWin;
        this.avgLoss = avgLoss;
    }

    public static AnalyticsMetricDto of(Integer tradeCount, BigDecimal totalPnl, BigDecimal winRate, BigDecimal avgPnl, BigDecimal avgWin, BigDecimal avgLoss) {
        return new AnalyticsMetricDto(tradeCount, totalPnl, winRate, avgPnl, avgWin, avgLoss);
    }

    public Integer getTradeCount() { return tradeCount; }
    public void setTradeCount(Integer tradeCount) { this.tradeCount = tradeCount; }
    public BigDecimal getTotalPnl() { return totalPnl; }
    public void setTotalPnl(BigDecimal totalPnl) { this.totalPnl = totalPnl; }
    public BigDecimal getWinRate() { return winRate; }
    public void setWinRate(BigDecimal winRate) { this.winRate = winRate; }
    public BigDecimal getAvgPnl() { return avgPnl; }
    public void setAvgPnl(BigDecimal avgPnl) { this.avgPnl = avgPnl; }
    public BigDecimal getAvgWin() { return avgWin; }
    public void setAvgWin(BigDecimal avgWin) { this.avgWin = avgWin; }
    public BigDecimal getAvgLoss() { return avgLoss; }
    public void setAvgLoss(BigDecimal avgLoss) { this.avgLoss = avgLoss; }
}
