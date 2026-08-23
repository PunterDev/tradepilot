package com.tradepilot.featurefunnel.dto;

import com.tradepilot.featurefunnel.FeatureFunnelConfidence;

public record FeatureFunnelInsightResponse(
        String type,
        String title,
        String funnelKey,
        String transition,
        String evidence,
        String whyItMatters,
        String suggestedInvestigation,
        String priority,
        FeatureFunnelConfidence confidence,
        String caveat
) {}
