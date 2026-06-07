package com.tradepilot.dto.admin;

import com.tradepilot.billing.PlanType;
import com.tradepilot.billing.SubscriptionStatus;
import com.tradepilot.enums.UserRole;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.Map;

@Data
public class AdminUserResponse {
    private Long id;
    private String email;
    private String name;
    private PlanType plan;
    private UserRole role;
    private LocalDateTime createdAt;
    private Boolean emailVerified;
    private String stripeCustomerId;
    private String stripeSubscriptionId;
    private LocalDateTime proUntil;
    private SubscriptionStatus subscriptionStatus;
    private long tradingAccountCount;
    private long tradeCount;
    private long aiReviewCount;
    private int dailyCoachUsage;
    private int weeklyCoachUsage;
    private int tradingDnaUsage;
    private String planSource;
    private Map<String, Integer> usageThisMonth;
    private Map<String, Integer> limits;
    private Map<String, Integer> remaining;
}
