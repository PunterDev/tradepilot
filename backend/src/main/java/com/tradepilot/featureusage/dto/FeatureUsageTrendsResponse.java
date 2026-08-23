package com.tradepilot.featureusage.dto;

import com.tradepilot.featureusage.FeatureKey;
import java.util.List;
import java.util.Map;

public record FeatureUsageTrendsResponse(
        List<FeatureTrendPointResponse> dailyCoach,
        List<FeatureTrendPointResponse> weeklyCoach,
        List<FeatureTrendPointResponse> tradingDna,
        List<FeatureTrendPointResponse> aiReview,
        List<FeatureTrendPointResponse> sessionReview,
        Map<FeatureKey, List<FeatureTrendPointResponse>> series,
        FeatureKey fastestGrowingFeature,
        FeatureKey largestDeclineFeature,
        FeatureKey mostStableFeature
) {
}
