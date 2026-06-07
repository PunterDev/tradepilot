package com.tradepilot.featureusage.dto;

import com.tradepilot.featureusage.FeatureKey;
import lombok.Builder;
import lombok.Data;

import java.util.List;

@Data
@Builder
public class FeatureUsageSummaryResponse {
    private long totalUsers;
    private long activeUsers30d;
    private List<FeatureAdoptionResponse> features;
    private FeatureKey mostUsedFeature;
    private FeatureKey leastUsedFeature;
}
