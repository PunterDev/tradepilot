package com.tradepilot.coach.dto;

import lombok.Getter;

import java.math.BigDecimal;
import java.util.List;

@Getter
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

    public CoachDeterministicSummary(
            Integer totalTrades,
            BigDecimal netPnl,
            BigDecimal winRate,
            BigDecimal profitFactor,
            String bestSetup,
            String worstSetup,
            String bestSession,
            String worstSession,
            String biggestMistake,
            String setupAnalysis,
            String emotionAnalysis,
            String confidenceAnalysis,
            List<String> journalMetadataExamples,
            BigDecimal executionScoreAverage,
            BigDecimal riskScoreAverage,
            BigDecimal disciplineScoreAverage,
            List<String> repeatedMistakes,
            List<String> strengths,
            List<String> warnings
    ) {
        this.totalTrades = totalTrades;
        this.netPnl = netPnl;
        this.winRate = winRate;
        this.profitFactor = profitFactor;
        this.bestSetup = bestSetup;
        this.worstSetup = worstSetup;
        this.bestSession = bestSession;
        this.worstSession = worstSession;
        this.biggestMistake = biggestMistake;
        this.setupAnalysis = setupAnalysis;
        this.emotionAnalysis = emotionAnalysis;
        this.confidenceAnalysis = confidenceAnalysis;
        this.journalMetadataExamples = journalMetadataExamples;
        this.executionScoreAverage = executionScoreAverage;
        this.riskScoreAverage = riskScoreAverage;
        this.disciplineScoreAverage = disciplineScoreAverage;
        this.repeatedMistakes = repeatedMistakes;
        this.strengths = strengths;
        this.warnings = warnings;
    }

    public static CoachDeterministicSummary of(
            Integer totalTrades,
            BigDecimal netPnl,
            BigDecimal winRate,
            BigDecimal profitFactor,
            String bestSetup,
            String worstSetup,
            String bestSession,
            String worstSession,
            String biggestMistake,
            String setupAnalysis,
            String emotionAnalysis,
            String confidenceAnalysis,
            List<String> journalMetadataExamples,
            BigDecimal executionScoreAverage,
            BigDecimal riskScoreAverage,
            BigDecimal disciplineScoreAverage,
            List<String> repeatedMistakes,
            List<String> strengths,
            List<String> warnings
    ) {
        return new CoachDeterministicSummary(
                totalTrades,
                netPnl,
                winRate,
                profitFactor,
                bestSetup,
                worstSetup,
                bestSession,
                worstSession,
                biggestMistake,
                setupAnalysis,
                emotionAnalysis,
                confidenceAnalysis,
                journalMetadataExamples,
                executionScoreAverage,
                riskScoreAverage,
                disciplineScoreAverage,
                repeatedMistakes,
                strengths,
                warnings
        );
    }

    public static CoachDeterministicSummary empty(String warning) {
        return of(
                0,
                BigDecimal.ZERO,
                BigDecimal.ZERO,
                BigDecimal.ZERO,
                "Not enough data",
                "Not enough data",
                "Not enough data",
                "Not enough data",
                "Not enough data",
                "No setup metadata available yet.",
                "No emotion metadata available yet.",
                "No confidence metadata available yet.",
                List.of(),
                BigDecimal.ZERO,
                BigDecimal.ZERO,
                BigDecimal.ZERO,
                List.of(),
                List.of(),
                List.of(warning)
        );
    }
}
