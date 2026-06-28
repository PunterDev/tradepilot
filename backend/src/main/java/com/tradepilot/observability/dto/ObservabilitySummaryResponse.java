package com.tradepilot.observability.dto;

import java.math.BigDecimal;
import java.util.List;

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

    @java.lang.SuppressWarnings("all")
    public ObservabilitySummaryResponse() {
    }

    @java.lang.SuppressWarnings("all")
    public long getRequestsToday() {
        return this.requestsToday;
    }

    @java.lang.SuppressWarnings("all")
    public double getAvgResponseTime() {
        return this.avgResponseTime;
    }

    @java.lang.SuppressWarnings("all")
    public long getP95ResponseTime() {
        return this.p95ResponseTime;
    }

    @java.lang.SuppressWarnings("all")
    public String getSlowestEndpoint() {
        return this.slowestEndpoint;
    }

    @java.lang.SuppressWarnings("all")
    public double getErrorRate() {
        return this.errorRate;
    }

    @java.lang.SuppressWarnings("all")
    public long getAiCallsToday() {
        return this.aiCallsToday;
    }

    @java.lang.SuppressWarnings("all")
    public long getAiTokensToday() {
        return this.aiTokensToday;
    }

    @java.lang.SuppressWarnings("all")
    public BigDecimal getAiCostToday() {
        return this.aiCostToday;
    }

    @java.lang.SuppressWarnings("all")
    public BigDecimal getAiCostMonth() {
        return this.aiCostMonth;
    }

    @java.lang.SuppressWarnings("all")
    public double getAiAvgResponseTime() {
        return this.aiAvgResponseTime;
    }

    @java.lang.SuppressWarnings("all")
    public String getMostExpensiveFeature() {
        return this.mostExpensiveFeature;
    }

    @java.lang.SuppressWarnings("all")
    public String getMostUsedModel() {
        return this.mostUsedModel;
    }

    @java.lang.SuppressWarnings("all")
    public long getCacheHits() {
        return this.cacheHits;
    }

    @java.lang.SuppressWarnings("all")
    public long getCacheMisses() {
        return this.cacheMisses;
    }

    @java.lang.SuppressWarnings("all")
    public double getCacheHitRate() {
        return this.cacheHitRate;
    }

    @java.lang.SuppressWarnings("all")
    public long getEmailsSent() {
        return this.emailsSent;
    }

    @java.lang.SuppressWarnings("all")
    public long getEmailsFailed() {
        return this.emailsFailed;
    }

    @java.lang.SuppressWarnings("all")
    public double getEmailSuccessRate() {
        return this.emailSuccessRate;
    }

    @java.lang.SuppressWarnings("all")
    public long getStripeSuccesses() {
        return this.stripeSuccesses;
    }

    @java.lang.SuppressWarnings("all")
    public long getStripeFailures() {
        return this.stripeFailures;
    }

    @java.lang.SuppressWarnings("all")
    public double getStripeSuccessRate() {
        return this.stripeSuccessRate;
    }

    @java.lang.SuppressWarnings("all")
    public List<EndpointMetricDto> getSlowestEndpoints() {
        return this.slowestEndpoints;
    }

    @java.lang.SuppressWarnings("all")
    public List<SlowRequestDto> getSlowRequests() {
        return this.slowRequests;
    }

    @java.lang.SuppressWarnings("all")
    public List<AiFeatureCostDto> getAiFeatureCosts() {
        return this.aiFeatureCosts;
    }

    @java.lang.SuppressWarnings("all")
    public List<CacheHealthDto> getCacheHealth() {
        return this.cacheHealth;
    }

    @java.lang.SuppressWarnings("all")
    public List<EmailFailureDto> getRecentEmailFailures() {
        return this.recentEmailFailures;
    }

    @java.lang.SuppressWarnings("all")
    public List<StripeEventDto> getRecentStripeEvents() {
        return this.recentStripeEvents;
    }

    @java.lang.SuppressWarnings("all")
    public List<SystemAlertDto> getAlerts() {
        return this.alerts;
    }

    @java.lang.SuppressWarnings("all")
    public List<SystemAlertDto> getResolvedAlerts() {
        return this.resolvedAlerts;
    }

    @java.lang.SuppressWarnings("all")
    public void setRequestsToday(final long requestsToday) {
        this.requestsToday = requestsToday;
    }

    @java.lang.SuppressWarnings("all")
    public void setAvgResponseTime(final double avgResponseTime) {
        this.avgResponseTime = avgResponseTime;
    }

    @java.lang.SuppressWarnings("all")
    public void setP95ResponseTime(final long p95ResponseTime) {
        this.p95ResponseTime = p95ResponseTime;
    }

    @java.lang.SuppressWarnings("all")
    public void setSlowestEndpoint(final String slowestEndpoint) {
        this.slowestEndpoint = slowestEndpoint;
    }

    @java.lang.SuppressWarnings("all")
    public void setErrorRate(final double errorRate) {
        this.errorRate = errorRate;
    }

    @java.lang.SuppressWarnings("all")
    public void setAiCallsToday(final long aiCallsToday) {
        this.aiCallsToday = aiCallsToday;
    }

    @java.lang.SuppressWarnings("all")
    public void setAiTokensToday(final long aiTokensToday) {
        this.aiTokensToday = aiTokensToday;
    }

    @java.lang.SuppressWarnings("all")
    public void setAiCostToday(final BigDecimal aiCostToday) {
        this.aiCostToday = aiCostToday;
    }

    @java.lang.SuppressWarnings("all")
    public void setAiCostMonth(final BigDecimal aiCostMonth) {
        this.aiCostMonth = aiCostMonth;
    }

    @java.lang.SuppressWarnings("all")
    public void setAiAvgResponseTime(final double aiAvgResponseTime) {
        this.aiAvgResponseTime = aiAvgResponseTime;
    }

    @java.lang.SuppressWarnings("all")
    public void setMostExpensiveFeature(final String mostExpensiveFeature) {
        this.mostExpensiveFeature = mostExpensiveFeature;
    }

    @java.lang.SuppressWarnings("all")
    public void setMostUsedModel(final String mostUsedModel) {
        this.mostUsedModel = mostUsedModel;
    }

    @java.lang.SuppressWarnings("all")
    public void setCacheHits(final long cacheHits) {
        this.cacheHits = cacheHits;
    }

    @java.lang.SuppressWarnings("all")
    public void setCacheMisses(final long cacheMisses) {
        this.cacheMisses = cacheMisses;
    }

    @java.lang.SuppressWarnings("all")
    public void setCacheHitRate(final double cacheHitRate) {
        this.cacheHitRate = cacheHitRate;
    }

    @java.lang.SuppressWarnings("all")
    public void setEmailsSent(final long emailsSent) {
        this.emailsSent = emailsSent;
    }

    @java.lang.SuppressWarnings("all")
    public void setEmailsFailed(final long emailsFailed) {
        this.emailsFailed = emailsFailed;
    }

    @java.lang.SuppressWarnings("all")
    public void setEmailSuccessRate(final double emailSuccessRate) {
        this.emailSuccessRate = emailSuccessRate;
    }

    @java.lang.SuppressWarnings("all")
    public void setStripeSuccesses(final long stripeSuccesses) {
        this.stripeSuccesses = stripeSuccesses;
    }

    @java.lang.SuppressWarnings("all")
    public void setStripeFailures(final long stripeFailures) {
        this.stripeFailures = stripeFailures;
    }

    @java.lang.SuppressWarnings("all")
    public void setStripeSuccessRate(final double stripeSuccessRate) {
        this.stripeSuccessRate = stripeSuccessRate;
    }

    @java.lang.SuppressWarnings("all")
    public void setSlowestEndpoints(final List<EndpointMetricDto> slowestEndpoints) {
        this.slowestEndpoints = slowestEndpoints;
    }

    @java.lang.SuppressWarnings("all")
    public void setSlowRequests(final List<SlowRequestDto> slowRequests) {
        this.slowRequests = slowRequests;
    }

    @java.lang.SuppressWarnings("all")
    public void setAiFeatureCosts(final List<AiFeatureCostDto> aiFeatureCosts) {
        this.aiFeatureCosts = aiFeatureCosts;
    }

    @java.lang.SuppressWarnings("all")
    public void setCacheHealth(final List<CacheHealthDto> cacheHealth) {
        this.cacheHealth = cacheHealth;
    }

    @java.lang.SuppressWarnings("all")
    public void setRecentEmailFailures(final List<EmailFailureDto> recentEmailFailures) {
        this.recentEmailFailures = recentEmailFailures;
    }

    @java.lang.SuppressWarnings("all")
    public void setRecentStripeEvents(final List<StripeEventDto> recentStripeEvents) {
        this.recentStripeEvents = recentStripeEvents;
    }

    @java.lang.SuppressWarnings("all")
    public void setAlerts(final List<SystemAlertDto> alerts) {
        this.alerts = alerts;
    }

    @java.lang.SuppressWarnings("all")
    public void setResolvedAlerts(final List<SystemAlertDto> resolvedAlerts) {
        this.resolvedAlerts = resolvedAlerts;
    }

    @java.lang.Override
    @java.lang.SuppressWarnings("all")
    public boolean equals(final java.lang.Object o) {
        if (o == this) return true;
        if (!(o instanceof ObservabilitySummaryResponse)) return false;
        final ObservabilitySummaryResponse other = (ObservabilitySummaryResponse) o;
        if (!other.canEqual((java.lang.Object) this)) return false;
        if (this.getRequestsToday() != other.getRequestsToday()) return false;
        if (java.lang.Double.compare(this.getAvgResponseTime(), other.getAvgResponseTime()) != 0) return false;
        if (this.getP95ResponseTime() != other.getP95ResponseTime()) return false;
        if (java.lang.Double.compare(this.getErrorRate(), other.getErrorRate()) != 0) return false;
        if (this.getAiCallsToday() != other.getAiCallsToday()) return false;
        if (this.getAiTokensToday() != other.getAiTokensToday()) return false;
        if (java.lang.Double.compare(this.getAiAvgResponseTime(), other.getAiAvgResponseTime()) != 0) return false;
        if (this.getCacheHits() != other.getCacheHits()) return false;
        if (this.getCacheMisses() != other.getCacheMisses()) return false;
        if (java.lang.Double.compare(this.getCacheHitRate(), other.getCacheHitRate()) != 0) return false;
        if (this.getEmailsSent() != other.getEmailsSent()) return false;
        if (this.getEmailsFailed() != other.getEmailsFailed()) return false;
        if (java.lang.Double.compare(this.getEmailSuccessRate(), other.getEmailSuccessRate()) != 0) return false;
        if (this.getStripeSuccesses() != other.getStripeSuccesses()) return false;
        if (this.getStripeFailures() != other.getStripeFailures()) return false;
        if (java.lang.Double.compare(this.getStripeSuccessRate(), other.getStripeSuccessRate()) != 0) return false;
        final java.lang.Object this$slowestEndpoint = this.getSlowestEndpoint();
        final java.lang.Object other$slowestEndpoint = other.getSlowestEndpoint();
        if (this$slowestEndpoint == null ? other$slowestEndpoint != null : !this$slowestEndpoint.equals(other$slowestEndpoint)) return false;
        final java.lang.Object this$aiCostToday = this.getAiCostToday();
        final java.lang.Object other$aiCostToday = other.getAiCostToday();
        if (this$aiCostToday == null ? other$aiCostToday != null : !this$aiCostToday.equals(other$aiCostToday)) return false;
        final java.lang.Object this$aiCostMonth = this.getAiCostMonth();
        final java.lang.Object other$aiCostMonth = other.getAiCostMonth();
        if (this$aiCostMonth == null ? other$aiCostMonth != null : !this$aiCostMonth.equals(other$aiCostMonth)) return false;
        final java.lang.Object this$mostExpensiveFeature = this.getMostExpensiveFeature();
        final java.lang.Object other$mostExpensiveFeature = other.getMostExpensiveFeature();
        if (this$mostExpensiveFeature == null ? other$mostExpensiveFeature != null : !this$mostExpensiveFeature.equals(other$mostExpensiveFeature)) return false;
        final java.lang.Object this$mostUsedModel = this.getMostUsedModel();
        final java.lang.Object other$mostUsedModel = other.getMostUsedModel();
        if (this$mostUsedModel == null ? other$mostUsedModel != null : !this$mostUsedModel.equals(other$mostUsedModel)) return false;
        final java.lang.Object this$slowestEndpoints = this.getSlowestEndpoints();
        final java.lang.Object other$slowestEndpoints = other.getSlowestEndpoints();
        if (this$slowestEndpoints == null ? other$slowestEndpoints != null : !this$slowestEndpoints.equals(other$slowestEndpoints)) return false;
        final java.lang.Object this$slowRequests = this.getSlowRequests();
        final java.lang.Object other$slowRequests = other.getSlowRequests();
        if (this$slowRequests == null ? other$slowRequests != null : !this$slowRequests.equals(other$slowRequests)) return false;
        final java.lang.Object this$aiFeatureCosts = this.getAiFeatureCosts();
        final java.lang.Object other$aiFeatureCosts = other.getAiFeatureCosts();
        if (this$aiFeatureCosts == null ? other$aiFeatureCosts != null : !this$aiFeatureCosts.equals(other$aiFeatureCosts)) return false;
        final java.lang.Object this$cacheHealth = this.getCacheHealth();
        final java.lang.Object other$cacheHealth = other.getCacheHealth();
        if (this$cacheHealth == null ? other$cacheHealth != null : !this$cacheHealth.equals(other$cacheHealth)) return false;
        final java.lang.Object this$recentEmailFailures = this.getRecentEmailFailures();
        final java.lang.Object other$recentEmailFailures = other.getRecentEmailFailures();
        if (this$recentEmailFailures == null ? other$recentEmailFailures != null : !this$recentEmailFailures.equals(other$recentEmailFailures)) return false;
        final java.lang.Object this$recentStripeEvents = this.getRecentStripeEvents();
        final java.lang.Object other$recentStripeEvents = other.getRecentStripeEvents();
        if (this$recentStripeEvents == null ? other$recentStripeEvents != null : !this$recentStripeEvents.equals(other$recentStripeEvents)) return false;
        final java.lang.Object this$alerts = this.getAlerts();
        final java.lang.Object other$alerts = other.getAlerts();
        if (this$alerts == null ? other$alerts != null : !this$alerts.equals(other$alerts)) return false;
        final java.lang.Object this$resolvedAlerts = this.getResolvedAlerts();
        final java.lang.Object other$resolvedAlerts = other.getResolvedAlerts();
        if (this$resolvedAlerts == null ? other$resolvedAlerts != null : !this$resolvedAlerts.equals(other$resolvedAlerts)) return false;
        return true;
    }

    @java.lang.SuppressWarnings("all")
    protected boolean canEqual(final java.lang.Object other) {
        return other instanceof ObservabilitySummaryResponse;
    }

    @java.lang.Override
    @java.lang.SuppressWarnings("all")
    public int hashCode() {
        final int PRIME = 59;
        int result = 1;
        final long $requestsToday = this.getRequestsToday();
        result = result * PRIME + (int) ($requestsToday >>> 32 ^ $requestsToday);
        final long $avgResponseTime = java.lang.Double.doubleToLongBits(this.getAvgResponseTime());
        result = result * PRIME + (int) ($avgResponseTime >>> 32 ^ $avgResponseTime);
        final long $p95ResponseTime = this.getP95ResponseTime();
        result = result * PRIME + (int) ($p95ResponseTime >>> 32 ^ $p95ResponseTime);
        final long $errorRate = java.lang.Double.doubleToLongBits(this.getErrorRate());
        result = result * PRIME + (int) ($errorRate >>> 32 ^ $errorRate);
        final long $aiCallsToday = this.getAiCallsToday();
        result = result * PRIME + (int) ($aiCallsToday >>> 32 ^ $aiCallsToday);
        final long $aiTokensToday = this.getAiTokensToday();
        result = result * PRIME + (int) ($aiTokensToday >>> 32 ^ $aiTokensToday);
        final long $aiAvgResponseTime = java.lang.Double.doubleToLongBits(this.getAiAvgResponseTime());
        result = result * PRIME + (int) ($aiAvgResponseTime >>> 32 ^ $aiAvgResponseTime);
        final long $cacheHits = this.getCacheHits();
        result = result * PRIME + (int) ($cacheHits >>> 32 ^ $cacheHits);
        final long $cacheMisses = this.getCacheMisses();
        result = result * PRIME + (int) ($cacheMisses >>> 32 ^ $cacheMisses);
        final long $cacheHitRate = java.lang.Double.doubleToLongBits(this.getCacheHitRate());
        result = result * PRIME + (int) ($cacheHitRate >>> 32 ^ $cacheHitRate);
        final long $emailsSent = this.getEmailsSent();
        result = result * PRIME + (int) ($emailsSent >>> 32 ^ $emailsSent);
        final long $emailsFailed = this.getEmailsFailed();
        result = result * PRIME + (int) ($emailsFailed >>> 32 ^ $emailsFailed);
        final long $emailSuccessRate = java.lang.Double.doubleToLongBits(this.getEmailSuccessRate());
        result = result * PRIME + (int) ($emailSuccessRate >>> 32 ^ $emailSuccessRate);
        final long $stripeSuccesses = this.getStripeSuccesses();
        result = result * PRIME + (int) ($stripeSuccesses >>> 32 ^ $stripeSuccesses);
        final long $stripeFailures = this.getStripeFailures();
        result = result * PRIME + (int) ($stripeFailures >>> 32 ^ $stripeFailures);
        final long $stripeSuccessRate = java.lang.Double.doubleToLongBits(this.getStripeSuccessRate());
        result = result * PRIME + (int) ($stripeSuccessRate >>> 32 ^ $stripeSuccessRate);
        final java.lang.Object $slowestEndpoint = this.getSlowestEndpoint();
        result = result * PRIME + ($slowestEndpoint == null ? 43 : $slowestEndpoint.hashCode());
        final java.lang.Object $aiCostToday = this.getAiCostToday();
        result = result * PRIME + ($aiCostToday == null ? 43 : $aiCostToday.hashCode());
        final java.lang.Object $aiCostMonth = this.getAiCostMonth();
        result = result * PRIME + ($aiCostMonth == null ? 43 : $aiCostMonth.hashCode());
        final java.lang.Object $mostExpensiveFeature = this.getMostExpensiveFeature();
        result = result * PRIME + ($mostExpensiveFeature == null ? 43 : $mostExpensiveFeature.hashCode());
        final java.lang.Object $mostUsedModel = this.getMostUsedModel();
        result = result * PRIME + ($mostUsedModel == null ? 43 : $mostUsedModel.hashCode());
        final java.lang.Object $slowestEndpoints = this.getSlowestEndpoints();
        result = result * PRIME + ($slowestEndpoints == null ? 43 : $slowestEndpoints.hashCode());
        final java.lang.Object $slowRequests = this.getSlowRequests();
        result = result * PRIME + ($slowRequests == null ? 43 : $slowRequests.hashCode());
        final java.lang.Object $aiFeatureCosts = this.getAiFeatureCosts();
        result = result * PRIME + ($aiFeatureCosts == null ? 43 : $aiFeatureCosts.hashCode());
        final java.lang.Object $cacheHealth = this.getCacheHealth();
        result = result * PRIME + ($cacheHealth == null ? 43 : $cacheHealth.hashCode());
        final java.lang.Object $recentEmailFailures = this.getRecentEmailFailures();
        result = result * PRIME + ($recentEmailFailures == null ? 43 : $recentEmailFailures.hashCode());
        final java.lang.Object $recentStripeEvents = this.getRecentStripeEvents();
        result = result * PRIME + ($recentStripeEvents == null ? 43 : $recentStripeEvents.hashCode());
        final java.lang.Object $alerts = this.getAlerts();
        result = result * PRIME + ($alerts == null ? 43 : $alerts.hashCode());
        final java.lang.Object $resolvedAlerts = this.getResolvedAlerts();
        result = result * PRIME + ($resolvedAlerts == null ? 43 : $resolvedAlerts.hashCode());
        return result;
    }

    @java.lang.Override
    @java.lang.SuppressWarnings("all")
    public java.lang.String toString() {
        return "ObservabilitySummaryResponse(requestsToday=" + this.getRequestsToday() + ", avgResponseTime=" + this.getAvgResponseTime() + ", p95ResponseTime=" + this.getP95ResponseTime() + ", slowestEndpoint=" + this.getSlowestEndpoint() + ", errorRate=" + this.getErrorRate() + ", aiCallsToday=" + this.getAiCallsToday() + ", aiTokensToday=" + this.getAiTokensToday() + ", aiCostToday=" + this.getAiCostToday() + ", aiCostMonth=" + this.getAiCostMonth() + ", aiAvgResponseTime=" + this.getAiAvgResponseTime() + ", mostExpensiveFeature=" + this.getMostExpensiveFeature() + ", mostUsedModel=" + this.getMostUsedModel() + ", cacheHits=" + this.getCacheHits() + ", cacheMisses=" + this.getCacheMisses() + ", cacheHitRate=" + this.getCacheHitRate() + ", emailsSent=" + this.getEmailsSent() + ", emailsFailed=" + this.getEmailsFailed() + ", emailSuccessRate=" + this.getEmailSuccessRate() + ", stripeSuccesses=" + this.getStripeSuccesses() + ", stripeFailures=" + this.getStripeFailures() + ", stripeSuccessRate=" + this.getStripeSuccessRate() + ", slowestEndpoints=" + this.getSlowestEndpoints() + ", slowRequests=" + this.getSlowRequests() + ", aiFeatureCosts=" + this.getAiFeatureCosts() + ", cacheHealth=" + this.getCacheHealth() + ", recentEmailFailures=" + this.getRecentEmailFailures() + ", recentStripeEvents=" + this.getRecentStripeEvents() + ", alerts=" + this.getAlerts() + ", resolvedAlerts=" + this.getResolvedAlerts() + ")";
    }
}
