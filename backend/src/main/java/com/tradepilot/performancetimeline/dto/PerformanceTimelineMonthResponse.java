package com.tradepilot.performancetimeline.dto;

import com.tradepilot.performancetimeline.entity.ScoreConfidence;
import com.tradepilot.performancetimeline.entity.TimelineDataStatus;

import java.time.LocalDate;
import java.util.List;

public class PerformanceTimelineMonthResponse {
    private String month;
    private int year;
    private LocalDate periodStart;
    private LocalDate periodEnd;
    private TimelineDataStatus status;
    private int tradeCount;
    private PerformanceTimelineMetrics metrics;
    private List<PerformanceMetricComparison> comparison;
    private PerformanceCategoryScores categoryScores;
    private Integer evolutionScore;
    private Integer scoreChange;
    private ScoreConfidence scoreConfidence;
    private PerformanceTimelineNarrativeResponse narrative;
    private List<PerformanceMilestoneResponse> milestones;
    private String dataHash;
    private String message;

    public PerformanceTimelineMonthResponse() {
    }

    public String getMonth() { return month; }
    public void setMonth(String month) { this.month = month; }
    public int getYear() { return year; }
    public void setYear(int year) { this.year = year; }
    public LocalDate getPeriodStart() { return periodStart; }
    public void setPeriodStart(LocalDate periodStart) { this.periodStart = periodStart; }
    public LocalDate getPeriodEnd() { return periodEnd; }
    public void setPeriodEnd(LocalDate periodEnd) { this.periodEnd = periodEnd; }
    public TimelineDataStatus getStatus() { return status; }
    public void setStatus(TimelineDataStatus status) { this.status = status; }
    public int getTradeCount() { return tradeCount; }
    public void setTradeCount(int tradeCount) { this.tradeCount = tradeCount; }
    public PerformanceTimelineMetrics getMetrics() { return metrics; }
    public void setMetrics(PerformanceTimelineMetrics metrics) { this.metrics = metrics; }
    public List<PerformanceMetricComparison> getComparison() { return comparison; }
    public void setComparison(List<PerformanceMetricComparison> comparison) { this.comparison = comparison; }
    public PerformanceCategoryScores getCategoryScores() { return categoryScores; }
    public void setCategoryScores(PerformanceCategoryScores categoryScores) { this.categoryScores = categoryScores; }
    public Integer getEvolutionScore() { return evolutionScore; }
    public void setEvolutionScore(Integer evolutionScore) { this.evolutionScore = evolutionScore; }
    public Integer getScoreChange() { return scoreChange; }
    public void setScoreChange(Integer scoreChange) { this.scoreChange = scoreChange; }
    public ScoreConfidence getScoreConfidence() { return scoreConfidence; }
    public void setScoreConfidence(ScoreConfidence scoreConfidence) { this.scoreConfidence = scoreConfidence; }
    public PerformanceTimelineNarrativeResponse getNarrative() { return narrative; }
    public void setNarrative(PerformanceTimelineNarrativeResponse narrative) { this.narrative = narrative; }
    public List<PerformanceMilestoneResponse> getMilestones() { return milestones; }
    public void setMilestones(List<PerformanceMilestoneResponse> milestones) { this.milestones = milestones; }
    public String getDataHash() { return dataHash; }
    public void setDataHash(String dataHash) { this.dataHash = dataHash; }
    public String getMessage() { return message; }
    public void setMessage(String message) { this.message = message; }
}
