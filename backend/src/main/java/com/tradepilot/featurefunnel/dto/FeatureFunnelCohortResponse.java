package com.tradepilot.featurefunnel.dto;

public record FeatureFunnelCohortResponse(
        String cohortKey,
        String cohortLabel,
        long entrants,
        long completed,
        double completionRate,
        double return7dRate,
        double return14dRate,
        double return30dRate
) {}
