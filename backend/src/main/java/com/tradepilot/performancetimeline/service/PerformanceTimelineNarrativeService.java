package com.tradepilot.performancetimeline.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.tradepilot.ai.openai.OpenAiResponsesClient;
import com.tradepilot.performancetimeline.dto.PerformanceTimelineMetrics;
import com.tradepilot.performancetimeline.dto.PerformanceTimelineNarrativeResponse;
import com.tradepilot.performancetimeline.entity.TimelineDataStatus;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Service
public class PerformanceTimelineNarrativeService {
    private static final Logger log = LoggerFactory.getLogger(PerformanceTimelineNarrativeService.class);
    private final ObjectMapper objectMapper;
    private final OpenAiResponsesClient openAiResponsesClient;

    public PerformanceTimelineNarrativeService(ObjectMapper objectMapper, OpenAiResponsesClient openAiResponsesClient) {
        this.objectMapper = objectMapper;
        this.openAiResponsesClient = openAiResponsesClient;
    }

    public PerformanceTimelineNarrativeResponse generateNarrative(Long userId, TimelineDataStatus status, PerformanceTimelineMetrics metrics, Integer scoreChange, boolean allowAi) {
        PerformanceTimelineNarrativeResponse fallback = deterministicNarrative(status, metrics, scoreChange);
        if (!allowAi || status != TimelineDataStatus.READY || !openAiResponsesClient.isConfigured()) {
            return fallback;
        }
        try {
            String content = cleanJson(openAiResponsesClient.generateJson(userId, "PERFORMANCE_TIMELINE", systemPrompt(), userPrompt(metrics, scoreChange)).content());
            PerformanceTimelineNarrativeResponse ai = objectMapper.readValue(content, PerformanceTimelineNarrativeResponse.class);
            ai.setAiGenerated(true);
            return mergeMissing(ai, fallback);
        } catch (Exception e) {
            log.warn("Performance Timeline AI narrative failed; deterministic narrative will be used. userId={}", userId, e);
            return fallback;
        }
    }

    public PerformanceTimelineNarrativeResponse deterministicNarrative(TimelineDataStatus status, PerformanceTimelineMetrics metrics, Integer scoreChange) {
        if (status == TimelineDataStatus.NO_ACTIVITY) {
            return PerformanceTimelineNarrativeResponse.of(
                    "No activity",
                    "No trades were recorded for this month, so TradePilot cannot judge progress.",
                    null,
                    null,
                    "Inactivity can hide whether process quality is improving.",
                    null,
                    "Import trades or journal the next active month before drawing conclusions.",
                    "A quiet month is not a failure, but it should not be scored as improvement.",
                    List.of("0 trades recorded"),
                    "No confidence because there was no trading activity.",
                    false
            );
        }
        if (status == TimelineDataStatus.LIMITED_DATA) {
            return PerformanceTimelineNarrativeResponse.of(
                    "Limited data",
                    "This month has fewer than five trades, so the timeline shows metrics without a full chapter.",
                    null,
                    null,
                    "Small samples can make one trade look like a trend.",
                    null,
                    "Collect at least five valid trades before using this month as a coaching chapter.",
                    "Keep journaling; the signal gets cleaner as the sample grows.",
                    List.of(metrics.getTotalTrades() + " trades recorded"),
                    "Low confidence because the sample is below the five-trade threshold.",
                    false
            );
        }
        List<String> evidence = new ArrayList<>();
        evidence.add(metrics.getTotalTrades() + " trades");
        evidence.add("Net P&L " + metrics.getNetPnl());
        if (metrics.getAverageR() != null) evidence.add("Average R " + metrics.getAverageR());
        String theme = metrics.getNetPnl().signum() >= 0 ? "Process is holding up" : "Process pressure month";
        String leak = metrics.getOvertradingDays() > 0
                ? "High trade-count days were the clearest discipline risk."
                : metrics.getLargestDailyLossR() != null && metrics.getLargestDailyLossR().signum() < 0
                    ? "Largest daily loss is the main risk-management item to review."
                    : "The main leak is not yet dominant; review weak setups and emotional tags.";
        String improvement = scoreChange != null && scoreChange > 0
                ? "Evolution Score improved by " + scoreChange + " points from the prior eligible month."
                : "Use this month as a baseline for the next comparison.";
        String nextFocus = metrics.getAverageR() != null && metrics.getAverageR().signum() < 0
                ? "Reduce weak trades and require clearer confirmation before entry."
                : "Protect the behaviors that produced positive expectancy.";
        return PerformanceTimelineNarrativeResponse.of(
                theme,
                "This month produced " + metrics.getNetPnl() + " across " + metrics.getTotalTrades() + " trades with a " + metrics.getWinRate() + "% win rate.",
                improvement,
                leak,
                metrics.getEmotionalTradeCount() > 0 ? "Emotion-tagged trades deserve review before they become a larger pattern." : "No strong new emotional risk is visible from available metadata.",
                metrics.getHighConfidenceTradePercent() != null && metrics.getHighConfidenceTradePercent().doubleValue() >= 50 ? "High-confidence trade selection was a positive signal." : "Trade selection quality needs more evidence.",
                nextFocus,
                "The goal is not only greener P&L; it is a cleaner process that can repeat.",
                evidence,
                confidenceText(metrics.getTotalTrades()),
                false
        );
    }

    private String confidenceText(int trades) {
        if (trades >= 30) return "High confidence because the month has 30 or more trades.";
        if (trades >= 10) return "Medium confidence because the month has a usable but still developing sample.";
        return "Low confidence because the month has fewer than 10 trades.";
    }

    private String systemPrompt() {
        return """
                You are TradePilot's AI Performance Timeline coach.
                Return strict JSON only. Do not wrap in markdown.
                Use only the metrics provided by the user. Do not invent trades, numbers, dates, or percentages.
                Keep the tone direct, calm, coach-like, and useful for a futures trader.
                JSON fields: theme, summary, biggestImprovement, biggestLeak, emergingRisk, bestBehavior, nextFocus, encouragement, supportingEvidence, confidenceExplanation.
                supportingEvidence must be an array of strings using only supplied values.
                """;
    }

    private String userPrompt(PerformanceTimelineMetrics metrics, Integer scoreChange) throws Exception {
        return "Create one monthly performance chapter from these deterministic metrics. scoreChange="
                + scoreChange
                + "\nmetrics="
                + objectMapper.writeValueAsString(metrics);
    }

    private String cleanJson(String content) {
        if (content == null) return "{}";
        return content.replace("```json", "").replace("```", "").trim();
    }

    private PerformanceTimelineNarrativeResponse mergeMissing(PerformanceTimelineNarrativeResponse ai, PerformanceTimelineNarrativeResponse fallback) {
        if (ai.getTheme() == null || ai.getTheme().isBlank()) ai.setTheme(fallback.getTheme());
        if (ai.getSummary() == null || ai.getSummary().isBlank()) ai.setSummary(fallback.getSummary());
        if (ai.getBiggestImprovement() == null || ai.getBiggestImprovement().isBlank()) ai.setBiggestImprovement(fallback.getBiggestImprovement());
        if (ai.getBiggestLeak() == null || ai.getBiggestLeak().isBlank()) ai.setBiggestLeak(fallback.getBiggestLeak());
        if (ai.getEmergingRisk() == null || ai.getEmergingRisk().isBlank()) ai.setEmergingRisk(fallback.getEmergingRisk());
        if (ai.getBestBehavior() == null || ai.getBestBehavior().isBlank()) ai.setBestBehavior(fallback.getBestBehavior());
        if (ai.getNextFocus() == null || ai.getNextFocus().isBlank()) ai.setNextFocus(fallback.getNextFocus());
        if (ai.getEncouragement() == null || ai.getEncouragement().isBlank()) ai.setEncouragement(fallback.getEncouragement());
        if (ai.getSupportingEvidence() == null || ai.getSupportingEvidence().isEmpty()) ai.setSupportingEvidence(fallback.getSupportingEvidence());
        if (ai.getConfidenceExplanation() == null || ai.getConfidenceExplanation().isBlank()) ai.setConfidenceExplanation(fallback.getConfidenceExplanation());
        return ai;
    }
}
