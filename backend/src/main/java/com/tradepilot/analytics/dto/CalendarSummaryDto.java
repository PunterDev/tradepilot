package com.tradepilot.analytics.dto;

import java.math.BigDecimal;
import java.time.LocalDate;

public class CalendarSummaryDto {
    private LocalDate date;
    private long tradeCount;
    private BigDecimal netPnl;
    private BigDecimal totalR;
    private long winningTrades;
    private long losingTrades;
    private long breakevenTrades;

    public CalendarSummaryDto() {
    }

    public CalendarSummaryDto(LocalDate date, long tradeCount, BigDecimal netPnl, BigDecimal totalR, long winningTrades, long losingTrades, long breakevenTrades) {
        this.date = date;
        this.tradeCount = tradeCount;
        this.netPnl = netPnl;
        this.totalR = totalR;
        this.winningTrades = winningTrades;
        this.losingTrades = losingTrades;
        this.breakevenTrades = breakevenTrades;
    }

    public static CalendarSummaryDto of(LocalDate date, long tradeCount, BigDecimal netPnl, BigDecimal totalR, long winningTrades, long losingTrades, long breakevenTrades) {
        return new CalendarSummaryDto(date, tradeCount, netPnl, totalR, winningTrades, losingTrades, breakevenTrades);
    }

    public LocalDate getDate() { return date; }
    public void setDate(LocalDate date) { this.date = date; }
    public long getTradeCount() { return tradeCount; }
    public void setTradeCount(long tradeCount) { this.tradeCount = tradeCount; }
    public BigDecimal getNetPnl() { return netPnl; }
    public void setNetPnl(BigDecimal netPnl) { this.netPnl = netPnl; }
    public BigDecimal getTotalR() { return totalR; }
    public void setTotalR(BigDecimal totalR) { this.totalR = totalR; }
    public long getWinningTrades() { return winningTrades; }
    public void setWinningTrades(long winningTrades) { this.winningTrades = winningTrades; }
    public long getLosingTrades() { return losingTrades; }
    public void setLosingTrades(long losingTrades) { this.losingTrades = losingTrades; }
    public long getBreakevenTrades() { return breakevenTrades; }
    public void setBreakevenTrades(long breakevenTrades) { this.breakevenTrades = breakevenTrades; }
}
