package com.tradepilot.billing;

import lombok.Data;

import java.util.Map;

@Data
public class BillingUsageResponse {
    private PlanType plan;
    private SubscriptionStatus subscriptionStatus;
    private Map<String, Integer> limits;
    private Map<String, Integer> usageThisMonth;
    private Map<String, Integer> remaining;
    private String resetDate;
    private Integer monthlyAiCredits;
    private Integer monthlyAiCreditsUsed;
    private Integer monthlyAiCreditsRemaining;
    private Integer purchasedAiCreditsBalance;
    private Integer totalAiCreditsAvailable;
}
