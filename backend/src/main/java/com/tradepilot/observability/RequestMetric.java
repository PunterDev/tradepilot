package com.tradepilot.observability;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.Table;

import java.time.LocalDateTime;

@Entity
@Table(name = "api_request_metrics", indexes = {
        @Index(name = "idx_api_request_metrics_created_at", columnList = "created_at"),
        @Index(name = "idx_api_request_metrics_created_duration", columnList = "created_at, duration_ms"),
        @Index(name = "idx_api_request_metrics_endpoint_created_at", columnList = "endpoint, created_at"),
        @Index(name = "idx_api_request_metrics_status_created_at", columnList = "status, created_at"),
        @Index(name = "idx_api_request_metrics_user_id_created_at", columnList = "user_id, created_at"),
        @Index(name = "idx_api_request_metrics_request_id", columnList = "request_id")
})
public class RequestMetric {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @Column(nullable = false, length = 220)
    private String endpoint;
    @Column(nullable = false, length = 16)
    private String method;
    @Column(nullable = false)
    private Integer status;
    @Column(name = "duration_ms", nullable = false)
    private Long durationMs;
    @Column(name = "user_id")
    private Long userId;
    @Column(name = "request_id", length = 80)
    private String requestId;
    @Column(name = "error_code", length = 80)
    private String errorCode;
    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    public static RequestMetricBuilder builder() {
        return new RequestMetricBuilder();
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public String getEndpoint() { return endpoint; }
    public void setEndpoint(String endpoint) { this.endpoint = endpoint; }
    public String getMethod() { return method; }
    public void setMethod(String method) { this.method = method; }
    public Integer getStatus() { return status; }
    public void setStatus(Integer status) { this.status = status; }
    public Long getDurationMs() { return durationMs; }
    public void setDurationMs(Long durationMs) { this.durationMs = durationMs; }
    public Long getUserId() { return userId; }
    public void setUserId(Long userId) { this.userId = userId; }
    public String getRequestId() { return requestId; }
    public void setRequestId(String requestId) { this.requestId = requestId; }
    public String getErrorCode() { return errorCode; }
    public void setErrorCode(String errorCode) { this.errorCode = errorCode; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }

    public static final class RequestMetricBuilder {
        private Long id;
        private String endpoint;
        private String method;
        private Integer status;
        private Long durationMs;
        private Long userId;
        private String requestId;
        private String errorCode;
        private LocalDateTime createdAt;

        private RequestMetricBuilder() {
        }

        public RequestMetricBuilder id(Long id) { this.id = id; return this; }
        public RequestMetricBuilder endpoint(String endpoint) { this.endpoint = endpoint; return this; }
        public RequestMetricBuilder method(String method) { this.method = method; return this; }
        public RequestMetricBuilder status(Integer status) { this.status = status; return this; }
        public RequestMetricBuilder durationMs(Long durationMs) { this.durationMs = durationMs; return this; }
        public RequestMetricBuilder userId(Long userId) { this.userId = userId; return this; }
        public RequestMetricBuilder requestId(String requestId) { this.requestId = requestId; return this; }
        public RequestMetricBuilder errorCode(String errorCode) { this.errorCode = errorCode; return this; }
        public RequestMetricBuilder createdAt(LocalDateTime createdAt) { this.createdAt = createdAt; return this; }

        public RequestMetric build() {
            RequestMetric metric = new RequestMetric();
            metric.setId(id);
            metric.setEndpoint(endpoint);
            metric.setMethod(method);
            metric.setStatus(status);
            metric.setDurationMs(durationMs);
            metric.setUserId(userId);
            metric.setRequestId(requestId);
            metric.setErrorCode(errorCode);
            metric.setCreatedAt(createdAt);
            return metric;
        }
    }
}
