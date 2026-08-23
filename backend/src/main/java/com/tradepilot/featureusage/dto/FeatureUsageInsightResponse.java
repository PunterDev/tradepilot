package com.tradepilot.featureusage.dto;

import com.tradepilot.featureusage.FeatureKey;

public record FeatureUsageInsightResponse(
        String type,
        FeatureKey feature,
        String title,
        String evidence,
        String whyItMatters,
        String suggestedInvestigation,
        String confidence,
        String priority
) {
}
