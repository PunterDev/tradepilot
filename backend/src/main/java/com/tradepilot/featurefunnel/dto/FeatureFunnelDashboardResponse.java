package com.tradepilot.featurefunnel.dto;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

public record FeatureFunnelDashboardResponse(
        LocalDate selectedStartDate,
        LocalDate selectedEndDate,
        LocalDateTime generatedAt,
        Map<String, String> activeFilters,
        List<FeatureFunnelSummaryResponse> funnelSummaries,
        FeatureFunnelDropoffResponse topDropoff,
        FeatureFunnelSummaryResponse highestCompletion,
        FeatureFunnelSummaryResponse fastestCompletion,
        FeatureFunnelSummaryResponse strongestRepeatUse,
        List<FeatureConversionAssociationResponse> conversionAssociations,
        List<FeatureRetentionAssociationResponse> retentionAssociations,
        List<FeatureFunnelCohortResponse> cohorts,
        List<FeatureFunnelInsightResponse> insights,
        List<String> dataQualityWarnings
) {}
