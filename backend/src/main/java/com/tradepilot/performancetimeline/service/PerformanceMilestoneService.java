package com.tradepilot.performancetimeline.service;

import com.fasterxml.jackson.databind.JavaType;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.tradepilot.performancetimeline.dto.PerformanceMilestoneResponse;
import com.tradepilot.performancetimeline.dto.PerformanceTimelineMonthResponse;
import com.tradepilot.performancetimeline.entity.MilestoneType;
import com.tradepilot.performancetimeline.entity.PerformanceMilestone;
import com.tradepilot.performancetimeline.repository.PerformanceMilestoneRepository;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

@Service
public class PerformanceMilestoneService {
    private final PerformanceMilestoneRepository repository;
    private final ObjectMapper objectMapper;

    public PerformanceMilestoneService(PerformanceMilestoneRepository repository, ObjectMapper objectMapper) {
        this.repository = repository;
        this.objectMapper = objectMapper;
    }

    public List<PerformanceMilestoneResponse> updateMilestones(Long userId, Long accountId, List<PerformanceTimelineMonthResponse> months) {
        List<PerformanceMilestoneResponse> earned = new ArrayList<>();
        int cumulativeTrades = months.stream().mapToInt(PerformanceTimelineMonthResponse::getTradeCount).sum();
        maybeEarn(userId, accountId, MilestoneType.FIRST_10_TRADES, cumulativeTrades >= 10, "First 10 trades analyzed", months, earned, List.of(cumulativeTrades + " trades in timeline sample"));
        maybeEarn(userId, accountId, MilestoneType.FIRST_50_TRADES, cumulativeTrades >= 50, "First 50 trades analyzed", months, earned, List.of(cumulativeTrades + " trades in timeline sample"));
        maybeEarn(userId, accountId, MilestoneType.FIRST_100_TRADES, cumulativeTrades >= 100, "First 100 trades analyzed", months, earned, List.of(cumulativeTrades + " trades in timeline sample"));
        months.stream().filter(month -> month.getEvolutionScore() != null).forEach(month -> {
            maybeEarn(userId, accountId, MilestoneType.EVOLUTION_SCORE_ABOVE_60, month.getEvolutionScore() >= 60, "Evolution Score above 60", List.of(month), earned, List.of("Score " + month.getEvolutionScore()));
            maybeEarn(userId, accountId, MilestoneType.EVOLUTION_SCORE_ABOVE_70, month.getEvolutionScore() >= 70, "Evolution Score above 70", List.of(month), earned, List.of("Score " + month.getEvolutionScore()));
            maybeEarn(userId, accountId, MilestoneType.EVOLUTION_SCORE_ABOVE_80, month.getEvolutionScore() >= 80, "Evolution Score above 80", List.of(month), earned, List.of("Score " + month.getEvolutionScore()));
            maybeEarn(userId, accountId, MilestoneType.EVOLUTION_SCORE_ABOVE_90, month.getEvolutionScore() >= 90, "Evolution Score above 90", List.of(month), earned, List.of("Score " + month.getEvolutionScore()));
            maybeEarn(userId, accountId, MilestoneType.FIRST_PROFITABLE_MONTH, month.getMetrics() != null && month.getMetrics().getNetPnl() != null && month.getMetrics().getNetPnl().signum() > 0, "First profitable timeline month", List.of(month), earned, List.of("Net P&L " + month.getMetrics().getNetPnl()));
            maybeEarn(userId, accountId, MilestoneType.FIRST_MONTH_ABOVE_2R, month.getMetrics() != null && month.getMetrics().getTotalR() != null && month.getMetrics().getTotalR().doubleValue() >= 2.0, "First month above +2R", List.of(month), earned, List.of("Total R " + month.getMetrics().getTotalR()));
            maybeEarn(userId, accountId, MilestoneType.FIRST_MONTH_ABOVE_5R, month.getMetrics() != null && month.getMetrics().getTotalR() != null && month.getMetrics().getTotalR().doubleValue() >= 5.0, "First month above +5R", List.of(month), earned, List.of("Total R " + month.getMetrics().getTotalR()));
            maybeEarn(userId, accountId, MilestoneType.FIRST_MONTH_ABOVE_10R, month.getMetrics() != null && month.getMetrics().getTotalR() != null && month.getMetrics().getTotalR().doubleValue() >= 10.0, "First month above +10R", List.of(month), earned, List.of("Total R " + month.getMetrics().getTotalR()));
        });
        return repository.findByUserIdAndAccountIdOrderByEarnedDateDesc(userId, accountId).stream().map(this::toResponse).toList();
    }

    private void maybeEarn(Long userId, Long accountId, MilestoneType type, boolean condition, String title, List<PerformanceTimelineMonthResponse> months, List<PerformanceMilestoneResponse> earned, List<String> evidence) {
        if (!condition || months.isEmpty() || repository.findByUserIdAndAccountIdAndMilestoneType(userId, accountId, type).isPresent()) {
            return;
        }
        PerformanceTimelineMonthResponse month = months.get(0);
        PerformanceMilestone milestone = new PerformanceMilestone();
        milestone.setUserId(userId);
        milestone.setAccountId(accountId);
        milestone.setMilestoneType(type);
        milestone.setEarnedDate(LocalDate.now());
        milestone.setPeriodStart(month.getPeriodStart());
        milestone.setPeriodEnd(month.getPeriodEnd());
        milestone.setEvidenceJson(writeEvidence(evidence));
        repository.save(milestone);
        earned.add(new PerformanceMilestoneResponse(type, title, milestone.getEarnedDate(), milestone.getPeriodStart(), milestone.getPeriodEnd(), evidence));
    }

    private PerformanceMilestoneResponse toResponse(PerformanceMilestone milestone) {
        return new PerformanceMilestoneResponse(
                milestone.getMilestoneType(),
                titleFor(milestone.getMilestoneType()),
                milestone.getEarnedDate(),
                milestone.getPeriodStart(),
                milestone.getPeriodEnd(),
                readEvidence(milestone.getEvidenceJson())
        );
    }

    private String titleFor(MilestoneType type) {
        if (type == MilestoneType.FIRST_10_TRADES) return "First 10 trades analyzed";
        if (type == MilestoneType.FIRST_50_TRADES) return "First 50 trades analyzed";
        if (type == MilestoneType.FIRST_100_TRADES) return "First 100 trades analyzed";
        if (type == MilestoneType.FIRST_PROFITABLE_MONTH) return "First profitable timeline month";
        if (type == MilestoneType.FIRST_MONTH_ABOVE_2R) return "First month above +2R";
        if (type == MilestoneType.FIRST_MONTH_ABOVE_5R) return "First month above +5R";
        if (type == MilestoneType.FIRST_MONTH_ABOVE_10R) return "First month above +10R";
        if (type == MilestoneType.THREE_GREEN_WEEKS) return "Three green weeks";
        if (type == MilestoneType.FOUR_GREEN_WEEKS) return "Four green weeks";
        if (type == MilestoneType.PROFIT_FACTOR_ABOVE_1_5) return "Profit factor above 1.5";
        if (type == MilestoneType.PROFIT_FACTOR_ABOVE_2_0) return "Profit factor above 2.0";
        if (type == MilestoneType.EVOLUTION_SCORE_ABOVE_60) return "Evolution Score above 60";
        if (type == MilestoneType.EVOLUTION_SCORE_ABOVE_70) return "Evolution Score above 70";
        if (type == MilestoneType.EVOLUTION_SCORE_ABOVE_80) return "Evolution Score above 80";
        if (type == MilestoneType.EVOLUTION_SCORE_ABOVE_90) return "Evolution Score above 90";
        if (type == MilestoneType.NEW_EVOLUTION_SCORE_HIGH) return "New Evolution Score high";
        return "Performance milestone";
    }

    private String writeEvidence(List<String> evidence) {
        try {
            return objectMapper.writeValueAsString(evidence);
        } catch (Exception e) {
            return "[]";
        }
    }

    private List<String> readEvidence(String json) {
        try {
            JavaType type = objectMapper.getTypeFactory().constructCollectionType(List.class, String.class);
            return objectMapper.readValue(json == null ? "[]" : json, type);
        } catch (Exception e) {
            return List.of();
        }
    }
}
