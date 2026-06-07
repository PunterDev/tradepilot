package com.tradepilot.featureusage.dto;

import com.tradepilot.billing.PlanType;
import com.tradepilot.featureusage.FeatureKey;
import lombok.Builder;
import lombok.Data;

import java.util.Map;

@Data
@Builder
public class FeatureUsageUserResponse {
    private Long userId;
    private String email;
    private PlanType plan;
    private long trades;
    private Map<FeatureKey, Long> featureUsageCounts;
}
