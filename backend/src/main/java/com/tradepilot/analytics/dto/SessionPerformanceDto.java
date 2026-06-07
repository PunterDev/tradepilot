package com.tradepilot.analytics.dto;

import java.math.BigDecimal;

public class SessionPerformanceDto {
    private String session;
    private Integer tradeCount;
    private BigDecimal totalPnl;
    private BigDecimal winRate;
    private BigDecimal avgPnl;

    public SessionPerformanceDto() {
    }

    public SessionPerformanceDto(String session, Integer tradeCount, BigDecimal totalPnl, BigDecimal winRate, BigDecimal avgPnl) {
        this.session = session;
        this.tradeCount = tradeCount;
        this.totalPnl = totalPnl;
        this.winRate = winRate;
        this.avgPnl = avgPnl;
    }

    public static SessionPerformanceDto of(String session, Integer tradeCount, BigDecimal totalPnl, BigDecimal winRate, BigDecimal avgPnl) {
        return new SessionPerformanceDto(session, tradeCount, totalPnl, winRate, avgPnl);
    }

    public String getSession() { return session; }
    public void setSession(String session) { this.session = session; }
    public Integer getTradeCount() { return tradeCount; }
    public void setTradeCount(Integer tradeCount) { this.tradeCount = tradeCount; }
    public BigDecimal getTotalPnl() { return totalPnl; }
    public void setTotalPnl(BigDecimal totalPnl) { this.totalPnl = totalPnl; }
    public BigDecimal getWinRate() { return winRate; }
    public void setWinRate(BigDecimal winRate) { this.winRate = winRate; }
    public BigDecimal getAvgPnl() { return avgPnl; }
    public void setAvgPnl(BigDecimal avgPnl) { this.avgPnl = avgPnl; }
}
