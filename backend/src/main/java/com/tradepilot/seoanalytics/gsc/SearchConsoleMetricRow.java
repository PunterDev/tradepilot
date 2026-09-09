package com.tradepilot.seoanalytics.gsc;

public record SearchConsoleMetricRow(
        String query,
        String page,
        String date,
        long clicks,
        long impressions,
        double ctr,
        double position
) {
}
