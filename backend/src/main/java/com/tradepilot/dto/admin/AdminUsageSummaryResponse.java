package com.tradepilot.dto.admin;

public class AdminUsageSummaryResponse {
    private long totalAiReviews;
    private long dailyCoachUsage;
    private long weeklyCoachUsage;
    private long tradingDnaUsage;
    private long totalMonthlyAiUsage;

    @java.lang.SuppressWarnings("all")
    public AdminUsageSummaryResponse() {
    }

    @java.lang.SuppressWarnings("all")
    public long getTotalAiReviews() {
        return this.totalAiReviews;
    }

    @java.lang.SuppressWarnings("all")
    public long getDailyCoachUsage() {
        return this.dailyCoachUsage;
    }

    @java.lang.SuppressWarnings("all")
    public long getWeeklyCoachUsage() {
        return this.weeklyCoachUsage;
    }

    @java.lang.SuppressWarnings("all")
    public long getTradingDnaUsage() {
        return this.tradingDnaUsage;
    }

    @java.lang.SuppressWarnings("all")
    public long getTotalMonthlyAiUsage() {
        return this.totalMonthlyAiUsage;
    }

    @java.lang.SuppressWarnings("all")
    public void setTotalAiReviews(final long totalAiReviews) {
        this.totalAiReviews = totalAiReviews;
    }

    @java.lang.SuppressWarnings("all")
    public void setDailyCoachUsage(final long dailyCoachUsage) {
        this.dailyCoachUsage = dailyCoachUsage;
    }

    @java.lang.SuppressWarnings("all")
    public void setWeeklyCoachUsage(final long weeklyCoachUsage) {
        this.weeklyCoachUsage = weeklyCoachUsage;
    }

    @java.lang.SuppressWarnings("all")
    public void setTradingDnaUsage(final long tradingDnaUsage) {
        this.tradingDnaUsage = tradingDnaUsage;
    }

    @java.lang.SuppressWarnings("all")
    public void setTotalMonthlyAiUsage(final long totalMonthlyAiUsage) {
        this.totalMonthlyAiUsage = totalMonthlyAiUsage;
    }

    @java.lang.Override
    @java.lang.SuppressWarnings("all")
    public boolean equals(final java.lang.Object o) {
        if (o == this) return true;
        if (!(o instanceof AdminUsageSummaryResponse)) return false;
        final AdminUsageSummaryResponse other = (AdminUsageSummaryResponse) o;
        if (!other.canEqual((java.lang.Object) this)) return false;
        if (this.getTotalAiReviews() != other.getTotalAiReviews()) return false;
        if (this.getDailyCoachUsage() != other.getDailyCoachUsage()) return false;
        if (this.getWeeklyCoachUsage() != other.getWeeklyCoachUsage()) return false;
        if (this.getTradingDnaUsage() != other.getTradingDnaUsage()) return false;
        if (this.getTotalMonthlyAiUsage() != other.getTotalMonthlyAiUsage()) return false;
        return true;
    }

    @java.lang.SuppressWarnings("all")
    protected boolean canEqual(final java.lang.Object other) {
        return other instanceof AdminUsageSummaryResponse;
    }

    @java.lang.Override
    @java.lang.SuppressWarnings("all")
    public int hashCode() {
        final int PRIME = 59;
        int result = 1;
        final long $totalAiReviews = this.getTotalAiReviews();
        result = result * PRIME + (int) ($totalAiReviews >>> 32 ^ $totalAiReviews);
        final long $dailyCoachUsage = this.getDailyCoachUsage();
        result = result * PRIME + (int) ($dailyCoachUsage >>> 32 ^ $dailyCoachUsage);
        final long $weeklyCoachUsage = this.getWeeklyCoachUsage();
        result = result * PRIME + (int) ($weeklyCoachUsage >>> 32 ^ $weeklyCoachUsage);
        final long $tradingDnaUsage = this.getTradingDnaUsage();
        result = result * PRIME + (int) ($tradingDnaUsage >>> 32 ^ $tradingDnaUsage);
        final long $totalMonthlyAiUsage = this.getTotalMonthlyAiUsage();
        result = result * PRIME + (int) ($totalMonthlyAiUsage >>> 32 ^ $totalMonthlyAiUsage);
        return result;
    }

    @java.lang.Override
    @java.lang.SuppressWarnings("all")
    public java.lang.String toString() {
        return "AdminUsageSummaryResponse(totalAiReviews=" + this.getTotalAiReviews() + ", dailyCoachUsage=" + this.getDailyCoachUsage() + ", weeklyCoachUsage=" + this.getWeeklyCoachUsage() + ", tradingDnaUsage=" + this.getTradingDnaUsage() + ", totalMonthlyAiUsage=" + this.getTotalMonthlyAiUsage() + ")";
    }
}
