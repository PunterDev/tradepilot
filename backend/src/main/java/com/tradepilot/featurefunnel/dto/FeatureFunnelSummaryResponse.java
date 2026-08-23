package com.tradepilot.featurefunnel.dto;

import com.tradepilot.featurefunnel.FeatureFunnelConfidence;
import com.tradepilot.featurefunnel.FeatureFunnelKey;
import com.tradepilot.featurefunnel.FeatureFunnelStatus;
import com.tradepilot.featureusage.FeatureUsageCategory;

import java.util.List;

public record FeatureFunnelSummaryResponse(
        FeatureFunnelKey funnelKey,
        String displayName,
        FeatureUsageCategory category,
        String description,
        long eligibleUsers,
        long enteredUsers,
        long completedUsers,
        double completionRate,
        double eligibleCompletionRate,
        Double previousCompletionRate,
        Double completionRateChange,
        double repeatRate7d,
        double repeatRate14d,
        Double medianCompletionTimeHours,
        Double averageCompletionTimeHours,
        FeatureFunnelDropoffResponse largestDropoffStep,
        FeatureFunnelConfidence confidence,
        FeatureFunnelStatus status,
        List<FeatureFunnelStepResponse> steps
) {}
