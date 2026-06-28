package com.tradepilot.dto.stats;

import java.math.BigDecimal;

public class MetricItemResponse {
    private String label;
    private long totalTrades;
    private long wins;
    private long losses;
    private long breakevens;
    private BigDecimal totalPnl;
    private BigDecimal averageR;
    private BigDecimal winRate;

    @java.lang.SuppressWarnings("all")
    public MetricItemResponse() {
    }

    @java.lang.SuppressWarnings("all")
    public String getLabel() {
        return this.label;
    }

    @java.lang.SuppressWarnings("all")
    public long getTotalTrades() {
        return this.totalTrades;
    }

    @java.lang.SuppressWarnings("all")
    public long getWins() {
        return this.wins;
    }

    @java.lang.SuppressWarnings("all")
    public long getLosses() {
        return this.losses;
    }

    @java.lang.SuppressWarnings("all")
    public long getBreakevens() {
        return this.breakevens;
    }

    @java.lang.SuppressWarnings("all")
    public BigDecimal getTotalPnl() {
        return this.totalPnl;
    }

    @java.lang.SuppressWarnings("all")
    public BigDecimal getAverageR() {
        return this.averageR;
    }

    @java.lang.SuppressWarnings("all")
    public BigDecimal getWinRate() {
        return this.winRate;
    }

    @java.lang.SuppressWarnings("all")
    public void setLabel(final String label) {
        this.label = label;
    }

    @java.lang.SuppressWarnings("all")
    public void setTotalTrades(final long totalTrades) {
        this.totalTrades = totalTrades;
    }

    @java.lang.SuppressWarnings("all")
    public void setWins(final long wins) {
        this.wins = wins;
    }

    @java.lang.SuppressWarnings("all")
    public void setLosses(final long losses) {
        this.losses = losses;
    }

    @java.lang.SuppressWarnings("all")
    public void setBreakevens(final long breakevens) {
        this.breakevens = breakevens;
    }

    @java.lang.SuppressWarnings("all")
    public void setTotalPnl(final BigDecimal totalPnl) {
        this.totalPnl = totalPnl;
    }

    @java.lang.SuppressWarnings("all")
    public void setAverageR(final BigDecimal averageR) {
        this.averageR = averageR;
    }

    @java.lang.SuppressWarnings("all")
    public void setWinRate(final BigDecimal winRate) {
        this.winRate = winRate;
    }

    @java.lang.Override
    @java.lang.SuppressWarnings("all")
    public boolean equals(final java.lang.Object o) {
        if (o == this) return true;
        if (!(o instanceof MetricItemResponse)) return false;
        final MetricItemResponse other = (MetricItemResponse) o;
        if (!other.canEqual((java.lang.Object) this)) return false;
        if (this.getTotalTrades() != other.getTotalTrades()) return false;
        if (this.getWins() != other.getWins()) return false;
        if (this.getLosses() != other.getLosses()) return false;
        if (this.getBreakevens() != other.getBreakevens()) return false;
        final java.lang.Object this$label = this.getLabel();
        final java.lang.Object other$label = other.getLabel();
        if (this$label == null ? other$label != null : !this$label.equals(other$label)) return false;
        final java.lang.Object this$totalPnl = this.getTotalPnl();
        final java.lang.Object other$totalPnl = other.getTotalPnl();
        if (this$totalPnl == null ? other$totalPnl != null : !this$totalPnl.equals(other$totalPnl)) return false;
        final java.lang.Object this$averageR = this.getAverageR();
        final java.lang.Object other$averageR = other.getAverageR();
        if (this$averageR == null ? other$averageR != null : !this$averageR.equals(other$averageR)) return false;
        final java.lang.Object this$winRate = this.getWinRate();
        final java.lang.Object other$winRate = other.getWinRate();
        if (this$winRate == null ? other$winRate != null : !this$winRate.equals(other$winRate)) return false;
        return true;
    }

    @java.lang.SuppressWarnings("all")
    protected boolean canEqual(final java.lang.Object other) {
        return other instanceof MetricItemResponse;
    }

    @java.lang.Override
    @java.lang.SuppressWarnings("all")
    public int hashCode() {
        final int PRIME = 59;
        int result = 1;
        final long $totalTrades = this.getTotalTrades();
        result = result * PRIME + (int) ($totalTrades >>> 32 ^ $totalTrades);
        final long $wins = this.getWins();
        result = result * PRIME + (int) ($wins >>> 32 ^ $wins);
        final long $losses = this.getLosses();
        result = result * PRIME + (int) ($losses >>> 32 ^ $losses);
        final long $breakevens = this.getBreakevens();
        result = result * PRIME + (int) ($breakevens >>> 32 ^ $breakevens);
        final java.lang.Object $label = this.getLabel();
        result = result * PRIME + ($label == null ? 43 : $label.hashCode());
        final java.lang.Object $totalPnl = this.getTotalPnl();
        result = result * PRIME + ($totalPnl == null ? 43 : $totalPnl.hashCode());
        final java.lang.Object $averageR = this.getAverageR();
        result = result * PRIME + ($averageR == null ? 43 : $averageR.hashCode());
        final java.lang.Object $winRate = this.getWinRate();
        result = result * PRIME + ($winRate == null ? 43 : $winRate.hashCode());
        return result;
    }

    @java.lang.Override
    @java.lang.SuppressWarnings("all")
    public java.lang.String toString() {
        return "MetricItemResponse(label=" + this.getLabel() + ", totalTrades=" + this.getTotalTrades() + ", wins=" + this.getWins() + ", losses=" + this.getLosses() + ", breakevens=" + this.getBreakevens() + ", totalPnl=" + this.getTotalPnl() + ", averageR=" + this.getAverageR() + ", winRate=" + this.getWinRate() + ")";
    }
}
