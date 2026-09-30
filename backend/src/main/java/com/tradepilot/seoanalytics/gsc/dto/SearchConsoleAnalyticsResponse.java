package com.tradepilot.seoanalytics.gsc.dto;

import java.time.LocalDate;
import java.time.Instant;
import java.util.List;

public record SearchConsoleAnalyticsResponse(
        boolean enabled,
        boolean available,
        String status,
        String property,
        LocalDate startDate,
        LocalDate endDate,
        Instant lastRefreshedAt,
        String error,
        Summary summary,
        List<PageRow> pages,
        List<QueryPageRow> queries,
        List<TrendPoint> trend,
        List<OpportunityRow> opportunities,
        List<String> notes
) {
    public record Summary(
            long impressions,
            long clicks,
            double ctr,
            double averagePosition,
            long brandedImpressions,
            long brandedClicks,
            long nonBrandedImpressions,
            long nonBrandedClicks
    ) {}

    public record PageRow(
            String landingPage,
            long impressions,
            long clicks,
            double ctr,
            double averagePosition,
            long signups,
            long firstImports,
            long firstInsights,
            long proConversions,
            double signupPerGscClick,
            double activationPerGscClick,
            double proPerGscClick,
            double activatedPer100Clicks,
            double proPer100Clicks
    ) {}

    public record QueryPageRow(
            String query,
            String landingPage,
            long impressions,
            long clicks,
            double ctr,
            double averagePosition,
            String searchIntent,
            long landingPageSignups,
            long landingPageFirstInsights,
            long landingPageProConversions,
            double activatedPer100Clicks,
            double proPer100Clicks
    ) {}

    public record TrendPoint(LocalDate date, long impressions, long clicks, double ctr, double averagePosition) {}

    public record OpportunityRow(
            String category,
            String query,
            String landingPage,
            long impressions,
            long clicks,
            double ctr,
            double averagePosition,
            long landingPageFirstInsights,
            double activatedPer100Clicks,
            String reason
    ) {}
}
