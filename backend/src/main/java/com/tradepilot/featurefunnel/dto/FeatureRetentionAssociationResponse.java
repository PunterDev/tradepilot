package com.tradepilot.featurefunnel.dto;

import com.tradepilot.featurefunnel.FeatureFunnelConfidence;

public record FeatureRetentionAssociationResponse(
        String funnelKey,
        String cohort,
        long cohortSize,
        long retained7dUsers,
        double retention7dRate,
        long retained14dUsers,
        double retention14dRate,
        long retained30dUsers,
        double retention30dRate,
        Double absoluteDifferencePoints,
        FeatureFunnelConfidence confidence,
        String caveat
) {}
