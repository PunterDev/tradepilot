package com.tradepilot.tradingdna.dto;

import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
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
}
