package com.tradepilot.tradingdna.dto;

import java.time.LocalDateTime;
import java.util.List;

public class TradingDnaResponse {
    private Long id;
    private Long accountId;
    private Integer days;
    private Boolean includeAllAccounts;
    private String status;
    private Boolean cached;
    private Boolean limitReached;
    private Boolean aiGenerated;
    private Boolean noTrades;
    private Boolean limitedData;
    private Integer creditsConsumed;
    private String message;
    private Integer totalTrades;
    private LocalDateTime lastGeneratedAt;
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
    private List<TradingDnaTimelinePoint> timeline;

    @java.lang.SuppressWarnings("all")
    public TradingDnaResponse() {
    }

    @java.lang.SuppressWarnings("all")
    public Long getId() {
        return this.id;
    }

    @java.lang.SuppressWarnings("all")
    public Long getAccountId() {
        return this.accountId;
    }

    @java.lang.SuppressWarnings("all")
    public Integer getDays() {
        return this.days;
    }

    @java.lang.SuppressWarnings("all")
    public Boolean getIncludeAllAccounts() {
        return this.includeAllAccounts;
    }

    @java.lang.SuppressWarnings("all")
    public String getStatus() {
        return this.status;
    }

    @java.lang.SuppressWarnings("all")
    public Boolean getCached() {
        return this.cached;
    }

    @java.lang.SuppressWarnings("all")
    public Boolean getLimitReached() {
        return this.limitReached;
    }

    @java.lang.SuppressWarnings("all")
    public Boolean getAiGenerated() {
        return this.aiGenerated;
    }

    @java.lang.SuppressWarnings("all")
    public Boolean getNoTrades() {
        return this.noTrades;
    }

    @java.lang.SuppressWarnings("all")
    public Boolean getLimitedData() {
        return this.limitedData;
    }

    @java.lang.SuppressWarnings("all")
    public Integer getCreditsConsumed() {
        return this.creditsConsumed;
    }

    @java.lang.SuppressWarnings("all")
    public String getMessage() {
        return this.message;
    }

    @java.lang.SuppressWarnings("all")
    public Integer getTotalTrades() {
        return this.totalTrades;
    }

    @java.lang.SuppressWarnings("all")
    public LocalDateTime getLastGeneratedAt() {
        return this.lastGeneratedAt;
    }

    @java.lang.SuppressWarnings("all")
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
    public List<TradingDnaTimelinePoint> getTimeline() {
        return this.timeline;
    }

    @java.lang.SuppressWarnings("all")
    public void setId(final Long id) {
        this.id = id;
    }

    @java.lang.SuppressWarnings("all")
    public void setAccountId(final Long accountId) {
        this.accountId = accountId;
    }

    @java.lang.SuppressWarnings("all")
    public void setDays(final Integer days) {
        this.days = days;
    }

    @java.lang.SuppressWarnings("all")
    public void setIncludeAllAccounts(final Boolean includeAllAccounts) {
        this.includeAllAccounts = includeAllAccounts;
    }

    @java.lang.SuppressWarnings("all")
    public void setStatus(final String status) {
        this.status = status;
    }

    @java.lang.SuppressWarnings("all")
    public void setCached(final Boolean cached) {
        this.cached = cached;
    }

    @java.lang.SuppressWarnings("all")
    public void setLimitReached(final Boolean limitReached) {
        this.limitReached = limitReached;
    }

    @java.lang.SuppressWarnings("all")
    public void setAiGenerated(final Boolean aiGenerated) {
        this.aiGenerated = aiGenerated;
    }

    @java.lang.SuppressWarnings("all")
    public void setNoTrades(final Boolean noTrades) {
        this.noTrades = noTrades;
    }

    @java.lang.SuppressWarnings("all")
    public void setLimitedData(final Boolean limitedData) {
        this.limitedData = limitedData;
    }

    @java.lang.SuppressWarnings("all")
    public void setCreditsConsumed(final Integer creditsConsumed) {
        this.creditsConsumed = creditsConsumed;
    }

    @java.lang.SuppressWarnings("all")
    public void setMessage(final String message) {
        this.message = message;
    }

    @java.lang.SuppressWarnings("all")
    public void setTotalTrades(final Integer totalTrades) {
        this.totalTrades = totalTrades;
    }

    @java.lang.SuppressWarnings("all")
    public void setLastGeneratedAt(final LocalDateTime lastGeneratedAt) {
        this.lastGeneratedAt = lastGeneratedAt;
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

    @java.lang.SuppressWarnings("all")
    public void setTimeline(final List<TradingDnaTimelinePoint> timeline) {
        this.timeline = timeline;
    }

    @java.lang.Override
    @java.lang.SuppressWarnings("all")
    public boolean equals(final java.lang.Object o) {
        if (o == this) return true;
        if (!(o instanceof TradingDnaResponse)) return false;
        final TradingDnaResponse other = (TradingDnaResponse) o;
        if (!other.canEqual((java.lang.Object) this)) return false;
        final java.lang.Object this$id = this.getId();
        final java.lang.Object other$id = other.getId();
        if (this$id == null ? other$id != null : !this$id.equals(other$id)) return false;
        final java.lang.Object this$accountId = this.getAccountId();
        final java.lang.Object other$accountId = other.getAccountId();
        if (this$accountId == null ? other$accountId != null : !this$accountId.equals(other$accountId)) return false;
        final java.lang.Object this$days = this.getDays();
        final java.lang.Object other$days = other.getDays();
        if (this$days == null ? other$days != null : !this$days.equals(other$days)) return false;
        final java.lang.Object this$includeAllAccounts = this.getIncludeAllAccounts();
        final java.lang.Object other$includeAllAccounts = other.getIncludeAllAccounts();
        if (this$includeAllAccounts == null ? other$includeAllAccounts != null : !this$includeAllAccounts.equals(other$includeAllAccounts)) return false;
        final java.lang.Object this$cached = this.getCached();
        final java.lang.Object other$cached = other.getCached();
        if (this$cached == null ? other$cached != null : !this$cached.equals(other$cached)) return false;
        final java.lang.Object this$limitReached = this.getLimitReached();
        final java.lang.Object other$limitReached = other.getLimitReached();
        if (this$limitReached == null ? other$limitReached != null : !this$limitReached.equals(other$limitReached)) return false;
        final java.lang.Object this$aiGenerated = this.getAiGenerated();
        final java.lang.Object other$aiGenerated = other.getAiGenerated();
        if (this$aiGenerated == null ? other$aiGenerated != null : !this$aiGenerated.equals(other$aiGenerated)) return false;
        final java.lang.Object this$noTrades = this.getNoTrades();
        final java.lang.Object other$noTrades = other.getNoTrades();
        if (this$noTrades == null ? other$noTrades != null : !this$noTrades.equals(other$noTrades)) return false;
        final java.lang.Object this$limitedData = this.getLimitedData();
        final java.lang.Object other$limitedData = other.getLimitedData();
        if (this$limitedData == null ? other$limitedData != null : !this$limitedData.equals(other$limitedData)) return false;
        final java.lang.Object this$creditsConsumed = this.getCreditsConsumed();
        final java.lang.Object other$creditsConsumed = other.getCreditsConsumed();
        if (this$creditsConsumed == null ? other$creditsConsumed != null : !this$creditsConsumed.equals(other$creditsConsumed)) return false;
        final java.lang.Object this$totalTrades = this.getTotalTrades();
        final java.lang.Object other$totalTrades = other.getTotalTrades();
        if (this$totalTrades == null ? other$totalTrades != null : !this$totalTrades.equals(other$totalTrades)) return false;
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
        final java.lang.Object this$status = this.getStatus();
        final java.lang.Object other$status = other.getStatus();
        if (this$status == null ? other$status != null : !this$status.equals(other$status)) return false;
        final java.lang.Object this$message = this.getMessage();
        final java.lang.Object other$message = other.getMessage();
        if (this$message == null ? other$message != null : !this$message.equals(other$message)) return false;
        final java.lang.Object this$lastGeneratedAt = this.getLastGeneratedAt();
        final java.lang.Object other$lastGeneratedAt = other.getLastGeneratedAt();
        if (this$lastGeneratedAt == null ? other$lastGeneratedAt != null : !this$lastGeneratedAt.equals(other$lastGeneratedAt)) return false;
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
        final java.lang.Object this$timeline = this.getTimeline();
        final java.lang.Object other$timeline = other.getTimeline();
        if (this$timeline == null ? other$timeline != null : !this$timeline.equals(other$timeline)) return false;
        return true;
    }

    @java.lang.SuppressWarnings("all")
    protected boolean canEqual(final java.lang.Object other) {
        return other instanceof TradingDnaResponse;
    }

    @java.lang.Override
    @java.lang.SuppressWarnings("all")
    public int hashCode() {
        final int PRIME = 59;
        int result = 1;
        final java.lang.Object $id = this.getId();
        result = result * PRIME + ($id == null ? 43 : $id.hashCode());
        final java.lang.Object $accountId = this.getAccountId();
        result = result * PRIME + ($accountId == null ? 43 : $accountId.hashCode());
        final java.lang.Object $days = this.getDays();
        result = result * PRIME + ($days == null ? 43 : $days.hashCode());
        final java.lang.Object $includeAllAccounts = this.getIncludeAllAccounts();
        result = result * PRIME + ($includeAllAccounts == null ? 43 : $includeAllAccounts.hashCode());
        final java.lang.Object $cached = this.getCached();
        result = result * PRIME + ($cached == null ? 43 : $cached.hashCode());
        final java.lang.Object $limitReached = this.getLimitReached();
        result = result * PRIME + ($limitReached == null ? 43 : $limitReached.hashCode());
        final java.lang.Object $aiGenerated = this.getAiGenerated();
        result = result * PRIME + ($aiGenerated == null ? 43 : $aiGenerated.hashCode());
        final java.lang.Object $noTrades = this.getNoTrades();
        result = result * PRIME + ($noTrades == null ? 43 : $noTrades.hashCode());
        final java.lang.Object $limitedData = this.getLimitedData();
        result = result * PRIME + ($limitedData == null ? 43 : $limitedData.hashCode());
        final java.lang.Object $creditsConsumed = this.getCreditsConsumed();
        result = result * PRIME + ($creditsConsumed == null ? 43 : $creditsConsumed.hashCode());
        final java.lang.Object $totalTrades = this.getTotalTrades();
        result = result * PRIME + ($totalTrades == null ? 43 : $totalTrades.hashCode());
        final java.lang.Object $consistencyScore = this.getConsistencyScore();
        result = result * PRIME + ($consistencyScore == null ? 43 : $consistencyScore.hashCode());
        final java.lang.Object $disciplineScore = this.getDisciplineScore();
        result = result * PRIME + ($disciplineScore == null ? 43 : $disciplineScore.hashCode());
        final java.lang.Object $executionScore = this.getExecutionScore();
        result = result * PRIME + ($executionScore == null ? 43 : $executionScore.hashCode());
        final java.lang.Object $patienceScore = this.getPatienceScore();
        result = result * PRIME + ($patienceScore == null ? 43 : $patienceScore.hashCode());
        final java.lang.Object $status = this.getStatus();
        result = result * PRIME + ($status == null ? 43 : $status.hashCode());
        final java.lang.Object $message = this.getMessage();
        result = result * PRIME + ($message == null ? 43 : $message.hashCode());
        final java.lang.Object $lastGeneratedAt = this.getLastGeneratedAt();
        result = result * PRIME + ($lastGeneratedAt == null ? 43 : $lastGeneratedAt.hashCode());
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
        final java.lang.Object $timeline = this.getTimeline();
        result = result * PRIME + ($timeline == null ? 43 : $timeline.hashCode());
        return result;
    }

    @java.lang.Override
    @java.lang.SuppressWarnings("all")
    public java.lang.String toString() {
        return "TradingDnaResponse(id=" + this.getId() + ", accountId=" + this.getAccountId() + ", days=" + this.getDays() + ", includeAllAccounts=" + this.getIncludeAllAccounts() + ", status=" + this.getStatus() + ", cached=" + this.getCached() + ", limitReached=" + this.getLimitReached() + ", aiGenerated=" + this.getAiGenerated() + ", noTrades=" + this.getNoTrades() + ", limitedData=" + this.getLimitedData() + ", creditsConsumed=" + this.getCreditsConsumed() + ", message=" + this.getMessage() + ", totalTrades=" + this.getTotalTrades() + ", lastGeneratedAt=" + this.getLastGeneratedAt() + ", strengths=" + this.getStrengths() + ", weaknesses=" + this.getWeaknesses() + ", emotionalPatterns=" + this.getEmotionalPatterns() + ", riskPatterns=" + this.getRiskPatterns() + ", bestConditions=" + this.getBestConditions() + ", worstConditions=" + this.getWorstConditions() + ", recurringMistakes=" + this.getRecurringMistakes() + ", improvementTrend=" + this.getImprovementTrend() + ", consistencyScore=" + this.getConsistencyScore() + ", disciplineScore=" + this.getDisciplineScore() + ", executionScore=" + this.getExecutionScore() + ", patienceScore=" + this.getPatienceScore() + ", confidenceSummary=" + this.getConfidenceSummary() + ", coachingSummary=" + this.getCoachingSummary() + ", timeline=" + this.getTimeline() + ")";
    }
}
