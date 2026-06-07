package com.tradepilot.dto.admin;

import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;
import java.util.List;

@Data
@Builder
public class AdminBillingSummaryResponse {
    private long totalUsers;
    private long freeUsers;
    private long proUsers;
    private long stripePayingSubscribers;
    private long manualProUsers;
    private double conversionRate;
    private double paidConversionRate;
    private BigDecimal estimatedMrr;
    private BigDecimal estimatedArr;
    private BigDecimal potentialMrr;
    private BigDecimal potentialArr;
    private long manualUpgrades;
    private long stripeSubscriptions;
    private boolean stripeCheckoutConfigured;
    private boolean stripeWebhookConfigured;
    private boolean monthlyPriceIdConfigured;
    private boolean yearlyPriceIdConfigured;
    private List<AdminRecentUpgradeResponse> recentPlanChanges;
}
