package com.tradepilot.featurefunnel.dto;

import com.tradepilot.featurefunnel.FeatureFunnelConfidence;

public record FeatureConversionAssociationResponse(
        String funnelKey,
        String behavior,
        long exposedUsers,
        long exposedConvertedUsers,
        double exposedConversionRate,
        long comparisonUsers,
        double comparisonConversionRate,
        Double absoluteDifferencePoints,
        Double relativeLift,
        long sampleSize,
        FeatureFunnelConfidence confidence,
        String caveat
) {}
