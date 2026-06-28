package com.tradepilot.dto.admin;

import java.time.LocalDate;

public class AdminTradeCleanupResponse {
    private Long userId;
    private Long accountId;
    private LocalDate startDate;
    private LocalDate endDate;
    private boolean dryRun;
    private long tradesMatched;
    private long tradesDeleted;
    private long tradeReviewsDeleted;
    private long tradeAnalysesDeleted;
    private long tradeRuleViolationsDeleted;
    private long dailyDisciplineScoresDeleted;
    private long sessionReviewsDeleted;
    private long sessionCachesInvalidated;
    private long leakSnapshotsDeleted;
    private long tradingDnaSnapshotsDeleted;
    private long dailyCoachSnapshotsDeleted;
    private long weeklyCoachSnapshotsDeleted;

    @java.lang.SuppressWarnings("all")
    public Long getUserId() {
        return this.userId;
    }

    @java.lang.SuppressWarnings("all")
    public Long getAccountId() {
        return this.accountId;
    }

    @java.lang.SuppressWarnings("all")
    public LocalDate getStartDate() {
        return this.startDate;
    }

    @java.lang.SuppressWarnings("all")
    public LocalDate getEndDate() {
        return this.endDate;
    }

    @java.lang.SuppressWarnings("all")
    public boolean isDryRun() {
        return this.dryRun;
    }

    @java.lang.SuppressWarnings("all")
    public long getTradesMatched() {
        return this.tradesMatched;
    }

    @java.lang.SuppressWarnings("all")
    public long getTradesDeleted() {
        return this.tradesDeleted;
    }

    @java.lang.SuppressWarnings("all")
    public long getTradeReviewsDeleted() {
        return this.tradeReviewsDeleted;
    }

    @java.lang.SuppressWarnings("all")
    public long getTradeAnalysesDeleted() {
        return this.tradeAnalysesDeleted;
    }

    @java.lang.SuppressWarnings("all")
    public long getTradeRuleViolationsDeleted() {
        return this.tradeRuleViolationsDeleted;
    }

    @java.lang.SuppressWarnings("all")
    public long getDailyDisciplineScoresDeleted() {
        return this.dailyDisciplineScoresDeleted;
    }

    @java.lang.SuppressWarnings("all")
    public long getSessionReviewsDeleted() {
        return this.sessionReviewsDeleted;
    }

    @java.lang.SuppressWarnings("all")
    public long getSessionCachesInvalidated() {
        return this.sessionCachesInvalidated;
    }

    @java.lang.SuppressWarnings("all")
    public long getLeakSnapshotsDeleted() {
        return this.leakSnapshotsDeleted;
    }

    @java.lang.SuppressWarnings("all")
    public long getTradingDnaSnapshotsDeleted() {
        return this.tradingDnaSnapshotsDeleted;
    }

    @java.lang.SuppressWarnings("all")
    public long getDailyCoachSnapshotsDeleted() {
        return this.dailyCoachSnapshotsDeleted;
    }

    @java.lang.SuppressWarnings("all")
    public long getWeeklyCoachSnapshotsDeleted() {
        return this.weeklyCoachSnapshotsDeleted;
    }

    @java.lang.SuppressWarnings("all")
    public void setUserId(final Long userId) {
        this.userId = userId;
    }

    @java.lang.SuppressWarnings("all")
    public void setAccountId(final Long accountId) {
        this.accountId = accountId;
    }

    @java.lang.SuppressWarnings("all")
    public void setStartDate(final LocalDate startDate) {
        this.startDate = startDate;
    }

    @java.lang.SuppressWarnings("all")
    public void setEndDate(final LocalDate endDate) {
        this.endDate = endDate;
    }

    @java.lang.SuppressWarnings("all")
    public void setDryRun(final boolean dryRun) {
        this.dryRun = dryRun;
    }

    @java.lang.SuppressWarnings("all")
    public void setTradesMatched(final long tradesMatched) {
        this.tradesMatched = tradesMatched;
    }

    @java.lang.SuppressWarnings("all")
    public void setTradesDeleted(final long tradesDeleted) {
        this.tradesDeleted = tradesDeleted;
    }

    @java.lang.SuppressWarnings("all")
    public void setTradeReviewsDeleted(final long tradeReviewsDeleted) {
        this.tradeReviewsDeleted = tradeReviewsDeleted;
    }

    @java.lang.SuppressWarnings("all")
    public void setTradeAnalysesDeleted(final long tradeAnalysesDeleted) {
        this.tradeAnalysesDeleted = tradeAnalysesDeleted;
    }

    @java.lang.SuppressWarnings("all")
    public void setTradeRuleViolationsDeleted(final long tradeRuleViolationsDeleted) {
        this.tradeRuleViolationsDeleted = tradeRuleViolationsDeleted;
    }

    @java.lang.SuppressWarnings("all")
    public void setDailyDisciplineScoresDeleted(final long dailyDisciplineScoresDeleted) {
        this.dailyDisciplineScoresDeleted = dailyDisciplineScoresDeleted;
    }

    @java.lang.SuppressWarnings("all")
    public void setSessionReviewsDeleted(final long sessionReviewsDeleted) {
        this.sessionReviewsDeleted = sessionReviewsDeleted;
    }

    @java.lang.SuppressWarnings("all")
    public void setSessionCachesInvalidated(final long sessionCachesInvalidated) {
        this.sessionCachesInvalidated = sessionCachesInvalidated;
    }

    @java.lang.SuppressWarnings("all")
    public void setLeakSnapshotsDeleted(final long leakSnapshotsDeleted) {
        this.leakSnapshotsDeleted = leakSnapshotsDeleted;
    }

    @java.lang.SuppressWarnings("all")
    public void setTradingDnaSnapshotsDeleted(final long tradingDnaSnapshotsDeleted) {
        this.tradingDnaSnapshotsDeleted = tradingDnaSnapshotsDeleted;
    }

    @java.lang.SuppressWarnings("all")
    public void setDailyCoachSnapshotsDeleted(final long dailyCoachSnapshotsDeleted) {
        this.dailyCoachSnapshotsDeleted = dailyCoachSnapshotsDeleted;
    }

    @java.lang.SuppressWarnings("all")
    public void setWeeklyCoachSnapshotsDeleted(final long weeklyCoachSnapshotsDeleted) {
        this.weeklyCoachSnapshotsDeleted = weeklyCoachSnapshotsDeleted;
    }

    @java.lang.Override
    @java.lang.SuppressWarnings("all")
    public boolean equals(final java.lang.Object o) {
        if (o == this) return true;
        if (!(o instanceof AdminTradeCleanupResponse)) return false;
        final AdminTradeCleanupResponse other = (AdminTradeCleanupResponse) o;
        if (!other.canEqual((java.lang.Object) this)) return false;
        if (this.isDryRun() != other.isDryRun()) return false;
        if (this.getTradesMatched() != other.getTradesMatched()) return false;
        if (this.getTradesDeleted() != other.getTradesDeleted()) return false;
        if (this.getTradeReviewsDeleted() != other.getTradeReviewsDeleted()) return false;
        if (this.getTradeAnalysesDeleted() != other.getTradeAnalysesDeleted()) return false;
        if (this.getTradeRuleViolationsDeleted() != other.getTradeRuleViolationsDeleted()) return false;
        if (this.getDailyDisciplineScoresDeleted() != other.getDailyDisciplineScoresDeleted()) return false;
        if (this.getSessionReviewsDeleted() != other.getSessionReviewsDeleted()) return false;
        if (this.getSessionCachesInvalidated() != other.getSessionCachesInvalidated()) return false;
        if (this.getLeakSnapshotsDeleted() != other.getLeakSnapshotsDeleted()) return false;
        if (this.getTradingDnaSnapshotsDeleted() != other.getTradingDnaSnapshotsDeleted()) return false;
        if (this.getDailyCoachSnapshotsDeleted() != other.getDailyCoachSnapshotsDeleted()) return false;
        if (this.getWeeklyCoachSnapshotsDeleted() != other.getWeeklyCoachSnapshotsDeleted()) return false;
        final java.lang.Object this$userId = this.getUserId();
        final java.lang.Object other$userId = other.getUserId();
        if (this$userId == null ? other$userId != null : !this$userId.equals(other$userId)) return false;
        final java.lang.Object this$accountId = this.getAccountId();
        final java.lang.Object other$accountId = other.getAccountId();
        if (this$accountId == null ? other$accountId != null : !this$accountId.equals(other$accountId)) return false;
        final java.lang.Object this$startDate = this.getStartDate();
        final java.lang.Object other$startDate = other.getStartDate();
        if (this$startDate == null ? other$startDate != null : !this$startDate.equals(other$startDate)) return false;
        final java.lang.Object this$endDate = this.getEndDate();
        final java.lang.Object other$endDate = other.getEndDate();
        if (this$endDate == null ? other$endDate != null : !this$endDate.equals(other$endDate)) return false;
        return true;
    }

    @java.lang.SuppressWarnings("all")
    protected boolean canEqual(final java.lang.Object other) {
        return other instanceof AdminTradeCleanupResponse;
    }

    @java.lang.Override
    @java.lang.SuppressWarnings("all")
    public int hashCode() {
        final int PRIME = 59;
        int result = 1;
        result = result * PRIME + (this.isDryRun() ? 79 : 97);
        final long $tradesMatched = this.getTradesMatched();
        result = result * PRIME + (int) ($tradesMatched >>> 32 ^ $tradesMatched);
        final long $tradesDeleted = this.getTradesDeleted();
        result = result * PRIME + (int) ($tradesDeleted >>> 32 ^ $tradesDeleted);
        final long $tradeReviewsDeleted = this.getTradeReviewsDeleted();
        result = result * PRIME + (int) ($tradeReviewsDeleted >>> 32 ^ $tradeReviewsDeleted);
        final long $tradeAnalysesDeleted = this.getTradeAnalysesDeleted();
        result = result * PRIME + (int) ($tradeAnalysesDeleted >>> 32 ^ $tradeAnalysesDeleted);
        final long $tradeRuleViolationsDeleted = this.getTradeRuleViolationsDeleted();
        result = result * PRIME + (int) ($tradeRuleViolationsDeleted >>> 32 ^ $tradeRuleViolationsDeleted);
        final long $dailyDisciplineScoresDeleted = this.getDailyDisciplineScoresDeleted();
        result = result * PRIME + (int) ($dailyDisciplineScoresDeleted >>> 32 ^ $dailyDisciplineScoresDeleted);
        final long $sessionReviewsDeleted = this.getSessionReviewsDeleted();
        result = result * PRIME + (int) ($sessionReviewsDeleted >>> 32 ^ $sessionReviewsDeleted);
        final long $sessionCachesInvalidated = this.getSessionCachesInvalidated();
        result = result * PRIME + (int) ($sessionCachesInvalidated >>> 32 ^ $sessionCachesInvalidated);
        final long $leakSnapshotsDeleted = this.getLeakSnapshotsDeleted();
        result = result * PRIME + (int) ($leakSnapshotsDeleted >>> 32 ^ $leakSnapshotsDeleted);
        final long $tradingDnaSnapshotsDeleted = this.getTradingDnaSnapshotsDeleted();
        result = result * PRIME + (int) ($tradingDnaSnapshotsDeleted >>> 32 ^ $tradingDnaSnapshotsDeleted);
        final long $dailyCoachSnapshotsDeleted = this.getDailyCoachSnapshotsDeleted();
        result = result * PRIME + (int) ($dailyCoachSnapshotsDeleted >>> 32 ^ $dailyCoachSnapshotsDeleted);
        final long $weeklyCoachSnapshotsDeleted = this.getWeeklyCoachSnapshotsDeleted();
        result = result * PRIME + (int) ($weeklyCoachSnapshotsDeleted >>> 32 ^ $weeklyCoachSnapshotsDeleted);
        final java.lang.Object $userId = this.getUserId();
        result = result * PRIME + ($userId == null ? 43 : $userId.hashCode());
        final java.lang.Object $accountId = this.getAccountId();
        result = result * PRIME + ($accountId == null ? 43 : $accountId.hashCode());
        final java.lang.Object $startDate = this.getStartDate();
        result = result * PRIME + ($startDate == null ? 43 : $startDate.hashCode());
        final java.lang.Object $endDate = this.getEndDate();
        result = result * PRIME + ($endDate == null ? 43 : $endDate.hashCode());
        return result;
    }

    @java.lang.Override
    @java.lang.SuppressWarnings("all")
    public java.lang.String toString() {
        return "AdminTradeCleanupResponse(userId=" + this.getUserId() + ", accountId=" + this.getAccountId() + ", startDate=" + this.getStartDate() + ", endDate=" + this.getEndDate() + ", dryRun=" + this.isDryRun() + ", tradesMatched=" + this.getTradesMatched() + ", tradesDeleted=" + this.getTradesDeleted() + ", tradeReviewsDeleted=" + this.getTradeReviewsDeleted() + ", tradeAnalysesDeleted=" + this.getTradeAnalysesDeleted() + ", tradeRuleViolationsDeleted=" + this.getTradeRuleViolationsDeleted() + ", dailyDisciplineScoresDeleted=" + this.getDailyDisciplineScoresDeleted() + ", sessionReviewsDeleted=" + this.getSessionReviewsDeleted() + ", sessionCachesInvalidated=" + this.getSessionCachesInvalidated() + ", leakSnapshotsDeleted=" + this.getLeakSnapshotsDeleted() + ", tradingDnaSnapshotsDeleted=" + this.getTradingDnaSnapshotsDeleted() + ", dailyCoachSnapshotsDeleted=" + this.getDailyCoachSnapshotsDeleted() + ", weeklyCoachSnapshotsDeleted=" + this.getWeeklyCoachSnapshotsDeleted() + ")";
    }

    @java.lang.SuppressWarnings("all")
    public AdminTradeCleanupResponse() {
    }
}
