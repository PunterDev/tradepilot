package com.tradepilot.observability.dto;

import java.util.List;

public class ObservabilityTrendsResponse {
    private List<ApiTrendPointDto> apiPerformance;
    private List<AiTrendPointDto> aiCosts;

    @java.lang.SuppressWarnings("all")
    public ObservabilityTrendsResponse() {
    }

    @java.lang.SuppressWarnings("all")
    public List<ApiTrendPointDto> getApiPerformance() {
        return this.apiPerformance;
    }

    @java.lang.SuppressWarnings("all")
    public List<AiTrendPointDto> getAiCosts() {
        return this.aiCosts;
    }

    @java.lang.SuppressWarnings("all")
    public void setApiPerformance(final List<ApiTrendPointDto> apiPerformance) {
        this.apiPerformance = apiPerformance;
    }

    @java.lang.SuppressWarnings("all")
    public void setAiCosts(final List<AiTrendPointDto> aiCosts) {
        this.aiCosts = aiCosts;
    }

    @java.lang.Override
    @java.lang.SuppressWarnings("all")
    public boolean equals(final java.lang.Object o) {
        if (o == this) return true;
        if (!(o instanceof ObservabilityTrendsResponse)) return false;
        final ObservabilityTrendsResponse other = (ObservabilityTrendsResponse) o;
        if (!other.canEqual((java.lang.Object) this)) return false;
        final java.lang.Object this$apiPerformance = this.getApiPerformance();
        final java.lang.Object other$apiPerformance = other.getApiPerformance();
        if (this$apiPerformance == null ? other$apiPerformance != null : !this$apiPerformance.equals(other$apiPerformance)) return false;
        final java.lang.Object this$aiCosts = this.getAiCosts();
        final java.lang.Object other$aiCosts = other.getAiCosts();
        if (this$aiCosts == null ? other$aiCosts != null : !this$aiCosts.equals(other$aiCosts)) return false;
        return true;
    }

    @java.lang.SuppressWarnings("all")
    protected boolean canEqual(final java.lang.Object other) {
        return other instanceof ObservabilityTrendsResponse;
    }

    @java.lang.Override
    @java.lang.SuppressWarnings("all")
    public int hashCode() {
        final int PRIME = 59;
        int result = 1;
        final java.lang.Object $apiPerformance = this.getApiPerformance();
        result = result * PRIME + ($apiPerformance == null ? 43 : $apiPerformance.hashCode());
        final java.lang.Object $aiCosts = this.getAiCosts();
        result = result * PRIME + ($aiCosts == null ? 43 : $aiCosts.hashCode());
        return result;
    }

    @java.lang.Override
    @java.lang.SuppressWarnings("all")
    public java.lang.String toString() {
        return "ObservabilityTrendsResponse(apiPerformance=" + this.getApiPerformance() + ", aiCosts=" + this.getAiCosts() + ")";
    }
}
