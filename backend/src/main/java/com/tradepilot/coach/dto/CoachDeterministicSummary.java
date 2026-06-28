package com.tradepilot.coach.dto;

import lombok.Getter;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

@Getter
public class CoachDeterministicSummary {

    private LocalDate selectedDate;
    private Integer totalTrades;
    private BigDecimal netPnl;
    private BigDecimal winRate;
    private BigDecimal profitFactor;
    private Integer longTradeCount;
    private Integer shortTradeCount;
    private Integer unknownTradeCount;
    private Integer winningTradeCount;
    private Integer losingTradeCount;
    private String dominantDirection;
    private List<String> instruments;
    private BigDecimal longPnl;
    private BigDecimal shortPnl;
    private String worstTradeSide;
    private String selectedDaySummary;
    private String primaryIssueToday;
    private String setupDirectionReview;
    private String dataQuality;
    private String dataQualityReason;
    private String nextSessionRule;

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
            LocalDate selectedDate,
            Integer totalTrades,
            BigDecimal netPnl,
            BigDecimal winRate,
            BigDecimal profitFactor,
            Integer longTradeCount,
            Integer shortTradeCount,
            Integer unknownTradeCount,
            Integer winningTradeCount,
            Integer losingTradeCount,
            String dominantDirection,
            List<String> instruments,
            BigDecimal longPnl,
            BigDecimal shortPnl,
            String worstTradeSide,
            String selectedDaySummary,
            String primaryIssueToday,
            String setupDirectionReview,
            String dataQuality,
            String dataQualityReason,
            String nextSessionRule,
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
        this.selectedDate = selectedDate;
        this.totalTrades = totalTrades;
        this.netPnl = netPnl;
        this.winRate = winRate;
        this.profitFactor = profitFactor;
        this.longTradeCount = longTradeCount;
        this.shortTradeCount = shortTradeCount;
        this.unknownTradeCount = unknownTradeCount;
        this.winningTradeCount = winningTradeCount;
        this.losingTradeCount = losingTradeCount;
        this.dominantDirection = dominantDirection;
        this.instruments = instruments;
        this.longPnl = longPnl;
        this.shortPnl = shortPnl;
        this.worstTradeSide = worstTradeSide;
        this.selectedDaySummary = selectedDaySummary;
        this.primaryIssueToday = primaryIssueToday;
        this.setupDirectionReview = setupDirectionReview;
        this.dataQuality = dataQuality;
        this.dataQualityReason = dataQualityReason;
        this.nextSessionRule = nextSessionRule;
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
            LocalDate selectedDate,
            Integer totalTrades,
            BigDecimal netPnl,
            BigDecimal winRate,
            BigDecimal profitFactor,
            Integer longTradeCount,
            Integer shortTradeCount,
            Integer unknownTradeCount,
            Integer winningTradeCount,
            Integer losingTradeCount,
            String dominantDirection,
            List<String> instruments,
            BigDecimal longPnl,
            BigDecimal shortPnl,
            String worstTradeSide,
            String selectedDaySummary,
            String primaryIssueToday,
            String setupDirectionReview,
            String dataQuality,
            String dataQualityReason,
            String nextSessionRule,
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
                selectedDate,
                totalTrades,
                netPnl,
                winRate,
                profitFactor,
                longTradeCount,
                shortTradeCount,
                unknownTradeCount,
                winningTradeCount,
                losingTradeCount,
                dominantDirection,
                instruments,
                longPnl,
                shortPnl,
                worstTradeSide,
                selectedDaySummary,
                primaryIssueToday,
                setupDirectionReview,
                dataQuality,
                dataQualityReason,
                nextSessionRule,
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
        return emptyDaily(warning);
    }

    public static CoachDeterministicSummary emptyDaily(String warning) {
        return of(
                null,
                0,
                BigDecimal.ZERO,
                BigDecimal.ZERO,
                BigDecimal.ZERO,
                0,
                0,
                0,
                0,
                0,
                "UNKNOWN",
                List.of(),
                BigDecimal.ZERO,
                BigDecimal.ZERO,
                "UNKNOWN",
                "No trades found for this date.",
                "Import trades for this date before drawing conclusions.",
                "No setup/direction review available because there were no trades on this date.",
                "No Trades",
                warning,
                "Import trades first, then review the next session plan.",
                "Not enough data",
                "Not enough data",
                "Not enough data",
                "Not enough data",
                "Not enough data",
                "No setup metadata available yet.",
                "No emotional trading data available due to lack of trades.",
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

    public static CoachDeterministicSummary emptyWeekly(String warning) {
        return of(
                null,
                0,
                BigDecimal.ZERO,
                BigDecimal.ZERO,
                BigDecimal.ZERO,
                0,
                0,
                0,
                0,
                0,
                "UNKNOWN",
                List.of(),
                BigDecimal.ZERO,
                BigDecimal.ZERO,
                "UNKNOWN",
                "No trades found for this week.",
                "Import trades for this week before generating weekly coaching.",
                "No setup review available because there were no trades this week.",
                "No Trades",
                warning,
                "Import trades for this week, then generate weekly coach feedback.",
                "Not enough data",
                "Not enough data",
                "Not enough data",
                "Not enough data",
                "Not enough data",
                "No setup metadata available yet.",
                "No emotional or behavioral pattern can be assessed because there were no trades this week.",
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
