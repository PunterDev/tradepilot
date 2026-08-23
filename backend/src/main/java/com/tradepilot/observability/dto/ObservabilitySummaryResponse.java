package com.tradepilot.observability.dto;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

public class ObservabilitySummaryResponse {
    private long requestsToday;
    private double avgResponseTime;
    private long p95ResponseTime;
    private String slowestEndpoint;
    private double errorRate;
    private String errorRateSeverity;
    private long aiCallsToday;
    private long aiFailuresToday;
    private long aiTokensToday;
    private BigDecimal aiCostToday;
    private BigDecimal aiCostMonth;
    private double aiAvgResponseTime;
    private long aiGenerationP95ResponseTime;
    private String aiGenerationP95Severity;
    private String mostExpensiveFeature;
    private String mostUsedModel;
    private long cacheHits;
    private long cacheMisses;
    private double cacheHitRate;
    private String cacheHitRateSeverity;
    private long emailsSent;
    private long emailsFailed;
    private double emailSuccessRate;
    private Boolean emailHasAttempts;
    private String emailSuccessRateDisplay;
    private String emailHealthSeverity;
    private long stripeSuccesses;
    private long stripeFailures;
    private double stripeSuccessRate;
    private Boolean stripeHasEvents;
    private String stripeSuccessRateDisplay;
    private String stripeHealthSeverity;
    private String lastStripeEventType;
    private LocalDateTime lastStripeEventTime;
    private long creditPackPurchasesCompletedToday;
    private long standardApiP95ResponseTime;
    private String standardApiP95Severity;
    private long adminApiP95ResponseTime;
    private List<EndpointMetricDto> slowestEndpoints;
    private List<SlowRequestDto> slowRequests;
    private List<AiFeatureCostDto> aiFeatureCosts;
    private List<CacheHealthDto> cacheHealth;
    private List<EmailFailureDto> recentEmailFailures;
    private List<StripeEventDto> recentStripeEvents;
    private List<SystemAlertDto> alerts;
    private List<SystemAlertDto> resolvedAlerts;
    private List<ErrorStatusBreakdownDto> errorsByStatus;
    private List<ErrorEndpointBreakdownDto> errorsByEndpoint;
    private List<LatencyCategoryDto> latencyCategories;
    private FailureSummaryDto lastApiFailure;
    private FailureSummaryDto lastStripeFailure;
    private FailureSummaryDto lastEmailFailure;
    private FailureSummaryDto lastAiFailure;

    public long getRequestsToday() { return requestsToday; }
    public void setRequestsToday(long requestsToday) { this.requestsToday = requestsToday; }
    public double getAvgResponseTime() { return avgResponseTime; }
    public void setAvgResponseTime(double avgResponseTime) { this.avgResponseTime = avgResponseTime; }
    public long getP95ResponseTime() { return p95ResponseTime; }
    public void setP95ResponseTime(long p95ResponseTime) { this.p95ResponseTime = p95ResponseTime; }
    public String getSlowestEndpoint() { return slowestEndpoint; }
    public void setSlowestEndpoint(String slowestEndpoint) { this.slowestEndpoint = slowestEndpoint; }
    public double getErrorRate() { return errorRate; }
    public void setErrorRate(double errorRate) { this.errorRate = errorRate; }
    public String getErrorRateSeverity() { return errorRateSeverity; }
    public void setErrorRateSeverity(String errorRateSeverity) { this.errorRateSeverity = errorRateSeverity; }
    public long getAiCallsToday() { return aiCallsToday; }
    public void setAiCallsToday(long aiCallsToday) { this.aiCallsToday = aiCallsToday; }
    public long getAiFailuresToday() { return aiFailuresToday; }
    public void setAiFailuresToday(long aiFailuresToday) { this.aiFailuresToday = aiFailuresToday; }
    public long getAiTokensToday() { return aiTokensToday; }
    public void setAiTokensToday(long aiTokensToday) { this.aiTokensToday = aiTokensToday; }
    public BigDecimal getAiCostToday() { return aiCostToday; }
    public void setAiCostToday(BigDecimal aiCostToday) { this.aiCostToday = aiCostToday; }
    public BigDecimal getAiCostMonth() { return aiCostMonth; }
    public void setAiCostMonth(BigDecimal aiCostMonth) { this.aiCostMonth = aiCostMonth; }
    public double getAiAvgResponseTime() { return aiAvgResponseTime; }
    public void setAiAvgResponseTime(double aiAvgResponseTime) { this.aiAvgResponseTime = aiAvgResponseTime; }
    public long getAiGenerationP95ResponseTime() { return aiGenerationP95ResponseTime; }
    public void setAiGenerationP95ResponseTime(long aiGenerationP95ResponseTime) { this.aiGenerationP95ResponseTime = aiGenerationP95ResponseTime; }
    public String getAiGenerationP95Severity() { return aiGenerationP95Severity; }
    public void setAiGenerationP95Severity(String aiGenerationP95Severity) { this.aiGenerationP95Severity = aiGenerationP95Severity; }
    public String getMostExpensiveFeature() { return mostExpensiveFeature; }
    public void setMostExpensiveFeature(String mostExpensiveFeature) { this.mostExpensiveFeature = mostExpensiveFeature; }
    public String getMostUsedModel() { return mostUsedModel; }
    public void setMostUsedModel(String mostUsedModel) { this.mostUsedModel = mostUsedModel; }
    public long getCacheHits() { return cacheHits; }
    public void setCacheHits(long cacheHits) { this.cacheHits = cacheHits; }
    public long getCacheMisses() { return cacheMisses; }
    public void setCacheMisses(long cacheMisses) { this.cacheMisses = cacheMisses; }
    public double getCacheHitRate() { return cacheHitRate; }
    public void setCacheHitRate(double cacheHitRate) { this.cacheHitRate = cacheHitRate; }
    public String getCacheHitRateSeverity() { return cacheHitRateSeverity; }
    public void setCacheHitRateSeverity(String cacheHitRateSeverity) { this.cacheHitRateSeverity = cacheHitRateSeverity; }
    public long getEmailsSent() { return emailsSent; }
    public void setEmailsSent(long emailsSent) { this.emailsSent = emailsSent; }
    public long getEmailsFailed() { return emailsFailed; }
    public void setEmailsFailed(long emailsFailed) { this.emailsFailed = emailsFailed; }
    public double getEmailSuccessRate() { return emailSuccessRate; }
    public void setEmailSuccessRate(double emailSuccessRate) { this.emailSuccessRate = emailSuccessRate; }
    public Boolean getEmailHasAttempts() { return emailHasAttempts; }
    public void setEmailHasAttempts(Boolean emailHasAttempts) { this.emailHasAttempts = emailHasAttempts; }
    public String getEmailSuccessRateDisplay() { return emailSuccessRateDisplay; }
    public void setEmailSuccessRateDisplay(String emailSuccessRateDisplay) { this.emailSuccessRateDisplay = emailSuccessRateDisplay; }
    public String getEmailHealthSeverity() { return emailHealthSeverity; }
    public void setEmailHealthSeverity(String emailHealthSeverity) { this.emailHealthSeverity = emailHealthSeverity; }
    public long getStripeSuccesses() { return stripeSuccesses; }
    public void setStripeSuccesses(long stripeSuccesses) { this.stripeSuccesses = stripeSuccesses; }
    public long getStripeFailures() { return stripeFailures; }
    public void setStripeFailures(long stripeFailures) { this.stripeFailures = stripeFailures; }
    public double getStripeSuccessRate() { return stripeSuccessRate; }
    public void setStripeSuccessRate(double stripeSuccessRate) { this.stripeSuccessRate = stripeSuccessRate; }
    public Boolean getStripeHasEvents() { return stripeHasEvents; }
    public void setStripeHasEvents(Boolean stripeHasEvents) { this.stripeHasEvents = stripeHasEvents; }
    public String getStripeSuccessRateDisplay() { return stripeSuccessRateDisplay; }
    public void setStripeSuccessRateDisplay(String stripeSuccessRateDisplay) { this.stripeSuccessRateDisplay = stripeSuccessRateDisplay; }
    public String getStripeHealthSeverity() { return stripeHealthSeverity; }
    public void setStripeHealthSeverity(String stripeHealthSeverity) { this.stripeHealthSeverity = stripeHealthSeverity; }
    public String getLastStripeEventType() { return lastStripeEventType; }
    public void setLastStripeEventType(String lastStripeEventType) { this.lastStripeEventType = lastStripeEventType; }
    public LocalDateTime getLastStripeEventTime() { return lastStripeEventTime; }
    public void setLastStripeEventTime(LocalDateTime lastStripeEventTime) { this.lastStripeEventTime = lastStripeEventTime; }
    public long getCreditPackPurchasesCompletedToday() { return creditPackPurchasesCompletedToday; }
    public void setCreditPackPurchasesCompletedToday(long creditPackPurchasesCompletedToday) { this.creditPackPurchasesCompletedToday = creditPackPurchasesCompletedToday; }
    public long getStandardApiP95ResponseTime() { return standardApiP95ResponseTime; }
    public void setStandardApiP95ResponseTime(long standardApiP95ResponseTime) { this.standardApiP95ResponseTime = standardApiP95ResponseTime; }
    public String getStandardApiP95Severity() { return standardApiP95Severity; }
    public void setStandardApiP95Severity(String standardApiP95Severity) { this.standardApiP95Severity = standardApiP95Severity; }
    public long getAdminApiP95ResponseTime() { return adminApiP95ResponseTime; }
    public void setAdminApiP95ResponseTime(long adminApiP95ResponseTime) { this.adminApiP95ResponseTime = adminApiP95ResponseTime; }
    public List<EndpointMetricDto> getSlowestEndpoints() { return slowestEndpoints; }
    public void setSlowestEndpoints(List<EndpointMetricDto> slowestEndpoints) { this.slowestEndpoints = slowestEndpoints; }
    public List<SlowRequestDto> getSlowRequests() { return slowRequests; }
    public void setSlowRequests(List<SlowRequestDto> slowRequests) { this.slowRequests = slowRequests; }
    public List<AiFeatureCostDto> getAiFeatureCosts() { return aiFeatureCosts; }
    public void setAiFeatureCosts(List<AiFeatureCostDto> aiFeatureCosts) { this.aiFeatureCosts = aiFeatureCosts; }
    public List<CacheHealthDto> getCacheHealth() { return cacheHealth; }
    public void setCacheHealth(List<CacheHealthDto> cacheHealth) { this.cacheHealth = cacheHealth; }
    public List<EmailFailureDto> getRecentEmailFailures() { return recentEmailFailures; }
    public void setRecentEmailFailures(List<EmailFailureDto> recentEmailFailures) { this.recentEmailFailures = recentEmailFailures; }
    public List<StripeEventDto> getRecentStripeEvents() { return recentStripeEvents; }
    public void setRecentStripeEvents(List<StripeEventDto> recentStripeEvents) { this.recentStripeEvents = recentStripeEvents; }
    public List<SystemAlertDto> getAlerts() { return alerts; }
    public void setAlerts(List<SystemAlertDto> alerts) { this.alerts = alerts; }
    public List<SystemAlertDto> getResolvedAlerts() { return resolvedAlerts; }
    public void setResolvedAlerts(List<SystemAlertDto> resolvedAlerts) { this.resolvedAlerts = resolvedAlerts; }
    public List<ErrorStatusBreakdownDto> getErrorsByStatus() { return errorsByStatus; }
    public void setErrorsByStatus(List<ErrorStatusBreakdownDto> errorsByStatus) { this.errorsByStatus = errorsByStatus; }
    public List<ErrorEndpointBreakdownDto> getErrorsByEndpoint() { return errorsByEndpoint; }
    public void setErrorsByEndpoint(List<ErrorEndpointBreakdownDto> errorsByEndpoint) { this.errorsByEndpoint = errorsByEndpoint; }
    public List<LatencyCategoryDto> getLatencyCategories() { return latencyCategories; }
    public void setLatencyCategories(List<LatencyCategoryDto> latencyCategories) { this.latencyCategories = latencyCategories; }
    public FailureSummaryDto getLastApiFailure() { return lastApiFailure; }
    public void setLastApiFailure(FailureSummaryDto lastApiFailure) { this.lastApiFailure = lastApiFailure; }
    public FailureSummaryDto getLastStripeFailure() { return lastStripeFailure; }
    public void setLastStripeFailure(FailureSummaryDto lastStripeFailure) { this.lastStripeFailure = lastStripeFailure; }
    public FailureSummaryDto getLastEmailFailure() { return lastEmailFailure; }
    public void setLastEmailFailure(FailureSummaryDto lastEmailFailure) { this.lastEmailFailure = lastEmailFailure; }
    public FailureSummaryDto getLastAiFailure() { return lastAiFailure; }
    public void setLastAiFailure(FailureSummaryDto lastAiFailure) { this.lastAiFailure = lastAiFailure; }
}
