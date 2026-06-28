package com.tradepilot.tradingdna.dto;

import java.math.BigDecimal;
import java.time.LocalDate;

public class TradingDnaTimelinePoint {
    private LocalDate date;
    private Integer tradeCount;
    private BigDecimal winRate;
    private Integer consistencyScore;
    private Integer emotionalStabilityScore;
    private Integer disciplineScore;

    @java.lang.SuppressWarnings("all")
    public TradingDnaTimelinePoint() {
    }

    @java.lang.SuppressWarnings("all")
    public LocalDate getDate() {
        return this.date;
    }

    @java.lang.SuppressWarnings("all")
    public Integer getTradeCount() {
        return this.tradeCount;
    }

    @java.lang.SuppressWarnings("all")
    public BigDecimal getWinRate() {
        return this.winRate;
    }

    @java.lang.SuppressWarnings("all")
    public Integer getConsistencyScore() {
        return this.consistencyScore;
    }

    @java.lang.SuppressWarnings("all")
    public Integer getEmotionalStabilityScore() {
        return this.emotionalStabilityScore;
    }

    @java.lang.SuppressWarnings("all")
    public Integer getDisciplineScore() {
        return this.disciplineScore;
    }

    @java.lang.SuppressWarnings("all")
    public void setDate(final LocalDate date) {
        this.date = date;
    }

    @java.lang.SuppressWarnings("all")
    public void setTradeCount(final Integer tradeCount) {
        this.tradeCount = tradeCount;
    }

    @java.lang.SuppressWarnings("all")
    public void setWinRate(final BigDecimal winRate) {
        this.winRate = winRate;
    }

    @java.lang.SuppressWarnings("all")
    public void setConsistencyScore(final Integer consistencyScore) {
        this.consistencyScore = consistencyScore;
    }

    @java.lang.SuppressWarnings("all")
    public void setEmotionalStabilityScore(final Integer emotionalStabilityScore) {
        this.emotionalStabilityScore = emotionalStabilityScore;
    }

    @java.lang.SuppressWarnings("all")
    public void setDisciplineScore(final Integer disciplineScore) {
        this.disciplineScore = disciplineScore;
    }

    @java.lang.Override
    @java.lang.SuppressWarnings("all")
    public boolean equals(final java.lang.Object o) {
        if (o == this) return true;
        if (!(o instanceof TradingDnaTimelinePoint)) return false;
        final TradingDnaTimelinePoint other = (TradingDnaTimelinePoint) o;
        if (!other.canEqual((java.lang.Object) this)) return false;
        final java.lang.Object this$tradeCount = this.getTradeCount();
        final java.lang.Object other$tradeCount = other.getTradeCount();
        if (this$tradeCount == null ? other$tradeCount != null : !this$tradeCount.equals(other$tradeCount)) return false;
        final java.lang.Object this$consistencyScore = this.getConsistencyScore();
        final java.lang.Object other$consistencyScore = other.getConsistencyScore();
        if (this$consistencyScore == null ? other$consistencyScore != null : !this$consistencyScore.equals(other$consistencyScore)) return false;
        final java.lang.Object this$emotionalStabilityScore = this.getEmotionalStabilityScore();
        final java.lang.Object other$emotionalStabilityScore = other.getEmotionalStabilityScore();
        if (this$emotionalStabilityScore == null ? other$emotionalStabilityScore != null : !this$emotionalStabilityScore.equals(other$emotionalStabilityScore)) return false;
        final java.lang.Object this$disciplineScore = this.getDisciplineScore();
        final java.lang.Object other$disciplineScore = other.getDisciplineScore();
        if (this$disciplineScore == null ? other$disciplineScore != null : !this$disciplineScore.equals(other$disciplineScore)) return false;
        final java.lang.Object this$date = this.getDate();
        final java.lang.Object other$date = other.getDate();
        if (this$date == null ? other$date != null : !this$date.equals(other$date)) return false;
        final java.lang.Object this$winRate = this.getWinRate();
        final java.lang.Object other$winRate = other.getWinRate();
        if (this$winRate == null ? other$winRate != null : !this$winRate.equals(other$winRate)) return false;
        return true;
    }

    @java.lang.SuppressWarnings("all")
    protected boolean canEqual(final java.lang.Object other) {
        return other instanceof TradingDnaTimelinePoint;
    }

    @java.lang.Override
    @java.lang.SuppressWarnings("all")
    public int hashCode() {
        final int PRIME = 59;
        int result = 1;
        final java.lang.Object $tradeCount = this.getTradeCount();
        result = result * PRIME + ($tradeCount == null ? 43 : $tradeCount.hashCode());
        final java.lang.Object $consistencyScore = this.getConsistencyScore();
        result = result * PRIME + ($consistencyScore == null ? 43 : $consistencyScore.hashCode());
        final java.lang.Object $emotionalStabilityScore = this.getEmotionalStabilityScore();
        result = result * PRIME + ($emotionalStabilityScore == null ? 43 : $emotionalStabilityScore.hashCode());
        final java.lang.Object $disciplineScore = this.getDisciplineScore();
        result = result * PRIME + ($disciplineScore == null ? 43 : $disciplineScore.hashCode());
        final java.lang.Object $date = this.getDate();
        result = result * PRIME + ($date == null ? 43 : $date.hashCode());
        final java.lang.Object $winRate = this.getWinRate();
        result = result * PRIME + ($winRate == null ? 43 : $winRate.hashCode());
        return result;
    }

    @java.lang.Override
    @java.lang.SuppressWarnings("all")
    public java.lang.String toString() {
        return "TradingDnaTimelinePoint(date=" + this.getDate() + ", tradeCount=" + this.getTradeCount() + ", winRate=" + this.getWinRate() + ", consistencyScore=" + this.getConsistencyScore() + ", emotionalStabilityScore=" + this.getEmotionalStabilityScore() + ", disciplineScore=" + this.getDisciplineScore() + ")";
    }
}
