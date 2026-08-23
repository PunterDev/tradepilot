package com.tradepilot.featureusage.dto;

import com.tradepilot.billing.PlanType;
import com.tradepilot.featureusage.FeatureKey;
import java.time.LocalDateTime;
import java.util.Map;

public record FeatureUsageUserResponse(
        Long userId,
        String email,
        String maskedEmail,
        PlanType plan,
        long trades,
        long activeFeatures,
        long activeActions,
        long passiveEvents,
        long aiGenerations,
        long totalEvents,
        LocalDateTime lastActiveAt,
        String engagementLevel,
        Map<FeatureKey, Long> featureUsageCounts
) {
    public Long getUserId() { return userId; }
}
