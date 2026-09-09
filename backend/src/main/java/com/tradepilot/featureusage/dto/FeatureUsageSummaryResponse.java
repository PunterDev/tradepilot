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
        long activeActions,
        long passiveViews,
        long aiGenerations,
        long uniqueFeaturesUsed,
        double avgEventsPerActiveUser,
        double avgUniqueFeaturesPerActiveUser,
        List<FeatureAdoptionResponse> features,
        FeatureKey mostUsedFeature,
        FeatureKey leastUsedFeature,
        FeatureKey highestVolumeFeature,
        FeatureKey highestAdoptionFeature,
        FeatureKey highestActiveAdoptionFeature,
        FeatureKey lowestAdoptionFeature,
        FeatureUsageComparisonResponse activeUsersComparison,
        FeatureUsageComparisonResponse activeActionsComparison,
        FeatureUsageComparisonResponse passiveViewsComparison,
        FeatureUsageComparisonResponse aiGenerationsComparison,
        FeatureUsageComparisonResponse featuresAdoptedComparison,
        List<FeatureUsageInsightResponse> insights,
        ActivationFunnelMetricsResponse activationFunnel,
        List<FeatureUsageDataQualityIssueResponse> dataQuality,
        List<FeatureUsageMetadataResponse> metadata,
        LocalDate selectedStartDate,
        LocalDate selectedEndDate,
        LocalDate previousStartDate,
        LocalDate previousEndDate,
        LocalDateTime generatedAt
) {
}
