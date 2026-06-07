package com.tradepilot.featureusage.dto;

import com.tradepilot.featureusage.FeatureKey;
import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class FeatureAdoptionResponse {
    private FeatureKey feature;
    private long uniqueUsers;
    private long totalUsage;
    private double overallAdoptionRate;
    private double activeUserAdoptionRate;
}
