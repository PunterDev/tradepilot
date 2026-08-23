package com.tradepilot.featureusage;

public record FeatureUsageMetadata(
        FeatureKey key,
        String displayName,
        String shortName,
        String description,
        FeatureUsageCategory category,
        FeatureUsageEventType eventType,
        boolean coreFeature,
        boolean aiFeature,
        boolean paidAction,
        boolean passiveEvent,
        int sortOrder,
        String iconKey,
        String colorKey
) {
}
