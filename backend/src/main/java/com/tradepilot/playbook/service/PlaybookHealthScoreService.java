package com.tradepilot.playbook.service;

import com.tradepilot.playbook.dto.PlaybookHealthScoreResponse;
import com.tradepilot.playbook.dto.PlaybookMetricsResponse;
import com.tradepilot.playbook.entity.PlaybookChecklistItem;
import com.tradepilot.playbook.entity.PlaybookSetup;
import com.tradepilot.playbook.entity.TradePlaybookAssignment;
import com.tradepilot.playbook.enums.PlaybookConfidence;
import org.springframework.stereotype.Service;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

@Service
public class PlaybookHealthScoreService {
    public PlaybookHealthScoreResponse score(PlaybookSetup setup, List<PlaybookChecklistItem> items, List<TradePlaybookAssignment> assignments, PlaybookMetricsResponse metrics) {
        int definition = definitionCompleteness(setup, items);
        int evidence = historicalEvidence(assignments);
        int adherence = ruleAdherence(assignments);
        int consistency = performanceConsistency(metrics);
        int relevance = recentRelevance(metrics);
        int total = clamp(Math.round(definition * 0.20f + evidence * 0.25f + adherence * 0.25f + consistency * 0.20f + relevance * 0.10f));
        return new PlaybookHealthScoreResponse(total, confidence(assignments.size()).name(), definition, evidence, adherence, consistency, relevance);
    }

    private int definitionCompleteness(PlaybookSetup setup, List<PlaybookChecklistItem> items) {
        int points = 0;
        if (has(setup.getDescription())) points += 15;
        if (has(setup.getMarketContextJson())) points += 12;
        if (items.stream().anyMatch(i -> i.getChecklistType().name().equals("ENTRY"))) points += 15;
        if (items.stream().anyMatch(i -> i.getChecklistType().name().equals("INVALIDATION"))) points += 12;
        if (has(setup.getRiskRulesJson()) || items.stream().anyMatch(i -> i.getChecklistType().name().equals("RISK"))) points += 12;
        if (has(setup.getManagementRulesJson()) || items.stream().anyMatch(i -> i.getChecklistType().name().equals("MANAGEMENT"))) points += 10;
        if (items.stream().anyMatch(i -> Boolean.TRUE.equals(i.getRequired()))) points += 12;
        if (has(setup.getInstrumentsJson())) points += 6;
        if (has(setup.getSessionsJson())) points += 6;
        return clamp(points);
    }

    private int historicalEvidence(List<TradePlaybookAssignment> assignments) {
        int count = assignments.size();
        if (count >= 30) return 100;
        if (count >= 20) return 80;
        if (count >= 10) return 60;
        if (count >= 5) return 40;
        return count * 6;
    }

    private int ruleAdherence(List<TradePlaybookAssignment> assignments) {
        if (assignments.isEmpty()) return 0;
        double average = assignments.stream().map(TradePlaybookAssignment::getComplianceScore).filter(v -> v != null).mapToInt(Integer::intValue).average().orElse(0);
        double reviewed = assignments.stream().filter(a -> Boolean.TRUE.equals(a.getReviewedByUser())).count() * 100.0 / assignments.size();
        return clamp((int) Math.round((average * 0.75) + (reviewed * 0.25)));
    }

    private int performanceConsistency(PlaybookMetricsResponse metrics) {
        if (metrics.totalLinkedTrades() < 5) return 20;
        int score = 55;
        if (metrics.profitFactor() != null && metrics.profitFactor().compareTo(BigDecimal.valueOf(1.5)) >= 0) score += 15;
        if (metrics.averageR() != null && metrics.averageR().compareTo(BigDecimal.ZERO) > 0) score += 15;
        if (metrics.maxDrawdownR() != null && metrics.maxDrawdownR().compareTo(BigDecimal.valueOf(-5)) > 0) score += 10;
        if (metrics.largestWinnerR() != null && metrics.totalR() != null && metrics.totalR().compareTo(BigDecimal.ZERO) > 0 && metrics.largestWinnerR().compareTo(metrics.totalR().multiply(BigDecimal.valueOf(0.7))) > 0) score -= 20;
        return clamp(score);
    }

    private int recentRelevance(PlaybookMetricsResponse metrics) {
        if (metrics.lastUsedDate() == null) return 0;
        long days = java.time.temporal.ChronoUnit.DAYS.between(metrics.lastUsedDate(), LocalDate.now());
        if (days <= 14) return 100;
        if (days <= 45) return 75;
        if (days <= 90) return 45;
        return 15;
    }

    private PlaybookConfidence confidence(int trades) {
        if (trades < 5) return PlaybookConfidence.INSUFFICIENT;
        if (trades < 10) return PlaybookConfidence.LOW;
        if (trades < 30) return PlaybookConfidence.MEDIUM;
        return PlaybookConfidence.HIGH;
    }

    private boolean has(String value) {
        return value != null && !value.isBlank() && !value.equals("[]");
    }

    private int clamp(int value) {
        return Math.max(0, Math.min(100, value));
    }
}
