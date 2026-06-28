package com.tradepilot.dto.ai;

import com.tradepilot.dto.discipline.RuleViolationDto;
import java.util.List;

public class AiTradeAnalysisResponse {
    private Long id;
    private Long tradeId;
    private String status;
    private String reviewVersion;
    private String reviewHash;
    private String summary;
    private String grade;
    private Integer executionScore;
    private Integer disciplineScore;
    private Integer riskScore;
    private String marketContext;
    private String entryAnalysis;
    private String exitAnalysis;
    private String riskAnalysis;
    private String psychologyAnalysis;
    private String chartAnalysis;
    private List<String> mistakes;
    private List<String> strengths;
    private List<String> improvementPlan;
    private List<String> tags;
    private List<RuleViolationDto> violations;
    private Integer currentDisciplineStreak;
    private Boolean cleanTrade;
    private String errorMessage;

    @java.lang.SuppressWarnings("all")
    public AiTradeAnalysisResponse() {
    }

    @java.lang.SuppressWarnings("all")
    public Long getId() {
        return this.id;
    }

    @java.lang.SuppressWarnings("all")
    public Long getTradeId() {
        return this.tradeId;
    }

    @java.lang.SuppressWarnings("all")
    public String getStatus() {
        return this.status;
    }

    @java.lang.SuppressWarnings("all")
    public String getReviewVersion() {
        return this.reviewVersion;
    }

    @java.lang.SuppressWarnings("all")
    public String getReviewHash() {
        return this.reviewHash;
    }

    @java.lang.SuppressWarnings("all")
    public String getSummary() {
        return this.summary;
    }

    @java.lang.SuppressWarnings("all")
    public String getGrade() {
        return this.grade;
    }

    @java.lang.SuppressWarnings("all")
    public Integer getExecutionScore() {
        return this.executionScore;
    }

    @java.lang.SuppressWarnings("all")
    public Integer getDisciplineScore() {
        return this.disciplineScore;
    }

    @java.lang.SuppressWarnings("all")
    public Integer getRiskScore() {
        return this.riskScore;
    }

    @java.lang.SuppressWarnings("all")
    public String getMarketContext() {
        return this.marketContext;
    }

    @java.lang.SuppressWarnings("all")
    public String getEntryAnalysis() {
        return this.entryAnalysis;
    }

    @java.lang.SuppressWarnings("all")
    public String getExitAnalysis() {
        return this.exitAnalysis;
    }

    @java.lang.SuppressWarnings("all")
    public String getRiskAnalysis() {
        return this.riskAnalysis;
    }

    @java.lang.SuppressWarnings("all")
    public String getPsychologyAnalysis() {
        return this.psychologyAnalysis;
    }

    @java.lang.SuppressWarnings("all")
    public String getChartAnalysis() {
        return this.chartAnalysis;
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
    public List<RuleViolationDto> getViolations() {
        return this.violations;
    }

    @java.lang.SuppressWarnings("all")
    public Integer getCurrentDisciplineStreak() {
        return this.currentDisciplineStreak;
    }

    @java.lang.SuppressWarnings("all")
    public Boolean getCleanTrade() {
        return this.cleanTrade;
    }

    @java.lang.SuppressWarnings("all")
    public String getErrorMessage() {
        return this.errorMessage;
    }

    @java.lang.SuppressWarnings("all")
    public void setId(final Long id) {
        this.id = id;
    }

    @java.lang.SuppressWarnings("all")
    public void setTradeId(final Long tradeId) {
        this.tradeId = tradeId;
    }

    @java.lang.SuppressWarnings("all")
    public void setStatus(final String status) {
        this.status = status;
    }

    @java.lang.SuppressWarnings("all")
    public void setReviewVersion(final String reviewVersion) {
        this.reviewVersion = reviewVersion;
    }

    @java.lang.SuppressWarnings("all")
    public void setReviewHash(final String reviewHash) {
        this.reviewHash = reviewHash;
    }

    @java.lang.SuppressWarnings("all")
    public void setSummary(final String summary) {
        this.summary = summary;
    }

    @java.lang.SuppressWarnings("all")
    public void setGrade(final String grade) {
        this.grade = grade;
    }

    @java.lang.SuppressWarnings("all")
    public void setExecutionScore(final Integer executionScore) {
        this.executionScore = executionScore;
    }

    @java.lang.SuppressWarnings("all")
    public void setDisciplineScore(final Integer disciplineScore) {
        this.disciplineScore = disciplineScore;
    }

    @java.lang.SuppressWarnings("all")
    public void setRiskScore(final Integer riskScore) {
        this.riskScore = riskScore;
    }

    @java.lang.SuppressWarnings("all")
    public void setMarketContext(final String marketContext) {
        this.marketContext = marketContext;
    }

    @java.lang.SuppressWarnings("all")
    public void setEntryAnalysis(final String entryAnalysis) {
        this.entryAnalysis = entryAnalysis;
    }

    @java.lang.SuppressWarnings("all")
    public void setExitAnalysis(final String exitAnalysis) {
        this.exitAnalysis = exitAnalysis;
    }

    @java.lang.SuppressWarnings("all")
    public void setRiskAnalysis(final String riskAnalysis) {
        this.riskAnalysis = riskAnalysis;
    }

    @java.lang.SuppressWarnings("all")
    public void setPsychologyAnalysis(final String psychologyAnalysis) {
        this.psychologyAnalysis = psychologyAnalysis;
    }

    @java.lang.SuppressWarnings("all")
    public void setChartAnalysis(final String chartAnalysis) {
        this.chartAnalysis = chartAnalysis;
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

    @java.lang.SuppressWarnings("all")
    public void setViolations(final List<RuleViolationDto> violations) {
        this.violations = violations;
    }

    @java.lang.SuppressWarnings("all")
    public void setCurrentDisciplineStreak(final Integer currentDisciplineStreak) {
        this.currentDisciplineStreak = currentDisciplineStreak;
    }

    @java.lang.SuppressWarnings("all")
    public void setCleanTrade(final Boolean cleanTrade) {
        this.cleanTrade = cleanTrade;
    }

    @java.lang.SuppressWarnings("all")
    public void setErrorMessage(final String errorMessage) {
        this.errorMessage = errorMessage;
    }

    @java.lang.Override
    @java.lang.SuppressWarnings("all")
    public boolean equals(final java.lang.Object o) {
        if (o == this) return true;
        if (!(o instanceof AiTradeAnalysisResponse)) return false;
        final AiTradeAnalysisResponse other = (AiTradeAnalysisResponse) o;
        if (!other.canEqual((java.lang.Object) this)) return false;
        final java.lang.Object this$id = this.getId();
        final java.lang.Object other$id = other.getId();
        if (this$id == null ? other$id != null : !this$id.equals(other$id)) return false;
        final java.lang.Object this$tradeId = this.getTradeId();
        final java.lang.Object other$tradeId = other.getTradeId();
        if (this$tradeId == null ? other$tradeId != null : !this$tradeId.equals(other$tradeId)) return false;
        final java.lang.Object this$executionScore = this.getExecutionScore();
        final java.lang.Object other$executionScore = other.getExecutionScore();
        if (this$executionScore == null ? other$executionScore != null : !this$executionScore.equals(other$executionScore)) return false;
        final java.lang.Object this$disciplineScore = this.getDisciplineScore();
        final java.lang.Object other$disciplineScore = other.getDisciplineScore();
        if (this$disciplineScore == null ? other$disciplineScore != null : !this$disciplineScore.equals(other$disciplineScore)) return false;
        final java.lang.Object this$riskScore = this.getRiskScore();
        final java.lang.Object other$riskScore = other.getRiskScore();
        if (this$riskScore == null ? other$riskScore != null : !this$riskScore.equals(other$riskScore)) return false;
        final java.lang.Object this$currentDisciplineStreak = this.getCurrentDisciplineStreak();
        final java.lang.Object other$currentDisciplineStreak = other.getCurrentDisciplineStreak();
        if (this$currentDisciplineStreak == null ? other$currentDisciplineStreak != null : !this$currentDisciplineStreak.equals(other$currentDisciplineStreak)) return false;
        final java.lang.Object this$cleanTrade = this.getCleanTrade();
        final java.lang.Object other$cleanTrade = other.getCleanTrade();
        if (this$cleanTrade == null ? other$cleanTrade != null : !this$cleanTrade.equals(other$cleanTrade)) return false;
        final java.lang.Object this$status = this.getStatus();
        final java.lang.Object other$status = other.getStatus();
        if (this$status == null ? other$status != null : !this$status.equals(other$status)) return false;
        final java.lang.Object this$reviewVersion = this.getReviewVersion();
        final java.lang.Object other$reviewVersion = other.getReviewVersion();
        if (this$reviewVersion == null ? other$reviewVersion != null : !this$reviewVersion.equals(other$reviewVersion)) return false;
        final java.lang.Object this$reviewHash = this.getReviewHash();
        final java.lang.Object other$reviewHash = other.getReviewHash();
        if (this$reviewHash == null ? other$reviewHash != null : !this$reviewHash.equals(other$reviewHash)) return false;
        final java.lang.Object this$summary = this.getSummary();
        final java.lang.Object other$summary = other.getSummary();
        if (this$summary == null ? other$summary != null : !this$summary.equals(other$summary)) return false;
        final java.lang.Object this$grade = this.getGrade();
        final java.lang.Object other$grade = other.getGrade();
        if (this$grade == null ? other$grade != null : !this$grade.equals(other$grade)) return false;
        final java.lang.Object this$marketContext = this.getMarketContext();
        final java.lang.Object other$marketContext = other.getMarketContext();
        if (this$marketContext == null ? other$marketContext != null : !this$marketContext.equals(other$marketContext)) return false;
        final java.lang.Object this$entryAnalysis = this.getEntryAnalysis();
        final java.lang.Object other$entryAnalysis = other.getEntryAnalysis();
        if (this$entryAnalysis == null ? other$entryAnalysis != null : !this$entryAnalysis.equals(other$entryAnalysis)) return false;
        final java.lang.Object this$exitAnalysis = this.getExitAnalysis();
        final java.lang.Object other$exitAnalysis = other.getExitAnalysis();
        if (this$exitAnalysis == null ? other$exitAnalysis != null : !this$exitAnalysis.equals(other$exitAnalysis)) return false;
        final java.lang.Object this$riskAnalysis = this.getRiskAnalysis();
        final java.lang.Object other$riskAnalysis = other.getRiskAnalysis();
        if (this$riskAnalysis == null ? other$riskAnalysis != null : !this$riskAnalysis.equals(other$riskAnalysis)) return false;
        final java.lang.Object this$psychologyAnalysis = this.getPsychologyAnalysis();
        final java.lang.Object other$psychologyAnalysis = other.getPsychologyAnalysis();
        if (this$psychologyAnalysis == null ? other$psychologyAnalysis != null : !this$psychologyAnalysis.equals(other$psychologyAnalysis)) return false;
        final java.lang.Object this$chartAnalysis = this.getChartAnalysis();
        final java.lang.Object other$chartAnalysis = other.getChartAnalysis();
        if (this$chartAnalysis == null ? other$chartAnalysis != null : !this$chartAnalysis.equals(other$chartAnalysis)) return false;
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
        final java.lang.Object this$violations = this.getViolations();
        final java.lang.Object other$violations = other.getViolations();
        if (this$violations == null ? other$violations != null : !this$violations.equals(other$violations)) return false;
        final java.lang.Object this$errorMessage = this.getErrorMessage();
        final java.lang.Object other$errorMessage = other.getErrorMessage();
        if (this$errorMessage == null ? other$errorMessage != null : !this$errorMessage.equals(other$errorMessage)) return false;
        return true;
    }

    @java.lang.SuppressWarnings("all")
    protected boolean canEqual(final java.lang.Object other) {
        return other instanceof AiTradeAnalysisResponse;
    }

    @java.lang.Override
    @java.lang.SuppressWarnings("all")
    public int hashCode() {
        final int PRIME = 59;
        int result = 1;
        final java.lang.Object $id = this.getId();
        result = result * PRIME + ($id == null ? 43 : $id.hashCode());
        final java.lang.Object $tradeId = this.getTradeId();
        result = result * PRIME + ($tradeId == null ? 43 : $tradeId.hashCode());
        final java.lang.Object $executionScore = this.getExecutionScore();
        result = result * PRIME + ($executionScore == null ? 43 : $executionScore.hashCode());
        final java.lang.Object $disciplineScore = this.getDisciplineScore();
        result = result * PRIME + ($disciplineScore == null ? 43 : $disciplineScore.hashCode());
        final java.lang.Object $riskScore = this.getRiskScore();
        result = result * PRIME + ($riskScore == null ? 43 : $riskScore.hashCode());
        final java.lang.Object $currentDisciplineStreak = this.getCurrentDisciplineStreak();
        result = result * PRIME + ($currentDisciplineStreak == null ? 43 : $currentDisciplineStreak.hashCode());
        final java.lang.Object $cleanTrade = this.getCleanTrade();
        result = result * PRIME + ($cleanTrade == null ? 43 : $cleanTrade.hashCode());
        final java.lang.Object $status = this.getStatus();
        result = result * PRIME + ($status == null ? 43 : $status.hashCode());
        final java.lang.Object $reviewVersion = this.getReviewVersion();
        result = result * PRIME + ($reviewVersion == null ? 43 : $reviewVersion.hashCode());
        final java.lang.Object $reviewHash = this.getReviewHash();
        result = result * PRIME + ($reviewHash == null ? 43 : $reviewHash.hashCode());
        final java.lang.Object $summary = this.getSummary();
        result = result * PRIME + ($summary == null ? 43 : $summary.hashCode());
        final java.lang.Object $grade = this.getGrade();
        result = result * PRIME + ($grade == null ? 43 : $grade.hashCode());
        final java.lang.Object $marketContext = this.getMarketContext();
        result = result * PRIME + ($marketContext == null ? 43 : $marketContext.hashCode());
        final java.lang.Object $entryAnalysis = this.getEntryAnalysis();
        result = result * PRIME + ($entryAnalysis == null ? 43 : $entryAnalysis.hashCode());
        final java.lang.Object $exitAnalysis = this.getExitAnalysis();
        result = result * PRIME + ($exitAnalysis == null ? 43 : $exitAnalysis.hashCode());
        final java.lang.Object $riskAnalysis = this.getRiskAnalysis();
        result = result * PRIME + ($riskAnalysis == null ? 43 : $riskAnalysis.hashCode());
        final java.lang.Object $psychologyAnalysis = this.getPsychologyAnalysis();
        result = result * PRIME + ($psychologyAnalysis == null ? 43 : $psychologyAnalysis.hashCode());
        final java.lang.Object $chartAnalysis = this.getChartAnalysis();
        result = result * PRIME + ($chartAnalysis == null ? 43 : $chartAnalysis.hashCode());
        final java.lang.Object $mistakes = this.getMistakes();
        result = result * PRIME + ($mistakes == null ? 43 : $mistakes.hashCode());
        final java.lang.Object $strengths = this.getStrengths();
        result = result * PRIME + ($strengths == null ? 43 : $strengths.hashCode());
        final java.lang.Object $improvementPlan = this.getImprovementPlan();
        result = result * PRIME + ($improvementPlan == null ? 43 : $improvementPlan.hashCode());
        final java.lang.Object $tags = this.getTags();
        result = result * PRIME + ($tags == null ? 43 : $tags.hashCode());
        final java.lang.Object $violations = this.getViolations();
        result = result * PRIME + ($violations == null ? 43 : $violations.hashCode());
        final java.lang.Object $errorMessage = this.getErrorMessage();
        result = result * PRIME + ($errorMessage == null ? 43 : $errorMessage.hashCode());
        return result;
    }

    @java.lang.Override
    @java.lang.SuppressWarnings("all")
    public java.lang.String toString() {
        return "AiTradeAnalysisResponse(id=" + this.getId() + ", tradeId=" + this.getTradeId() + ", status=" + this.getStatus() + ", reviewVersion=" + this.getReviewVersion() + ", reviewHash=" + this.getReviewHash() + ", summary=" + this.getSummary() + ", grade=" + this.getGrade() + ", executionScore=" + this.getExecutionScore() + ", disciplineScore=" + this.getDisciplineScore() + ", riskScore=" + this.getRiskScore() + ", marketContext=" + this.getMarketContext() + ", entryAnalysis=" + this.getEntryAnalysis() + ", exitAnalysis=" + this.getExitAnalysis() + ", riskAnalysis=" + this.getRiskAnalysis() + ", psychologyAnalysis=" + this.getPsychologyAnalysis() + ", chartAnalysis=" + this.getChartAnalysis() + ", mistakes=" + this.getMistakes() + ", strengths=" + this.getStrengths() + ", improvementPlan=" + this.getImprovementPlan() + ", tags=" + this.getTags() + ", violations=" + this.getViolations() + ", currentDisciplineStreak=" + this.getCurrentDisciplineStreak() + ", cleanTrade=" + this.getCleanTrade() + ", errorMessage=" + this.getErrorMessage() + ")";
    }
}
