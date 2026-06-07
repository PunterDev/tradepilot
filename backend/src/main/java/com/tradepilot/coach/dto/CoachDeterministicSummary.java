package com.tradepilot.coach.dto;

import lombok.Builder;
import lombok.Getter;

import java.math.BigDecimal;
import java.util.List;

@Getter
@Builder
public class CoachDeterministicSummary {

    private Integer totalTrades;
    private BigDecimal netPnl;
    private BigDecimal winRate;
    private BigDecimal profitFactor;

    private String bestSetup;
    private String worstSetup;
    private String bestSession;
    private String worstSession;
    private String biggestMistake;
    private String setupAnalysis;
    private String emotionAnalysis;
    private String confidenceAnalysis;
    private List<String> journalMetadataExamples;

    private BigDecimal executionScoreAverage;
    private BigDecimal riskScoreAverage;
    private BigDecimal disciplineScoreAverage;

    private List<String> repeatedMistakes;
    private List<String> strengths;
    private List<String> warnings;
}
