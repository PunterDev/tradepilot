package com.tradepilot.tradingdna.dto;

import java.util.List;

public class TradingDnaAiResult {
    private List<String> strengths;
    private List<String> weaknesses;
    private List<String> emotionalPatterns;
    private List<String> riskPatterns;
    private List<String> bestConditions;
    private List<String> worstConditions;
    private List<String> recurringMistakes;
    private String improvementTrend;
    private Integer consistencyScore;
    private Integer disciplineScore;
    private Integer executionScore;
    private Integer patienceScore;
    private String confidenceSummary;
    private String coachingSummary;

    public static TradingDnaAiResult of(
            List<String> strengths,
            List<String> weaknesses,
            List<String> emotionalPatterns,
            List<String> riskPatterns,
            List<String> bestConditions,
            List<String> worstConditions,
            List<String> recurringMistakes,
            String improvementTrend,
            Integer consistencyScore,
            Integer disciplineScore,
            Integer executionScore,
            Integer patienceScore,
            String confidenceSummary,
            String coachingSummary
    ) {
        return new TradingDnaAiResult(
                strengths,
                weaknesses,
                emotionalPatterns,
                riskPatterns,
                bestConditions,
                worstConditions,
                recurringMistakes,
                improvementTrend,
                consistencyScore,
                disciplineScore,
                executionScore,
                patienceScore,
                confidenceSummary,
                coachingSummary
        );
    }

    public List<String> getStrengths() {
        return this.strengths;
    }

    @java.lang.SuppressWarnings("all")
    public List<String> getWeaknesses() {
        return this.weaknesses;
    }

    @java.lang.SuppressWarnings("all")
    public List<String> getEmotionalPatterns() {
        return this.emotionalPatterns;
    }

    @java.lang.SuppressWarnings("all")
    public List<String> getRiskPatterns() {
        return this.riskPatterns;
    }

    @java.lang.SuppressWarnings("all")
    public List<String> getBestConditions() {
        return this.bestConditions;
    }

    @java.lang.SuppressWarnings("all")
    public List<String> getWorstConditions() {
        return this.worstConditions;
    }

    @java.lang.SuppressWarnings("all")
    public List<String> getRecurringMistakes() {
        return this.recurringMistakes;
    }

    @java.lang.SuppressWarnings("all")
    public String getImprovementTrend() {
        return this.improvementTrend;
    }

    @java.lang.SuppressWarnings("all")
    public Integer getConsistencyScore() {
        return this.consistencyScore;
    }

    @java.lang.SuppressWarnings("all")
    public Integer getDisciplineScore() {
        return this.disciplineScore;
    }

    @java.lang.SuppressWarnings("all")
    public Integer getExecutionScore() {
        return this.executionScore;
    }

    @java.lang.SuppressWarnings("all")
    public Integer getPatienceScore() {
        return this.patienceScore;
    }

    @java.lang.SuppressWarnings("all")
    public String getConfidenceSummary() {
        return this.confidenceSummary;
    }

    @java.lang.SuppressWarnings("all")
    public String getCoachingSummary() {
        return this.coachingSummary;
    }

    @java.lang.SuppressWarnings("all")
    public void setStrengths(final List<String> strengths) {
        this.strengths = strengths;
    }

    @java.lang.SuppressWarnings("all")
    public void setWeaknesses(final List<String> weaknesses) {
        this.weaknesses = weaknesses;
    }

    @java.lang.SuppressWarnings("all")
    public void setEmotionalPatterns(final List<String> emotionalPatterns) {
        this.emotionalPatterns = emotionalPatterns;
    }

    @java.lang.SuppressWarnings("all")
    public void setRiskPatterns(final List<String> riskPatterns) {
        this.riskPatterns = riskPatterns;
    }

    @java.lang.SuppressWarnings("all")
    public void setBestConditions(final List<String> bestConditions) {
        this.bestConditions = bestConditions;
    }

    @java.lang.SuppressWarnings("all")
    public void setWorstConditions(final List<String> worstConditions) {
        this.worstConditions = worstConditions;
    }

    @java.lang.SuppressWarnings("all")
    public void setRecurringMistakes(final List<String> recurringMistakes) {
        this.recurringMistakes = recurringMistakes;
    }

    @java.lang.SuppressWarnings("all")
    public void setImprovementTrend(final String improvementTrend) {
        this.improvementTrend = improvementTrend;
    }

    @java.lang.SuppressWarnings("all")
    public void setConsistencyScore(final Integer consistencyScore) {
        this.consistencyScore = consistencyScore;
    }

    @java.lang.SuppressWarnings("all")
    public void setDisciplineScore(final Integer disciplineScore) {
        this.disciplineScore = disciplineScore;
    }

    @java.lang.SuppressWarnings("all")
    public void setExecutionScore(final Integer executionScore) {
        this.executionScore = executionScore;
    }

    @java.lang.SuppressWarnings("all")
    public void setPatienceScore(final Integer patienceScore) {
        this.patienceScore = patienceScore;
    }

    @java.lang.SuppressWarnings("all")
    public void setConfidenceSummary(final String confidenceSummary) {
        this.confidenceSummary = confidenceSummary;
    }

    @java.lang.SuppressWarnings("all")
    public void setCoachingSummary(final String coachingSummary) {
        this.coachingSummary = coachingSummary;
    }

    @java.lang.Override
    @java.lang.SuppressWarnings("all")
    public boolean equals(final java.lang.Object o) {
        if (o == this) return true;
        if (!(o instanceof TradingDnaAiResult)) return false;
        final TradingDnaAiResult other = (TradingDnaAiResult) o;
        if (!other.canEqual((java.lang.Object) this)) return false;
        final java.lang.Object this$consistencyScore = this.getConsistencyScore();
        final java.lang.Object other$consistencyScore = other.getConsistencyScore();
        if (this$consistencyScore == null ? other$consistencyScore != null : !this$consistencyScore.equals(other$consistencyScore)) return false;
        final java.lang.Object this$disciplineScore = this.getDisciplineScore();
        final java.lang.Object other$disciplineScore = other.getDisciplineScore();
        if (this$disciplineScore == null ? other$disciplineScore != null : !this$disciplineScore.equals(other$disciplineScore)) return false;
        final java.lang.Object this$executionScore = this.getExecutionScore();
        final java.lang.Object other$executionScore = other.getExecutionScore();
        if (this$executionScore == null ? other$executionScore != null : !this$executionScore.equals(other$executionScore)) return false;
        final java.lang.Object this$patienceScore = this.getPatienceScore();
        final java.lang.Object other$patienceScore = other.getPatienceScore();
        if (this$patienceScore == null ? other$patienceScore != null : !this$patienceScore.equals(other$patienceScore)) return false;
        final java.lang.Object this$strengths = this.getStrengths();
        final java.lang.Object other$strengths = other.getStrengths();
        if (this$strengths == null ? other$strengths != null : !this$strengths.equals(other$strengths)) return false;
        final java.lang.Object this$weaknesses = this.getWeaknesses();
        final java.lang.Object other$weaknesses = other.getWeaknesses();
        if (this$weaknesses == null ? other$weaknesses != null : !this$weaknesses.equals(other$weaknesses)) return false;
        final java.lang.Object this$emotionalPatterns = this.getEmotionalPatterns();
        final java.lang.Object other$emotionalPatterns = other.getEmotionalPatterns();
        if (this$emotionalPatterns == null ? other$emotionalPatterns != null : !this$emotionalPatterns.equals(other$emotionalPatterns)) return false;
        final java.lang.Object this$riskPatterns = this.getRiskPatterns();
        final java.lang.Object other$riskPatterns = other.getRiskPatterns();
        if (this$riskPatterns == null ? other$riskPatterns != null : !this$riskPatterns.equals(other$riskPatterns)) return false;
        final java.lang.Object this$bestConditions = this.getBestConditions();
        final java.lang.Object other$bestConditions = other.getBestConditions();
        if (this$bestConditions == null ? other$bestConditions != null : !this$bestConditions.equals(other$bestConditions)) return false;
        final java.lang.Object this$worstConditions = this.getWorstConditions();
        final java.lang.Object other$worstConditions = other.getWorstConditions();
        if (this$worstConditions == null ? other$worstConditions != null : !this$worstConditions.equals(other$worstConditions)) return false;
        final java.lang.Object this$recurringMistakes = this.getRecurringMistakes();
        final java.lang.Object other$recurringMistakes = other.getRecurringMistakes();
        if (this$recurringMistakes == null ? other$recurringMistakes != null : !this$recurringMistakes.equals(other$recurringMistakes)) return false;
        final java.lang.Object this$improvementTrend = this.getImprovementTrend();
        final java.lang.Object other$improvementTrend = other.getImprovementTrend();
        if (this$improvementTrend == null ? other$improvementTrend != null : !this$improvementTrend.equals(other$improvementTrend)) return false;
        final java.lang.Object this$confidenceSummary = this.getConfidenceSummary();
        final java.lang.Object other$confidenceSummary = other.getConfidenceSummary();
        if (this$confidenceSummary == null ? other$confidenceSummary != null : !this$confidenceSummary.equals(other$confidenceSummary)) return false;
        final java.lang.Object this$coachingSummary = this.getCoachingSummary();
        final java.lang.Object other$coachingSummary = other.getCoachingSummary();
        if (this$coachingSummary == null ? other$coachingSummary != null : !this$coachingSummary.equals(other$coachingSummary)) return false;
        return true;
    }

    @java.lang.SuppressWarnings("all")
    protected boolean canEqual(final java.lang.Object other) {
        return other instanceof TradingDnaAiResult;
    }

    @java.lang.Override
    @java.lang.SuppressWarnings("all")
    public int hashCode() {
        final int PRIME = 59;
        int result = 1;
        final java.lang.Object $consistencyScore = this.getConsistencyScore();
        result = result * PRIME + ($consistencyScore == null ? 43 : $consistencyScore.hashCode());
        final java.lang.Object $disciplineScore = this.getDisciplineScore();
        result = result * PRIME + ($disciplineScore == null ? 43 : $disciplineScore.hashCode());
        final java.lang.Object $executionScore = this.getExecutionScore();
        result = result * PRIME + ($executionScore == null ? 43 : $executionScore.hashCode());
        final java.lang.Object $patienceScore = this.getPatienceScore();
        result = result * PRIME + ($patienceScore == null ? 43 : $patienceScore.hashCode());
        final java.lang.Object $strengths = this.getStrengths();
        result = result * PRIME + ($strengths == null ? 43 : $strengths.hashCode());
        final java.lang.Object $weaknesses = this.getWeaknesses();
        result = result * PRIME + ($weaknesses == null ? 43 : $weaknesses.hashCode());
        final java.lang.Object $emotionalPatterns = this.getEmotionalPatterns();
        result = result * PRIME + ($emotionalPatterns == null ? 43 : $emotionalPatterns.hashCode());
        final java.lang.Object $riskPatterns = this.getRiskPatterns();
        result = result * PRIME + ($riskPatterns == null ? 43 : $riskPatterns.hashCode());
        final java.lang.Object $bestConditions = this.getBestConditions();
        result = result * PRIME + ($bestConditions == null ? 43 : $bestConditions.hashCode());
        final java.lang.Object $worstConditions = this.getWorstConditions();
        result = result * PRIME + ($worstConditions == null ? 43 : $worstConditions.hashCode());
        final java.lang.Object $recurringMistakes = this.getRecurringMistakes();
        result = result * PRIME + ($recurringMistakes == null ? 43 : $recurringMistakes.hashCode());
        final java.lang.Object $improvementTrend = this.getImprovementTrend();
        result = result * PRIME + ($improvementTrend == null ? 43 : $improvementTrend.hashCode());
        final java.lang.Object $confidenceSummary = this.getConfidenceSummary();
        result = result * PRIME + ($confidenceSummary == null ? 43 : $confidenceSummary.hashCode());
        final java.lang.Object $coachingSummary = this.getCoachingSummary();
        result = result * PRIME + ($coachingSummary == null ? 43 : $coachingSummary.hashCode());
        return result;
    }

    @java.lang.Override
    @java.lang.SuppressWarnings("all")
    public java.lang.String toString() {
        return "TradingDnaAiResult(strengths=" + this.getStrengths() + ", weaknesses=" + this.getWeaknesses() + ", emotionalPatterns=" + this.getEmotionalPatterns() + ", riskPatterns=" + this.getRiskPatterns() + ", bestConditions=" + this.getBestConditions() + ", worstConditions=" + this.getWorstConditions() + ", recurringMistakes=" + this.getRecurringMistakes() + ", improvementTrend=" + this.getImprovementTrend() + ", consistencyScore=" + this.getConsistencyScore() + ", disciplineScore=" + this.getDisciplineScore() + ", executionScore=" + this.getExecutionScore() + ", patienceScore=" + this.getPatienceScore() + ", confidenceSummary=" + this.getConfidenceSummary() + ", coachingSummary=" + this.getCoachingSummary() + ")";
    }

    @java.lang.SuppressWarnings("all")
    public TradingDnaAiResult() {
    }

    @java.lang.SuppressWarnings("all")
    public TradingDnaAiResult(final List<String> strengths, final List<String> weaknesses, final List<String> emotionalPatterns, final List<String> riskPatterns, final List<String> bestConditions, final List<String> worstConditions, final List<String> recurringMistakes, final String improvementTrend, final Integer consistencyScore, final Integer disciplineScore, final Integer executionScore, final Integer patienceScore, final String confidenceSummary, final String coachingSummary) {
        this.strengths = strengths;
        this.weaknesses = weaknesses;
        this.emotionalPatterns = emotionalPatterns;
        this.riskPatterns = riskPatterns;
        this.bestConditions = bestConditions;
        this.worstConditions = worstConditions;
        this.recurringMistakes = recurringMistakes;
        this.improvementTrend = improvementTrend;
        this.consistencyScore = consistencyScore;
        this.disciplineScore = disciplineScore;
        this.executionScore = executionScore;
        this.patienceScore = patienceScore;
        this.confidenceSummary = confidenceSummary;
        this.coachingSummary = coachingSummary;
    }
}
