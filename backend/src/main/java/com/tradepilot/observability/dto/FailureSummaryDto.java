package com.tradepilot.observability.dto;

import java.time.LocalDateTime;

public class FailureSummaryDto {
    private String source;
    private LocalDateTime timestamp;
    private String requestId;
    private Long userId;
    private String endpoint;
    private String feature;
    private String errorCode;
    private String userMessage;

    public FailureSummaryDto() {
    }

    public FailureSummaryDto(String source, LocalDateTime timestamp, String requestId, Long userId, String endpoint, String feature, String errorCode, String userMessage) {
        this.source = source;
        this.timestamp = timestamp;
        this.requestId = requestId;
        this.userId = userId;
        this.endpoint = endpoint;
        this.feature = feature;
        this.errorCode = errorCode;
        this.userMessage = userMessage;
    }

    public String getSource() { return source; }
    public void setSource(String source) { this.source = source; }
    public LocalDateTime getTimestamp() { return timestamp; }
    public void setTimestamp(LocalDateTime timestamp) { this.timestamp = timestamp; }
    public String getRequestId() { return requestId; }
    public void setRequestId(String requestId) { this.requestId = requestId; }
    public Long getUserId() { return userId; }
    public void setUserId(Long userId) { this.userId = userId; }
    public String getEndpoint() { return endpoint; }
    public void setEndpoint(String endpoint) { this.endpoint = endpoint; }
    public String getFeature() { return feature; }
    public void setFeature(String feature) { this.feature = feature; }
    public String getErrorCode() { return errorCode; }
    public void setErrorCode(String errorCode) { this.errorCode = errorCode; }
    public String getUserMessage() { return userMessage; }
    public void setUserMessage(String userMessage) { this.userMessage = userMessage; }
}
