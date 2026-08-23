package com.tradepilot.observability.dto;

import java.time.LocalDateTime;

public class ErrorEndpointBreakdownDto {
    private String endpoint;
    private String category;
    private long count;
    private String latestRequestId;
    private LocalDateTime latestTimestamp;
    private String latestErrorCode;

    public ErrorEndpointBreakdownDto() {
    }

    public ErrorEndpointBreakdownDto(String endpoint, String category, long count, String latestRequestId, LocalDateTime latestTimestamp, String latestErrorCode) {
        this.endpoint = endpoint;
        this.category = category;
        this.count = count;
        this.latestRequestId = latestRequestId;
        this.latestTimestamp = latestTimestamp;
        this.latestErrorCode = latestErrorCode;
    }

    public String getEndpoint() { return endpoint; }
    public void setEndpoint(String endpoint) { this.endpoint = endpoint; }
    public String getCategory() { return category; }
    public void setCategory(String category) { this.category = category; }
    public long getCount() { return count; }
    public void setCount(long count) { this.count = count; }
    public String getLatestRequestId() { return latestRequestId; }
    public void setLatestRequestId(String latestRequestId) { this.latestRequestId = latestRequestId; }
    public LocalDateTime getLatestTimestamp() { return latestTimestamp; }
    public void setLatestTimestamp(LocalDateTime latestTimestamp) { this.latestTimestamp = latestTimestamp; }
    public String getLatestErrorCode() { return latestErrorCode; }
    public void setLatestErrorCode(String latestErrorCode) { this.latestErrorCode = latestErrorCode; }
}
