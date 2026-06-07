package com.tradepilot.dto.admin;

import lombok.Data;

@Data
public class AdminUsageSummaryResponse {
    private long totalAiReviews;
    private long dailyCoachUsage;
    private long weeklyCoachUsage;
    private long tradingDnaUsage;
    private long totalMonthlyAiUsage;
}
