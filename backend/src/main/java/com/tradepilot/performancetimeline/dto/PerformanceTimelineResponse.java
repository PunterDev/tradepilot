package com.tradepilot.performancetimeline.dto;

import com.tradepilot.performancetimeline.entity.PerformanceTrend;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

public class PerformanceTimelineResponse {
    private Long accountId;
    private LocalDateTime generatedAt;
    private LocalDate fromDate;
    private LocalDate toDate;
    private PerformanceTrend overallTrend;
    private Integer currentEvolutionScore;
    private Integer previousEvolutionScore;
    private Integer scoreChange;
    private Integer highestEvolutionScore;
    private String strongestCategory;
    private String weakestCategory;
    private String latestFocus;
    private List<PerformanceTimelineMonthResponse> months;
    private List<PerformanceMilestoneResponse> milestones;
    private int creditCost;
    private boolean cached;

    public PerformanceTimelineResponse() {
    }

    public Long getAccountId() { return accountId; }
    public void setAccountId(Long accountId) { this.accountId = accountId; }
    public LocalDateTime getGeneratedAt() { return generatedAt; }
    public void setGeneratedAt(LocalDateTime generatedAt) { this.generatedAt = generatedAt; }
    public LocalDate getFromDate() { return fromDate; }
    public void setFromDate(LocalDate fromDate) { this.fromDate = fromDate; }
    public LocalDate getToDate() { return toDate; }
    public void setToDate(LocalDate toDate) { this.toDate = toDate; }
    public PerformanceTrend getOverallTrend() { return overallTrend; }
    public void setOverallTrend(PerformanceTrend overallTrend) { this.overallTrend = overallTrend; }
    public Integer getCurrentEvolutionScore() { return currentEvolutionScore; }
    public void setCurrentEvolutionScore(Integer currentEvolutionScore) { this.currentEvolutionScore = currentEvolutionScore; }
    public Integer getPreviousEvolutionScore() { return previousEvolutionScore; }
    public void setPreviousEvolutionScore(Integer previousEvolutionScore) { this.previousEvolutionScore = previousEvolutionScore; }
    public Integer getScoreChange() { return scoreChange; }
    public void setScoreChange(Integer scoreChange) { this.scoreChange = scoreChange; }
    public Integer getHighestEvolutionScore() { return highestEvolutionScore; }
    public void setHighestEvolutionScore(Integer highestEvolutionScore) { this.highestEvolutionScore = highestEvolutionScore; }
    public String getStrongestCategory() { return strongestCategory; }
    public void setStrongestCategory(String strongestCategory) { this.strongestCategory = strongestCategory; }
    public String getWeakestCategory() { return weakestCategory; }
    public void setWeakestCategory(String weakestCategory) { this.weakestCategory = weakestCategory; }
    public String getLatestFocus() { return latestFocus; }
    public void setLatestFocus(String latestFocus) { this.latestFocus = latestFocus; }
    public List<PerformanceTimelineMonthResponse> getMonths() { return months; }
    public void setMonths(List<PerformanceTimelineMonthResponse> months) { this.months = months; }
    public List<PerformanceMilestoneResponse> getMilestones() { return milestones; }
    public void setMilestones(List<PerformanceMilestoneResponse> milestones) { this.milestones = milestones; }
    public int getCreditCost() { return creditCost; }
    public void setCreditCost(int creditCost) { this.creditCost = creditCost; }
    public boolean isCached() { return cached; }
    public void setCached(boolean cached) { this.cached = cached; }
}
