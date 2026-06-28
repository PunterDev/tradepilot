package com.tradepilot.dto.ai;

import java.util.List;

public class AiChartReviewResponse {
    private String summary;
    private String bias;
    private String entryQuality;
    private String stopLossQuality;
    private String takeProfitQuality;
    private String riskManagement;
    private String grade;
    private List<String> mistakes;
    private List<String> strengths;
    private List<String> improvementPlan;
    private List<String> tags;

    @java.lang.SuppressWarnings("all")
    public AiChartReviewResponse() {
    }

    @java.lang.SuppressWarnings("all")
    public String getSummary() {
        return this.summary;
    }

    @java.lang.SuppressWarnings("all")
    public String getBias() {
        return this.bias;
    }

    @java.lang.SuppressWarnings("all")
    public String getEntryQuality() {
        return this.entryQuality;
    }

    @java.lang.SuppressWarnings("all")
    public String getStopLossQuality() {
        return this.stopLossQuality;
    }

    @java.lang.SuppressWarnings("all")
    public String getTakeProfitQuality() {
        return this.takeProfitQuality;
    }

    @java.lang.SuppressWarnings("all")
    public String getRiskManagement() {
        return this.riskManagement;
    }

    @java.lang.SuppressWarnings("all")
    public String getGrade() {
        return this.grade;
    }

    @java.lang.SuppressWarnings("all")
    public List<String> getMistakes() {
        return this.mistakes;
    }

    @java.lang.SuppressWarnings("all")
    public List<String> getStrengths() {
        return this.strengths;
    }

    @java.lang.SuppressWarnings("all")
    public List<String> getImprovementPlan() {
        return this.improvementPlan;
    }

    @java.lang.SuppressWarnings("all")
    public List<String> getTags() {
        return this.tags;
    }

    @java.lang.SuppressWarnings("all")
    public void setSummary(final String summary) {
        this.summary = summary;
    }

    @java.lang.SuppressWarnings("all")
    public void setBias(final String bias) {
        this.bias = bias;
    }

    @java.lang.SuppressWarnings("all")
    public void setEntryQuality(final String entryQuality) {
        this.entryQuality = entryQuality;
    }

    @java.lang.SuppressWarnings("all")
    public void setStopLossQuality(final String stopLossQuality) {
        this.stopLossQuality = stopLossQuality;
    }

    @java.lang.SuppressWarnings("all")
    public void setTakeProfitQuality(final String takeProfitQuality) {
        this.takeProfitQuality = takeProfitQuality;
    }

    @java.lang.SuppressWarnings("all")
    public void setRiskManagement(final String riskManagement) {
        this.riskManagement = riskManagement;
    }

    @java.lang.SuppressWarnings("all")
    public void setGrade(final String grade) {
        this.grade = grade;
    }

    @java.lang.SuppressWarnings("all")
    public void setMistakes(final List<String> mistakes) {
        this.mistakes = mistakes;
    }

    @java.lang.SuppressWarnings("all")
    public void setStrengths(final List<String> strengths) {
        this.strengths = strengths;
    }

    @java.lang.SuppressWarnings("all")
    public void setImprovementPlan(final List<String> improvementPlan) {
        this.improvementPlan = improvementPlan;
    }

    @java.lang.SuppressWarnings("all")
    public void setTags(final List<String> tags) {
        this.tags = tags;
    }

    @java.lang.Override
    @java.lang.SuppressWarnings("all")
    public boolean equals(final java.lang.Object o) {
        if (o == this) return true;
        if (!(o instanceof AiChartReviewResponse)) return false;
        final AiChartReviewResponse other = (AiChartReviewResponse) o;
        if (!other.canEqual((java.lang.Object) this)) return false;
        final java.lang.Object this$summary = this.getSummary();
        final java.lang.Object other$summary = other.getSummary();
        if (this$summary == null ? other$summary != null : !this$summary.equals(other$summary)) return false;
        final java.lang.Object this$bias = this.getBias();
        final java.lang.Object other$bias = other.getBias();
        if (this$bias == null ? other$bias != null : !this$bias.equals(other$bias)) return false;
        final java.lang.Object this$entryQuality = this.getEntryQuality();
        final java.lang.Object other$entryQuality = other.getEntryQuality();
        if (this$entryQuality == null ? other$entryQuality != null : !this$entryQuality.equals(other$entryQuality)) return false;
        final java.lang.Object this$stopLossQuality = this.getStopLossQuality();
        final java.lang.Object other$stopLossQuality = other.getStopLossQuality();
        if (this$stopLossQuality == null ? other$stopLossQuality != null : !this$stopLossQuality.equals(other$stopLossQuality)) return false;
        final java.lang.Object this$takeProfitQuality = this.getTakeProfitQuality();
        final java.lang.Object other$takeProfitQuality = other.getTakeProfitQuality();
        if (this$takeProfitQuality == null ? other$takeProfitQuality != null : !this$takeProfitQuality.equals(other$takeProfitQuality)) return false;
        final java.lang.Object this$riskManagement = this.getRiskManagement();
        final java.lang.Object other$riskManagement = other.getRiskManagement();
        if (this$riskManagement == null ? other$riskManagement != null : !this$riskManagement.equals(other$riskManagement)) return false;
        final java.lang.Object this$grade = this.getGrade();
        final java.lang.Object other$grade = other.getGrade();
        if (this$grade == null ? other$grade != null : !this$grade.equals(other$grade)) return false;
        final java.lang.Object this$mistakes = this.getMistakes();
        final java.lang.Object other$mistakes = other.getMistakes();
        if (this$mistakes == null ? other$mistakes != null : !this$mistakes.equals(other$mistakes)) return false;
        final java.lang.Object this$strengths = this.getStrengths();
        final java.lang.Object other$strengths = other.getStrengths();
        if (this$strengths == null ? other$strengths != null : !this$strengths.equals(other$strengths)) return false;
        final java.lang.Object this$improvementPlan = this.getImprovementPlan();
        final java.lang.Object other$improvementPlan = other.getImprovementPlan();
        if (this$improvementPlan == null ? other$improvementPlan != null : !this$improvementPlan.equals(other$improvementPlan)) return false;
        final java.lang.Object this$tags = this.getTags();
        final java.lang.Object other$tags = other.getTags();
        if (this$tags == null ? other$tags != null : !this$tags.equals(other$tags)) return false;
        return true;
    }

    @java.lang.SuppressWarnings("all")
    protected boolean canEqual(final java.lang.Object other) {
        return other instanceof AiChartReviewResponse;
    }

    @java.lang.Override
    @java.lang.SuppressWarnings("all")
    public int hashCode() {
        final int PRIME = 59;
        int result = 1;
        final java.lang.Object $summary = this.getSummary();
        result = result * PRIME + ($summary == null ? 43 : $summary.hashCode());
        final java.lang.Object $bias = this.getBias();
        result = result * PRIME + ($bias == null ? 43 : $bias.hashCode());
        final java.lang.Object $entryQuality = this.getEntryQuality();
        result = result * PRIME + ($entryQuality == null ? 43 : $entryQuality.hashCode());
        final java.lang.Object $stopLossQuality = this.getStopLossQuality();
        result = result * PRIME + ($stopLossQuality == null ? 43 : $stopLossQuality.hashCode());
        final java.lang.Object $takeProfitQuality = this.getTakeProfitQuality();
        result = result * PRIME + ($takeProfitQuality == null ? 43 : $takeProfitQuality.hashCode());
        final java.lang.Object $riskManagement = this.getRiskManagement();
        result = result * PRIME + ($riskManagement == null ? 43 : $riskManagement.hashCode());
        final java.lang.Object $grade = this.getGrade();
        result = result * PRIME + ($grade == null ? 43 : $grade.hashCode());
        final java.lang.Object $mistakes = this.getMistakes();
        result = result * PRIME + ($mistakes == null ? 43 : $mistakes.hashCode());
        final java.lang.Object $strengths = this.getStrengths();
        result = result * PRIME + ($strengths == null ? 43 : $strengths.hashCode());
        final java.lang.Object $improvementPlan = this.getImprovementPlan();
        result = result * PRIME + ($improvementPlan == null ? 43 : $improvementPlan.hashCode());
        final java.lang.Object $tags = this.getTags();
        result = result * PRIME + ($tags == null ? 43 : $tags.hashCode());
        return result;
    }

    @java.lang.Override
    @java.lang.SuppressWarnings("all")
    public java.lang.String toString() {
        return "AiChartReviewResponse(summary=" + this.getSummary() + ", bias=" + this.getBias() + ", entryQuality=" + this.getEntryQuality() + ", stopLossQuality=" + this.getStopLossQuality() + ", takeProfitQuality=" + this.getTakeProfitQuality() + ", riskManagement=" + this.getRiskManagement() + ", grade=" + this.getGrade() + ", mistakes=" + this.getMistakes() + ", strengths=" + this.getStrengths() + ", improvementPlan=" + this.getImprovementPlan() + ", tags=" + this.getTags() + ")";
    }
}
