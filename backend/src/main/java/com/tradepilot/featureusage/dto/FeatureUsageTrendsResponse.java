package com.tradepilot.featureusage.dto;

import lombok.Builder;
import lombok.Data;

import java.util.List;

@Data
@Builder
public class FeatureUsageTrendsResponse {
    private List<FeatureTrendPointResponse> dailyCoach;
    private List<FeatureTrendPointResponse> weeklyCoach;
    private List<FeatureTrendPointResponse> tradingDna;
    private List<FeatureTrendPointResponse> aiReview;
    private List<FeatureTrendPointResponse> sessionReview;
}
