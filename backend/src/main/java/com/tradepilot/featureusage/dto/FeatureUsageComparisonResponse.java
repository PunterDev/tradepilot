package com.tradepilot.featureusage.dto;

public record FeatureUsageComparisonResponse(
        long currentValue,
        long previousValue,
        double changePercent,
        double percentagePointDelta,
        String direction
) {
}
