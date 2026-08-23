package com.tradepilot.featureusage.dto;

import com.tradepilot.featureusage.FeatureKey;
import com.tradepilot.featureusage.FeatureUsageCategory;
import com.tradepilot.featureusage.FeatureUsageEventType;

public record FeatureUsageMetadataResponse(
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
