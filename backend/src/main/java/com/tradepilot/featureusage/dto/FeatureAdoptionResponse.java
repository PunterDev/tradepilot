package com.tradepilot.featureusage.dto;

import com.tradepilot.featureusage.FeatureKey;
import com.tradepilot.featureusage.FeatureUsageCategory;
import com.tradepilot.featureusage.FeatureUsageEventType;

public record FeatureAdoptionResponse(
        FeatureKey feature,
        String displayName,
        String shortName,
        String description,
        FeatureUsageCategory category,
        FeatureUsageEventType eventType,
        boolean coreFeature,
        boolean aiFeature,
        boolean paidAction,
        boolean passiveEvent,
        boolean knownFeature,
        long uniqueUsers,
        long totalUsage,
        long activeEventCount,
        long passiveEventCount,
        long aiGenerationCount,
        double overallAdoptionRate,
        double activeUserAdoptionRate,
        double eventsPerUser,
        double repeatUsageRate,
        long previousPeriodUsers,
        long previousPeriodEvents,
        double userGrowthPercent,
        double eventGrowthPercent,
        double engagementScore,
        double opportunityScore,
        String status
) {
    public long getUniqueUsers() { return uniqueUsers; }
    public long getTotalUsage() { return totalUsage; }
    public FeatureKey getFeature() { return feature; }
}
