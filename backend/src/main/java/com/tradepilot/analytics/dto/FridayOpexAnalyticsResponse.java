package com.tradepilot.analytics.dto;

public class FridayOpexAnalyticsResponse {
    private AnalyticsMetricDto allFridays;
    private AnalyticsMetricDto opexFridays;
    private AnalyticsMetricDto nonOpexFridays;
    private String insight;

    public FridayOpexAnalyticsResponse() {
    }

    public FridayOpexAnalyticsResponse(AnalyticsMetricDto allFridays, AnalyticsMetricDto opexFridays, AnalyticsMetricDto nonOpexFridays, String insight) {
        this.allFridays = allFridays;
        this.opexFridays = opexFridays;
        this.nonOpexFridays = nonOpexFridays;
        this.insight = insight;
    }

    public static FridayOpexAnalyticsResponse of(AnalyticsMetricDto allFridays, AnalyticsMetricDto opexFridays, AnalyticsMetricDto nonOpexFridays, String insight) {
        return new FridayOpexAnalyticsResponse(allFridays, opexFridays, nonOpexFridays, insight);
    }

    public AnalyticsMetricDto getAllFridays() { return allFridays; }
    public void setAllFridays(AnalyticsMetricDto allFridays) { this.allFridays = allFridays; }
    public AnalyticsMetricDto getOpexFridays() { return opexFridays; }
    public void setOpexFridays(AnalyticsMetricDto opexFridays) { this.opexFridays = opexFridays; }
    public AnalyticsMetricDto getNonOpexFridays() { return nonOpexFridays; }
    public void setNonOpexFridays(AnalyticsMetricDto nonOpexFridays) { this.nonOpexFridays = nonOpexFridays; }
    public String getInsight() { return insight; }
    public void setInsight(String insight) { this.insight = insight; }
}
