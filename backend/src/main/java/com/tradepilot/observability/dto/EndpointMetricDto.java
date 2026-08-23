package com.tradepilot.observability.dto;

public class EndpointMetricDto {
    private String endpoint;
    private String category;
    private long requestCount;
    private double avgDurationMs;
    private long maxDurationMs;
    private String latestRequestId;

    public EndpointMetricDto() {
    }

    public EndpointMetricDto(String endpoint, String category, long requestCount, double avgDurationMs, long maxDurationMs, String latestRequestId) {
        this.endpoint = endpoint;
        this.category = category;
        this.requestCount = requestCount;
        this.avgDurationMs = avgDurationMs;
        this.maxDurationMs = maxDurationMs;
        this.latestRequestId = latestRequestId;
    }

    public String getEndpoint() { return endpoint; }
    public void setEndpoint(String endpoint) { this.endpoint = endpoint; }
    public String getCategory() { return category; }
    public void setCategory(String category) { this.category = category; }
    public long getRequestCount() { return requestCount; }
    public void setRequestCount(long requestCount) { this.requestCount = requestCount; }
    public double getAvgDurationMs() { return avgDurationMs; }
    public void setAvgDurationMs(double avgDurationMs) { this.avgDurationMs = avgDurationMs; }
    public long getMaxDurationMs() { return maxDurationMs; }
    public void setMaxDurationMs(long maxDurationMs) { this.maxDurationMs = maxDurationMs; }
    public String getLatestRequestId() { return latestRequestId; }
    public void setLatestRequestId(String latestRequestId) { this.latestRequestId = latestRequestId; }
}
