package com.tradepilot.performancetimeline.dto;

import java.util.List;

public class PerformanceTimelineNarrativeResponse {
    private String theme;
    private String summary;
    private String biggestImprovement;
    private String biggestLeak;
    private String emergingRisk;
    private String bestBehavior;
    private String nextFocus;
    private String encouragement;
    private List<String> supportingEvidence;
    private String confidenceExplanation;
    private boolean aiGenerated;

    public PerformanceTimelineNarrativeResponse() {
    }

    public static PerformanceTimelineNarrativeResponse of(String theme, String summary, String biggestImprovement, String biggestLeak, String emergingRisk, String bestBehavior, String nextFocus, String encouragement, List<String> supportingEvidence, String confidenceExplanation, boolean aiGenerated) {
        PerformanceTimelineNarrativeResponse response = new PerformanceTimelineNarrativeResponse();
        response.theme = theme;
        response.summary = summary;
        response.biggestImprovement = biggestImprovement;
        response.biggestLeak = biggestLeak;
        response.emergingRisk = emergingRisk;
        response.bestBehavior = bestBehavior;
        response.nextFocus = nextFocus;
        response.encouragement = encouragement;
        response.supportingEvidence = supportingEvidence;
        response.confidenceExplanation = confidenceExplanation;
        response.aiGenerated = aiGenerated;
        return response;
    }

    public String getTheme() { return theme; }
    public void setTheme(String theme) { this.theme = theme; }
    public String getSummary() { return summary; }
    public void setSummary(String summary) { this.summary = summary; }
    public String getBiggestImprovement() { return biggestImprovement; }
    public void setBiggestImprovement(String biggestImprovement) { this.biggestImprovement = biggestImprovement; }
    public String getBiggestLeak() { return biggestLeak; }
    public void setBiggestLeak(String biggestLeak) { this.biggestLeak = biggestLeak; }
    public String getEmergingRisk() { return emergingRisk; }
    public void setEmergingRisk(String emergingRisk) { this.emergingRisk = emergingRisk; }
    public String getBestBehavior() { return bestBehavior; }
    public void setBestBehavior(String bestBehavior) { this.bestBehavior = bestBehavior; }
    public String getNextFocus() { return nextFocus; }
    public void setNextFocus(String nextFocus) { this.nextFocus = nextFocus; }
    public String getEncouragement() { return encouragement; }
    public void setEncouragement(String encouragement) { this.encouragement = encouragement; }
    public List<String> getSupportingEvidence() { return supportingEvidence; }
    public void setSupportingEvidence(List<String> supportingEvidence) { this.supportingEvidence = supportingEvidence; }
    public String getConfidenceExplanation() { return confidenceExplanation; }
    public void setConfidenceExplanation(String confidenceExplanation) { this.confidenceExplanation = confidenceExplanation; }
    public boolean isAiGenerated() { return aiGenerated; }
    public void setAiGenerated(boolean aiGenerated) { this.aiGenerated = aiGenerated; }
}
