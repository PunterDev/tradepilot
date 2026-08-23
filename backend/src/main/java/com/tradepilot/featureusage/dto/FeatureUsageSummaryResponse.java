package com.tradepilot.featureusage.dto;

import com.tradepilot.featureusage.FeatureKey;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

public record FeatureUsageSummaryResponse(
        long totalUsers,
        long activeUsers30d,
        long activeUsers,
        long totalEvents,
        long uniqueFeaturesUsed,
        double avgEventsPerActiveUser,
        double avgUniqueFeaturesPerActiveUser,
        List<FeatureAdoptionResponse> features,
        FeatureKey mostUsedFeature,
        FeatureKey leastUsedFeature,
        FeatureKey highestVolumeFeature,
        FeatureKey highestAdoptionFeature,
        FeatureKey lowestAdoptionFeature,
        List<FeatureUsageInsightResponse> insights,
        List<FeatureUsageDataQualityIssueResponse> dataQuality,
        List<FeatureUsageMetadataResponse> metadata,
        LocalDate selectedStartDate,
        LocalDate selectedEndDate,
        LocalDateTime generatedAt
) {
}
