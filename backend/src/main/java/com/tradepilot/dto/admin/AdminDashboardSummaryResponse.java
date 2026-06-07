package com.tradepilot.dto.admin;

import lombok.Data;

import java.util.List;

@Data
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
}
