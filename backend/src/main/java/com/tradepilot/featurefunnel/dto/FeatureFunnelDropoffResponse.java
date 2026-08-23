package com.tradepilot.featurefunnel.dto;

import com.tradepilot.featurefunnel.FeatureFunnelConfidence;

public record FeatureFunnelDropoffResponse(
        String funnelKey,
        String fromStepKey,
        String toStepKey,
        String transition,
        long previousStepUsers,
        long nextStepUsers,
        long dropoffUsers,
        double dropoffRate,
        Double medianDelayHours,
        String suggestedInvestigation,
        FeatureFunnelConfidence confidence
) {}
