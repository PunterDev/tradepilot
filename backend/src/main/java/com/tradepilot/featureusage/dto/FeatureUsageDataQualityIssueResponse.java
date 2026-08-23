package com.tradepilot.featureusage.dto;

import java.time.LocalDateTime;

public record FeatureUsageDataQualityIssueResponse(
        String issue,
        long eventCount,
        LocalDateTime earliestOccurrence,
        LocalDateTime latestOccurrence,
        String suggestedFix
) {
}
