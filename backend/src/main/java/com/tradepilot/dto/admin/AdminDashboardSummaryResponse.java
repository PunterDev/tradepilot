package com.tradepilot.dto.admin;

import java.util.List;

public class AdminDashboardSummaryResponse {
    private long totalUsers;
    private long freeUsers;
    private long proUsers;
    private long adminUsers;
    private long verifiedUsers;
    private long unverifiedUsers;
    private long totalContactMessages;
    private long newContactMessages;
    private long openContactMessages;
    private long repliedContactMessages;
    private long failedEmailMessages;
    private long totalTradingAccounts;
    private long totalTrades;
    private long totalAiReviews;
    private long dailyCoachUsage;
    private long weeklyCoachUsage;
    private long tradingDnaUsage;
    private List<AdminUserResponse> recentUsers;
    private List<AdminContactMessageResponse> recentContactMessages;
    private List<AdminRecentUpgradeResponse> recentUpgrades;

    @java.lang.SuppressWarnings("all")
    public AdminDashboardSummaryResponse() {
    }

    @java.lang.SuppressWarnings("all")
    public long getTotalUsers() {
        return this.totalUsers;
    }

    @java.lang.SuppressWarnings("all")
    public long getFreeUsers() {
        return this.freeUsers;
    }

    @java.lang.SuppressWarnings("all")
    public long getProUsers() {
        return this.proUsers;
    }

    @java.lang.SuppressWarnings("all")
    public long getAdminUsers() {
        return this.adminUsers;
    }

    @java.lang.SuppressWarnings("all")
    public long getVerifiedUsers() {
        return this.verifiedUsers;
    }

    @java.lang.SuppressWarnings("all")
    public long getUnverifiedUsers() {
        return this.unverifiedUsers;
    }

    @java.lang.SuppressWarnings("all")
    public long getTotalContactMessages() {
        return this.totalContactMessages;
    }

    @java.lang.SuppressWarnings("all")
    public long getNewContactMessages() {
        return this.newContactMessages;
    }

    @java.lang.SuppressWarnings("all")
    public long getOpenContactMessages() {
        return this.openContactMessages;
    }

    @java.lang.SuppressWarnings("all")
    public long getRepliedContactMessages() {
        return this.repliedContactMessages;
    }

    @java.lang.SuppressWarnings("all")
    public long getFailedEmailMessages() {
        return this.failedEmailMessages;
    }

    @java.lang.SuppressWarnings("all")
    public long getTotalTradingAccounts() {
        return this.totalTradingAccounts;
    }

    @java.lang.SuppressWarnings("all")
    public long getTotalTrades() {
        return this.totalTrades;
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
    public List<AdminUserResponse> getRecentUsers() {
        return this.recentUsers;
    }

    @java.lang.SuppressWarnings("all")
    public List<AdminContactMessageResponse> getRecentContactMessages() {
        return this.recentContactMessages;
    }

    @java.lang.SuppressWarnings("all")
    public List<AdminRecentUpgradeResponse> getRecentUpgrades() {
        return this.recentUpgrades;
    }

    @java.lang.SuppressWarnings("all")
    public void setTotalUsers(final long totalUsers) {
        this.totalUsers = totalUsers;
    }

    @java.lang.SuppressWarnings("all")
    public void setFreeUsers(final long freeUsers) {
        this.freeUsers = freeUsers;
    }

    @java.lang.SuppressWarnings("all")
    public void setProUsers(final long proUsers) {
        this.proUsers = proUsers;
    }

    @java.lang.SuppressWarnings("all")
    public void setAdminUsers(final long adminUsers) {
        this.adminUsers = adminUsers;
    }

    @java.lang.SuppressWarnings("all")
    public void setVerifiedUsers(final long verifiedUsers) {
        this.verifiedUsers = verifiedUsers;
    }

    @java.lang.SuppressWarnings("all")
    public void setUnverifiedUsers(final long unverifiedUsers) {
        this.unverifiedUsers = unverifiedUsers;
    }

    @java.lang.SuppressWarnings("all")
    public void setTotalContactMessages(final long totalContactMessages) {
        this.totalContactMessages = totalContactMessages;
    }

    @java.lang.SuppressWarnings("all")
    public void setNewContactMessages(final long newContactMessages) {
        this.newContactMessages = newContactMessages;
    }

    @java.lang.SuppressWarnings("all")
    public void setOpenContactMessages(final long openContactMessages) {
        this.openContactMessages = openContactMessages;
    }

    @java.lang.SuppressWarnings("all")
    public void setRepliedContactMessages(final long repliedContactMessages) {
        this.repliedContactMessages = repliedContactMessages;
    }

    @java.lang.SuppressWarnings("all")
    public void setFailedEmailMessages(final long failedEmailMessages) {
        this.failedEmailMessages = failedEmailMessages;
    }

    @java.lang.SuppressWarnings("all")
    public void setTotalTradingAccounts(final long totalTradingAccounts) {
        this.totalTradingAccounts = totalTradingAccounts;
    }

    @java.lang.SuppressWarnings("all")
    public void setTotalTrades(final long totalTrades) {
        this.totalTrades = totalTrades;
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
    public void setRecentUsers(final List<AdminUserResponse> recentUsers) {
        this.recentUsers = recentUsers;
    }

    @java.lang.SuppressWarnings("all")
    public void setRecentContactMessages(final List<AdminContactMessageResponse> recentContactMessages) {
        this.recentContactMessages = recentContactMessages;
    }

    @java.lang.SuppressWarnings("all")
    public void setRecentUpgrades(final List<AdminRecentUpgradeResponse> recentUpgrades) {
        this.recentUpgrades = recentUpgrades;
    }

    @java.lang.Override
    @java.lang.SuppressWarnings("all")
    public boolean equals(final java.lang.Object o) {
        if (o == this) return true;
        if (!(o instanceof AdminDashboardSummaryResponse)) return false;
        final AdminDashboardSummaryResponse other = (AdminDashboardSummaryResponse) o;
        if (!other.canEqual((java.lang.Object) this)) return false;
        if (this.getTotalUsers() != other.getTotalUsers()) return false;
        if (this.getFreeUsers() != other.getFreeUsers()) return false;
        if (this.getProUsers() != other.getProUsers()) return false;
        if (this.getAdminUsers() != other.getAdminUsers()) return false;
        if (this.getVerifiedUsers() != other.getVerifiedUsers()) return false;
        if (this.getUnverifiedUsers() != other.getUnverifiedUsers()) return false;
        if (this.getTotalContactMessages() != other.getTotalContactMessages()) return false;
        if (this.getNewContactMessages() != other.getNewContactMessages()) return false;
        if (this.getOpenContactMessages() != other.getOpenContactMessages()) return false;
        if (this.getRepliedContactMessages() != other.getRepliedContactMessages()) return false;
        if (this.getFailedEmailMessages() != other.getFailedEmailMessages()) return false;
        if (this.getTotalTradingAccounts() != other.getTotalTradingAccounts()) return false;
        if (this.getTotalTrades() != other.getTotalTrades()) return false;
        if (this.getTotalAiReviews() != other.getTotalAiReviews()) return false;
        if (this.getDailyCoachUsage() != other.getDailyCoachUsage()) return false;
        if (this.getWeeklyCoachUsage() != other.getWeeklyCoachUsage()) return false;
        if (this.getTradingDnaUsage() != other.getTradingDnaUsage()) return false;
        final java.lang.Object this$recentUsers = this.getRecentUsers();
        final java.lang.Object other$recentUsers = other.getRecentUsers();
        if (this$recentUsers == null ? other$recentUsers != null : !this$recentUsers.equals(other$recentUsers)) return false;
        final java.lang.Object this$recentContactMessages = this.getRecentContactMessages();
        final java.lang.Object other$recentContactMessages = other.getRecentContactMessages();
        if (this$recentContactMessages == null ? other$recentContactMessages != null : !this$recentContactMessages.equals(other$recentContactMessages)) return false;
        final java.lang.Object this$recentUpgrades = this.getRecentUpgrades();
        final java.lang.Object other$recentUpgrades = other.getRecentUpgrades();
        if (this$recentUpgrades == null ? other$recentUpgrades != null : !this$recentUpgrades.equals(other$recentUpgrades)) return false;
        return true;
    }

    @java.lang.SuppressWarnings("all")
    protected boolean canEqual(final java.lang.Object other) {
        return other instanceof AdminDashboardSummaryResponse;
    }

    @java.lang.Override
    @java.lang.SuppressWarnings("all")
    public int hashCode() {
        final int PRIME = 59;
        int result = 1;
        final long $totalUsers = this.getTotalUsers();
        result = result * PRIME + (int) ($totalUsers >>> 32 ^ $totalUsers);
        final long $freeUsers = this.getFreeUsers();
        result = result * PRIME + (int) ($freeUsers >>> 32 ^ $freeUsers);
        final long $proUsers = this.getProUsers();
        result = result * PRIME + (int) ($proUsers >>> 32 ^ $proUsers);
        final long $adminUsers = this.getAdminUsers();
        result = result * PRIME + (int) ($adminUsers >>> 32 ^ $adminUsers);
        final long $verifiedUsers = this.getVerifiedUsers();
        result = result * PRIME + (int) ($verifiedUsers >>> 32 ^ $verifiedUsers);
        final long $unverifiedUsers = this.getUnverifiedUsers();
        result = result * PRIME + (int) ($unverifiedUsers >>> 32 ^ $unverifiedUsers);
        final long $totalContactMessages = this.getTotalContactMessages();
        result = result * PRIME + (int) ($totalContactMessages >>> 32 ^ $totalContactMessages);
        final long $newContactMessages = this.getNewContactMessages();
        result = result * PRIME + (int) ($newContactMessages >>> 32 ^ $newContactMessages);
        final long $openContactMessages = this.getOpenContactMessages();
        result = result * PRIME + (int) ($openContactMessages >>> 32 ^ $openContactMessages);
        final long $repliedContactMessages = this.getRepliedContactMessages();
        result = result * PRIME + (int) ($repliedContactMessages >>> 32 ^ $repliedContactMessages);
        final long $failedEmailMessages = this.getFailedEmailMessages();
        result = result * PRIME + (int) ($failedEmailMessages >>> 32 ^ $failedEmailMessages);
        final long $totalTradingAccounts = this.getTotalTradingAccounts();
        result = result * PRIME + (int) ($totalTradingAccounts >>> 32 ^ $totalTradingAccounts);
        final long $totalTrades = this.getTotalTrades();
        result = result * PRIME + (int) ($totalTrades >>> 32 ^ $totalTrades);
        final long $totalAiReviews = this.getTotalAiReviews();
        result = result * PRIME + (int) ($totalAiReviews >>> 32 ^ $totalAiReviews);
        final long $dailyCoachUsage = this.getDailyCoachUsage();
        result = result * PRIME + (int) ($dailyCoachUsage >>> 32 ^ $dailyCoachUsage);
        final long $weeklyCoachUsage = this.getWeeklyCoachUsage();
        result = result * PRIME + (int) ($weeklyCoachUsage >>> 32 ^ $weeklyCoachUsage);
        final long $tradingDnaUsage = this.getTradingDnaUsage();
        result = result * PRIME + (int) ($tradingDnaUsage >>> 32 ^ $tradingDnaUsage);
        final java.lang.Object $recentUsers = this.getRecentUsers();
        result = result * PRIME + ($recentUsers == null ? 43 : $recentUsers.hashCode());
        final java.lang.Object $recentContactMessages = this.getRecentContactMessages();
        result = result * PRIME + ($recentContactMessages == null ? 43 : $recentContactMessages.hashCode());
        final java.lang.Object $recentUpgrades = this.getRecentUpgrades();
        result = result * PRIME + ($recentUpgrades == null ? 43 : $recentUpgrades.hashCode());
        return result;
    }

    @java.lang.Override
    @java.lang.SuppressWarnings("all")
    public java.lang.String toString() {
        return "AdminDashboardSummaryResponse(totalUsers=" + this.getTotalUsers() + ", freeUsers=" + this.getFreeUsers() + ", proUsers=" + this.getProUsers() + ", adminUsers=" + this.getAdminUsers() + ", verifiedUsers=" + this.getVerifiedUsers() + ", unverifiedUsers=" + this.getUnverifiedUsers() + ", totalContactMessages=" + this.getTotalContactMessages() + ", newContactMessages=" + this.getNewContactMessages() + ", openContactMessages=" + this.getOpenContactMessages() + ", repliedContactMessages=" + this.getRepliedContactMessages() + ", failedEmailMessages=" + this.getFailedEmailMessages() + ", totalTradingAccounts=" + this.getTotalTradingAccounts() + ", totalTrades=" + this.getTotalTrades() + ", totalAiReviews=" + this.getTotalAiReviews() + ", dailyCoachUsage=" + this.getDailyCoachUsage() + ", weeklyCoachUsage=" + this.getWeeklyCoachUsage() + ", tradingDnaUsage=" + this.getTradingDnaUsage() + ", recentUsers=" + this.getRecentUsers() + ", recentContactMessages=" + this.getRecentContactMessages() + ", recentUpgrades=" + this.getRecentUpgrades() + ")";
    }
}
