package com.tradepilot.analytics.dto;

import java.math.BigDecimal;

public class HeatmapCellDto {
    private String dayOfWeek;
    private String session;
    private String sessionBucket;
    private Integer tradeCount;
    private Integer wins;
    private Integer losses;
    private BigDecimal totalPnl;
    private BigDecimal pnl;
    private BigDecimal winRate;

    public HeatmapCellDto() {
    }

    public HeatmapCellDto(String dayOfWeek, String session, String sessionBucket, Integer tradeCount, Integer wins, Integer losses, BigDecimal totalPnl, BigDecimal winRate) {
        this.dayOfWeek = dayOfWeek;
        this.session = session;
        this.sessionBucket = sessionBucket;
        this.tradeCount = tradeCount;
        this.wins = wins;
        this.losses = losses;
        this.totalPnl = totalPnl;
        this.pnl = totalPnl;
        this.winRate = winRate;
    }

    public static HeatmapCellDto of(String dayOfWeek, String sessionBucket, Integer tradeCount, Integer wins, Integer losses, BigDecimal totalPnl, BigDecimal winRate) {
        return new HeatmapCellDto(dayOfWeek, sessionBucket, sessionBucket, tradeCount, wins, losses, totalPnl, winRate);
    }

    public String getDayOfWeek() { return dayOfWeek; }
    public void setDayOfWeek(String dayOfWeek) { this.dayOfWeek = dayOfWeek; }
    public String getSession() { return session; }
    public void setSession(String session) { this.session = session; }
    public String getSessionBucket() { return sessionBucket; }
    public void setSessionBucket(String sessionBucket) { this.sessionBucket = sessionBucket; }
    public Integer getTradeCount() { return tradeCount; }
    public void setTradeCount(Integer tradeCount) { this.tradeCount = tradeCount; }
    public Integer getWins() { return wins; }
    public void setWins(Integer wins) { this.wins = wins; }
    public Integer getLosses() { return losses; }
    public void setLosses(Integer losses) { this.losses = losses; }
    public BigDecimal getTotalPnl() { return totalPnl; }
    public void setTotalPnl(BigDecimal totalPnl) { this.totalPnl = totalPnl; }
    public BigDecimal getPnl() { return pnl; }
    public void setPnl(BigDecimal pnl) { this.pnl = pnl; }
    public BigDecimal getWinRate() { return winRate; }
    public void setWinRate(BigDecimal winRate) { this.winRate = winRate; }
}
