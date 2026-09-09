package com.tradepilot.seoanalytics.dto;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

public record SeoAnalyticsResponse(
        LocalDate startDate,
        LocalDate endDate,
        LocalDateTime generatedAt,
        List<Kpi> kpis,
        List<FunnelStep> funnel,
        List<LandingPageRow> landingPages,
        List<ToolRow> tools,
        List<SourceRow> sources,
        List<CampaignRow> campaigns,
        List<TrendPoint> activationTrend,
        LatencyMetrics latency,
        List<String> notes
) {
    public record Kpi(String key, String label, long value, double rate, String helper) {}
    public record FunnelStep(String key, String label, long count, double conversionFromPrevious, double conversionFromInitial) {}
    public record LandingPageRow(String landingPage, long views, long ctaClicks, long signups, long firstImports, long firstInsights, long proConversions, double signupRate, double activationRate, double paidConversionRate) {}
    public record ToolRow(String tool, long views, long started, long completed, long ctaClicks, long signups, long firstImports, long firstInsights, long proConversions, double completionRate, double signupRate, double activationRate, double paidConversionRate) {}
    public record SourceRow(String source, String medium, long views, long ctaClicks, long signups, long firstInsights, long proConversions, double signupRate, double activationRate, double paidConversionRate) {}
    public record CampaignRow(String campaign, long views, long signups, long firstInsights, long proConversions, double signupRate, double activationRate, double paidConversionRate) {}
    public record TrendPoint(LocalDate date, long signups, long firstInsights) {}
    public record LatencyMetrics(Double medianSignupToFirstImportMinutes, Double medianSignupToFirstInsightMinutes) {}
}
