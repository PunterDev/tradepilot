package com.tradepilot.dto.session;

import java.time.LocalDateTime;

public class AiSessionReviewResponse {
    private Long id;
    private String summary;
    private String whatWentWell;
    private String whatHurtPerformance;
    private String biggestMistake;
    private String bestSetup;
    private String worstDecision;
    private String emotionalPattern;
    private String executionPattern;
    private String riskManagementPattern;
    private String oneRuleForTomorrow;
    private LocalDateTime generatedAt;

    @java.lang.SuppressWarnings("all")
    public AiSessionReviewResponse() {
    }

    @java.lang.SuppressWarnings("all")
    public Long getId() {
        return this.id;
    }

    @java.lang.SuppressWarnings("all")
    public String getSummary() {
        return this.summary;
    }

    @java.lang.SuppressWarnings("all")
    public String getWhatWentWell() {
        return this.whatWentWell;
    }

    @java.lang.SuppressWarnings("all")
    public String getWhatHurtPerformance() {
        return this.whatHurtPerformance;
    }

    @java.lang.SuppressWarnings("all")
    public String getBiggestMistake() {
        return this.biggestMistake;
    }

    @java.lang.SuppressWarnings("all")
    public String getBestSetup() {
        return this.bestSetup;
    }

    @java.lang.SuppressWarnings("all")
    public String getWorstDecision() {
        return this.worstDecision;
    }

    @java.lang.SuppressWarnings("all")
    public String getEmotionalPattern() {
        return this.emotionalPattern;
    }

    @java.lang.SuppressWarnings("all")
    public String getExecutionPattern() {
        return this.executionPattern;
    }

    @java.lang.SuppressWarnings("all")
    public String getRiskManagementPattern() {
        return this.riskManagementPattern;
    }

    @java.lang.SuppressWarnings("all")
    public String getOneRuleForTomorrow() {
        return this.oneRuleForTomorrow;
    }

    @java.lang.SuppressWarnings("all")
    public LocalDateTime getGeneratedAt() {
        return this.generatedAt;
    }

    @java.lang.SuppressWarnings("all")
    public void setId(final Long id) {
        this.id = id;
    }

    @java.lang.SuppressWarnings("all")
    public void setSummary(final String summary) {
        this.summary = summary;
    }

    @java.lang.SuppressWarnings("all")
    public void setWhatWentWell(final String whatWentWell) {
        this.whatWentWell = whatWentWell;
    }

    @java.lang.SuppressWarnings("all")
    public void setWhatHurtPerformance(final String whatHurtPerformance) {
        this.whatHurtPerformance = whatHurtPerformance;
    }

    @java.lang.SuppressWarnings("all")
    public void setBiggestMistake(final String biggestMistake) {
        this.biggestMistake = biggestMistake;
    }

    @java.lang.SuppressWarnings("all")
    public void setBestSetup(final String bestSetup) {
        this.bestSetup = bestSetup;
    }

    @java.lang.SuppressWarnings("all")
    public void setWorstDecision(final String worstDecision) {
        this.worstDecision = worstDecision;
    }

    @java.lang.SuppressWarnings("all")
    public void setEmotionalPattern(final String emotionalPattern) {
        this.emotionalPattern = emotionalPattern;
    }

    @java.lang.SuppressWarnings("all")
    public void setExecutionPattern(final String executionPattern) {
        this.executionPattern = executionPattern;
    }

    @java.lang.SuppressWarnings("all")
    public void setRiskManagementPattern(final String riskManagementPattern) {
        this.riskManagementPattern = riskManagementPattern;
    }

    @java.lang.SuppressWarnings("all")
    public void setOneRuleForTomorrow(final String oneRuleForTomorrow) {
        this.oneRuleForTomorrow = oneRuleForTomorrow;
    }

    @java.lang.SuppressWarnings("all")
    public void setGeneratedAt(final LocalDateTime generatedAt) {
        this.generatedAt = generatedAt;
    }

    @java.lang.Override
    @java.lang.SuppressWarnings("all")
    public boolean equals(final java.lang.Object o) {
        if (o == this) return true;
        if (!(o instanceof AiSessionReviewResponse)) return false;
        final AiSessionReviewResponse other = (AiSessionReviewResponse) o;
        if (!other.canEqual((java.lang.Object) this)) return false;
        final java.lang.Object this$id = this.getId();
        final java.lang.Object other$id = other.getId();
        if (this$id == null ? other$id != null : !this$id.equals(other$id)) return false;
        final java.lang.Object this$summary = this.getSummary();
        final java.lang.Object other$summary = other.getSummary();
        if (this$summary == null ? other$summary != null : !this$summary.equals(other$summary)) return false;
        final java.lang.Object this$whatWentWell = this.getWhatWentWell();
        final java.lang.Object other$whatWentWell = other.getWhatWentWell();
        if (this$whatWentWell == null ? other$whatWentWell != null : !this$whatWentWell.equals(other$whatWentWell)) return false;
        final java.lang.Object this$whatHurtPerformance = this.getWhatHurtPerformance();
        final java.lang.Object other$whatHurtPerformance = other.getWhatHurtPerformance();
        if (this$whatHurtPerformance == null ? other$whatHurtPerformance != null : !this$whatHurtPerformance.equals(other$whatHurtPerformance)) return false;
        final java.lang.Object this$biggestMistake = this.getBiggestMistake();
        final java.lang.Object other$biggestMistake = other.getBiggestMistake();
        if (this$biggestMistake == null ? other$biggestMistake != null : !this$biggestMistake.equals(other$biggestMistake)) return false;
        final java.lang.Object this$bestSetup = this.getBestSetup();
        final java.lang.Object other$bestSetup = other.getBestSetup();
        if (this$bestSetup == null ? other$bestSetup != null : !this$bestSetup.equals(other$bestSetup)) return false;
        final java.lang.Object this$worstDecision = this.getWorstDecision();
        final java.lang.Object other$worstDecision = other.getWorstDecision();
        if (this$worstDecision == null ? other$worstDecision != null : !this$worstDecision.equals(other$worstDecision)) return false;
        final java.lang.Object this$emotionalPattern = this.getEmotionalPattern();
        final java.lang.Object other$emotionalPattern = other.getEmotionalPattern();
        if (this$emotionalPattern == null ? other$emotionalPattern != null : !this$emotionalPattern.equals(other$emotionalPattern)) return false;
        final java.lang.Object this$executionPattern = this.getExecutionPattern();
        final java.lang.Object other$executionPattern = other.getExecutionPattern();
        if (this$executionPattern == null ? other$executionPattern != null : !this$executionPattern.equals(other$executionPattern)) return false;
        final java.lang.Object this$riskManagementPattern = this.getRiskManagementPattern();
        final java.lang.Object other$riskManagementPattern = other.getRiskManagementPattern();
        if (this$riskManagementPattern == null ? other$riskManagementPattern != null : !this$riskManagementPattern.equals(other$riskManagementPattern)) return false;
        final java.lang.Object this$oneRuleForTomorrow = this.getOneRuleForTomorrow();
        final java.lang.Object other$oneRuleForTomorrow = other.getOneRuleForTomorrow();
        if (this$oneRuleForTomorrow == null ? other$oneRuleForTomorrow != null : !this$oneRuleForTomorrow.equals(other$oneRuleForTomorrow)) return false;
        final java.lang.Object this$generatedAt = this.getGeneratedAt();
        final java.lang.Object other$generatedAt = other.getGeneratedAt();
        if (this$generatedAt == null ? other$generatedAt != null : !this$generatedAt.equals(other$generatedAt)) return false;
        return true;
    }

    @java.lang.SuppressWarnings("all")
    protected boolean canEqual(final java.lang.Object other) {
        return other instanceof AiSessionReviewResponse;
    }

    @java.lang.Override
    @java.lang.SuppressWarnings("all")
    public int hashCode() {
        final int PRIME = 59;
        int result = 1;
        final java.lang.Object $id = this.getId();
        result = result * PRIME + ($id == null ? 43 : $id.hashCode());
        final java.lang.Object $summary = this.getSummary();
        result = result * PRIME + ($summary == null ? 43 : $summary.hashCode());
        final java.lang.Object $whatWentWell = this.getWhatWentWell();
        result = result * PRIME + ($whatWentWell == null ? 43 : $whatWentWell.hashCode());
        final java.lang.Object $whatHurtPerformance = this.getWhatHurtPerformance();
        result = result * PRIME + ($whatHurtPerformance == null ? 43 : $whatHurtPerformance.hashCode());
        final java.lang.Object $biggestMistake = this.getBiggestMistake();
        result = result * PRIME + ($biggestMistake == null ? 43 : $biggestMistake.hashCode());
        final java.lang.Object $bestSetup = this.getBestSetup();
        result = result * PRIME + ($bestSetup == null ? 43 : $bestSetup.hashCode());
        final java.lang.Object $worstDecision = this.getWorstDecision();
        result = result * PRIME + ($worstDecision == null ? 43 : $worstDecision.hashCode());
        final java.lang.Object $emotionalPattern = this.getEmotionalPattern();
        result = result * PRIME + ($emotionalPattern == null ? 43 : $emotionalPattern.hashCode());
        final java.lang.Object $executionPattern = this.getExecutionPattern();
        result = result * PRIME + ($executionPattern == null ? 43 : $executionPattern.hashCode());
        final java.lang.Object $riskManagementPattern = this.getRiskManagementPattern();
        result = result * PRIME + ($riskManagementPattern == null ? 43 : $riskManagementPattern.hashCode());
        final java.lang.Object $oneRuleForTomorrow = this.getOneRuleForTomorrow();
        result = result * PRIME + ($oneRuleForTomorrow == null ? 43 : $oneRuleForTomorrow.hashCode());
        final java.lang.Object $generatedAt = this.getGeneratedAt();
        result = result * PRIME + ($generatedAt == null ? 43 : $generatedAt.hashCode());
        return result;
    }

    @java.lang.Override
    @java.lang.SuppressWarnings("all")
    public java.lang.String toString() {
        return "AiSessionReviewResponse(id=" + this.getId() + ", summary=" + this.getSummary() + ", whatWentWell=" + this.getWhatWentWell() + ", whatHurtPerformance=" + this.getWhatHurtPerformance() + ", biggestMistake=" + this.getBiggestMistake() + ", bestSetup=" + this.getBestSetup() + ", worstDecision=" + this.getWorstDecision() + ", emotionalPattern=" + this.getEmotionalPattern() + ", executionPattern=" + this.getExecutionPattern() + ", riskManagementPattern=" + this.getRiskManagementPattern() + ", oneRuleForTomorrow=" + this.getOneRuleForTomorrow() + ", generatedAt=" + this.getGeneratedAt() + ")";
    }
}
