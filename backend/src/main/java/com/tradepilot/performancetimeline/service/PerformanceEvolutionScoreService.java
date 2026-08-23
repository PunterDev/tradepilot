package com.tradepilot.performancetimeline.service;

import com.tradepilot.performancetimeline.dto.PerformanceCategoryScores;
import com.tradepilot.performancetimeline.dto.PerformanceTimelineMetrics;
import com.tradepilot.performancetimeline.entity.ScoreConfidence;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;

@Service
public class PerformanceEvolutionScoreService {
    public PerformanceCategoryScores categoryScores(PerformanceTimelineMetrics metrics) {
        int discipline = clamp(100
                - metrics.getOvertradingDays() * 8
                - metrics.getTradesAfterConsecutiveLosses() * 4
                - metrics.getRevengeTradeCount() * 7
                - metrics.getFomoTradeCount() * 5);
        int risk = clamp(100
                - metrics.getOversizedTradeCount() * 8
                - penalty(metrics.getLargestDailyLossR(), new BigDecimal("2.00"), 22)
                - penalty(metrics.getRiskConsistency(), new BigDecimal("0.50"), 20));
        int execution = clamp(55
                + scoreFrom(metrics.getAverageR(), 15)
                + scoreFrom(metrics.getProfitFactor(), 8)
                + scoreFrom(metrics.getHighConfidenceTradePercent(), 0.18)
                - metrics.getScratchTradeCount());
        int consistency = clamp(55
                + scoreFrom(metrics.getProfitableTradingDayPercent(), 0.25)
                - penalty(metrics.getDailyRVolatility(), new BigDecimal("2.00"), 18)
                + metrics.getGreenWeekCount() * 4
                - metrics.getRedWeekCount() * 3);
        int psychology = clamp(55
                + scoreFrom(metrics.getMetadataCompletionPercent(), 0.20)
                + metrics.getCalmTradeCount() * 2
                - metrics.getEmotionalTradeCount() * 4);
        return new PerformanceCategoryScores(discipline, risk, execution, consistency, psychology);
    }

    public int evolutionScore(PerformanceCategoryScores scores) {
        double value = scores.getDiscipline() * 0.25
                + scores.getRiskManagement() * 0.25
                + scores.getExecution() * 0.20
                + scores.getConsistency() * 0.15
                + scores.getPsychology() * 0.15;
        return clamp((int) Math.round(value));
    }

    public ScoreConfidence confidence(int tradeCount) {
        if (tradeCount >= 30) return ScoreConfidence.HIGH;
        if (tradeCount >= 10) return ScoreConfidence.MEDIUM;
        return ScoreConfidence.LOW;
    }

    private int scoreFrom(BigDecimal value, double multiplier) {
        if (value == null) return 0;
        return (int) Math.round(value.doubleValue() * multiplier);
    }

    private int penalty(BigDecimal value, BigDecimal threshold, int maxPenalty) {
        if (value == null) return 0;
        BigDecimal absolute = value.abs();
        if (absolute.compareTo(threshold) <= 0) return 0;
        return Math.min(maxPenalty, (int) Math.round(absolute.subtract(threshold).doubleValue() * 8));
    }

    private int clamp(int value) {
        return Math.max(0, Math.min(100, value));
    }
}
