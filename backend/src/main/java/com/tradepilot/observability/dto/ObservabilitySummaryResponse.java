package com.tradepilot.observability.dto;

import lombok.Data;

import java.math.BigDecimal;
import java.util.List;

@Data
public class ObservabilitySummaryResponse {
    private long requestsToday;
    private double avgResponseTime;
    private long p95ResponseTime;
    private String slowestEndpoint;
    private double errorRate;

    private long aiCallsToday;
    private long aiTokensToday;
    private BigDecimal aiCostToday;
    private BigDecimal aiCostMonth;
    private double aiAvgResponseTime;
    private String mostExpensiveFeature;
    private String mostUsedModel;

    private long cacheHits;
    private long cacheMisses;
    private double cacheHitRate;

    private long emailsSent;
    private long emailsFailed;
    private double emailSuccessRate;

    private long stripeSuccesses;
    private long stripeFailures;
    private double stripeSuccessRate;

    private List<EndpointMetricDto> slowestEndpoints;
    private List<SlowRequestDto> slowRequests;
    private List<AiFeatureCostDto> aiFeatureCosts;
    private List<CacheHealthDto> cacheHealth;
    private List<EmailFailureDto> recentEmailFailures;
    private List<StripeEventDto> recentStripeEvents;
    private List<SystemAlertDto> alerts;
    private List<SystemAlertDto> resolvedAlerts;
}
