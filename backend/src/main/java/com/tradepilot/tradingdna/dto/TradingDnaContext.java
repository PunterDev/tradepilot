package com.tradepilot.tradingdna.dto;

import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;

@Data
public class TradingDnaContext {
    private Long userId;
    private Long accountId;
    private boolean includeAllAccounts;
    private int days;
    private LocalDate startDate;
    private LocalDate endDate;
    private int totalTrades;
    private BigDecimal netPnl;
    private BigDecimal winRate;
    private String bestSetup;
    private String worstSetup;
    private String bestSession;
    private String worstSession;
    private Map<String, Long> mistakeCounts;
    private Map<String, BigDecimal> setupPnl;
    private Map<String, BigDecimal> emotionPnl;
    private Map<String, BigDecimal> confidencePnl;
    private Map<String, Long> violationCounts;
    private List<String> journalMetadataExamples;
    private List<String> behavioralInsights;
    private List<String> tradeReviewSummaries;
    private List<String> aiTradeStrengths;
    private List<String> aiTradeMistakes;
    private List<String> coachNotes;
    private List<String> deterministicSignals;
    private List<TradingDnaTimelinePoint> timeline;
    private Integer deterministicDisciplineScore;
    private Integer deterministicExecutionScore;
    private Integer deterministicPatienceScore;
    private Integer deterministicConsistencyScore;

    public static TradingDnaContext of(
            Long userId,
            Long accountId,
            boolean includeAllAccounts,
            int days,
            LocalDate startDate,
            LocalDate endDate,
            int totalTrades,
            BigDecimal netPnl,
            BigDecimal winRate,
            String bestSetup,
            String worstSetup,
            String bestSession,
            String worstSession,
            Map<String, Long> mistakeCounts,
            Map<String, BigDecimal> setupPnl,
            Map<String, BigDecimal> emotionPnl,
            Map<String, BigDecimal> confidencePnl,
            Map<String, Long> violationCounts,
            List<String> journalMetadataExamples,
            List<String> behavioralInsights,
            List<String> tradeReviewSummaries,
            List<String> aiTradeStrengths,
            List<String> aiTradeMistakes,
            List<String> coachNotes,
            List<String> deterministicSignals,
            List<TradingDnaTimelinePoint> timeline,
            Integer deterministicDisciplineScore,
            Integer deterministicExecutionScore,
            Integer deterministicPatienceScore,
            Integer deterministicConsistencyScore
    ) {
        TradingDnaContext context = new TradingDnaContext();
        context.setUserId(userId);
        context.setAccountId(accountId);
        context.setIncludeAllAccounts(includeAllAccounts);
        context.setDays(days);
        context.setStartDate(startDate);
        context.setEndDate(endDate);
        context.setTotalTrades(totalTrades);
        context.setNetPnl(netPnl);
        context.setWinRate(winRate);
        context.setBestSetup(bestSetup);
        context.setWorstSetup(worstSetup);
        context.setBestSession(bestSession);
        context.setWorstSession(worstSession);
        context.setMistakeCounts(mistakeCounts);
        context.setSetupPnl(setupPnl);
        context.setEmotionPnl(emotionPnl);
        context.setConfidencePnl(confidencePnl);
        context.setViolationCounts(violationCounts);
        context.setJournalMetadataExamples(journalMetadataExamples);
        context.setBehavioralInsights(behavioralInsights);
        context.setTradeReviewSummaries(tradeReviewSummaries);
        context.setAiTradeStrengths(aiTradeStrengths);
        context.setAiTradeMistakes(aiTradeMistakes);
        context.setCoachNotes(coachNotes);
        context.setDeterministicSignals(deterministicSignals);
        context.setTimeline(timeline);
        context.setDeterministicDisciplineScore(deterministicDisciplineScore);
        context.setDeterministicExecutionScore(deterministicExecutionScore);
        context.setDeterministicPatienceScore(deterministicPatienceScore);
        context.setDeterministicConsistencyScore(deterministicConsistencyScore);
        return context;
    }
}
