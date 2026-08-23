package com.tradepilot.performancetimeline.dto;

import com.tradepilot.performancetimeline.entity.MilestoneType;

import java.time.LocalDate;
import java.util.List;

public class PerformanceMilestoneResponse {
    private MilestoneType milestoneType;
    private String title;
    private LocalDate earnedDate;
    private LocalDate periodStart;
    private LocalDate periodEnd;
    private List<String> evidence;

    public PerformanceMilestoneResponse() {
    }

    public PerformanceMilestoneResponse(MilestoneType milestoneType, String title, LocalDate earnedDate, LocalDate periodStart, LocalDate periodEnd, List<String> evidence) {
        this.milestoneType = milestoneType;
        this.title = title;
        this.earnedDate = earnedDate;
        this.periodStart = periodStart;
        this.periodEnd = periodEnd;
        this.evidence = evidence;
    }

    public MilestoneType getMilestoneType() { return milestoneType; }
    public void setMilestoneType(MilestoneType milestoneType) { this.milestoneType = milestoneType; }
    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }
    public LocalDate getEarnedDate() { return earnedDate; }
    public void setEarnedDate(LocalDate earnedDate) { this.earnedDate = earnedDate; }
    public LocalDate getPeriodStart() { return periodStart; }
    public void setPeriodStart(LocalDate periodStart) { this.periodStart = periodStart; }
    public LocalDate getPeriodEnd() { return periodEnd; }
    public void setPeriodEnd(LocalDate periodEnd) { this.periodEnd = periodEnd; }
    public List<String> getEvidence() { return evidence; }
    public void setEvidence(List<String> evidence) { this.evidence = evidence; }
}
