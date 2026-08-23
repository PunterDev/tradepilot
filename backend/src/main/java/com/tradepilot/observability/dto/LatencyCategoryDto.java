package com.tradepilot.observability.dto;

public class LatencyCategoryDto {
    private String category;
    private long requestCount;
    private double avgDurationMs;
    private long p95DurationMs;

    public LatencyCategoryDto() {
    }

    public LatencyCategoryDto(String category, long requestCount, double avgDurationMs, long p95DurationMs) {
        this.category = category;
        this.requestCount = requestCount;
        this.avgDurationMs = avgDurationMs;
        this.p95DurationMs = p95DurationMs;
    }

    public String getCategory() { return category; }
    public void setCategory(String category) { this.category = category; }
    public long getRequestCount() { return requestCount; }
    public void setRequestCount(long requestCount) { this.requestCount = requestCount; }
    public double getAvgDurationMs() { return avgDurationMs; }
    public void setAvgDurationMs(double avgDurationMs) { this.avgDurationMs = avgDurationMs; }
    public long getP95DurationMs() { return p95DurationMs; }
    public void setP95DurationMs(long p95DurationMs) { this.p95DurationMs = p95DurationMs; }
}
