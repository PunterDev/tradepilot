package com.tradepilot.performancetimeline.service;

import com.tradepilot.performancetimeline.dto.PerformanceMetricComparison;
import com.tradepilot.performancetimeline.dto.PerformanceTimelineMetrics;
import com.tradepilot.performancetimeline.entity.ComparisonResult;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.List;

@Service
public class PerformanceTimelineComparisonService {
    public List<PerformanceMetricComparison> compare(PerformanceTimelineMetrics previous, PerformanceTimelineMetrics current, Integer previousScore, Integer currentScore) {
        if (previous == null || current == null) {
            return List.of();
        }
        List<PerformanceMetricComparison> comparisons = new ArrayList<>();
        comparisons.add(metric("Total R", previous.getTotalR(), current.getTotalR(), true));
        comparisons.add(metric("Average R", previous.getAverageR(), current.getAverageR(), true));
        comparisons.add(metric("Win rate", previous.getWinRate(), current.getWinRate(), true));
        comparisons.add(metric("Profit factor", previous.getProfitFactor(), current.getProfitFactor(), true));
        comparisons.add(metric("Overtrading days", BigDecimal.valueOf(previous.getOvertradingDays()), BigDecimal.valueOf(current.getOvertradingDays()), false));
        comparisons.add(metric("Risk consistency", previous.getRiskConsistency(), current.getRiskConsistency(), false));
        comparisons.add(metric("Largest daily loss R", previous.getLargestDailyLossR(), current.getLargestDailyLossR(), true));
        comparisons.add(metric("Emotional trades", BigDecimal.valueOf(previous.getEmotionalTradeCount()), BigDecimal.valueOf(current.getEmotionalTradeCount()), false));
        comparisons.add(metric("Average trades per day", previous.getAverageTradesPerActiveDay(), current.getAverageTradesPerActiveDay(), false));
        comparisons.add(metric("Evolution Score", previousScore == null ? null : BigDecimal.valueOf(previousScore), currentScore == null ? null : BigDecimal.valueOf(currentScore), true));
        return comparisons.stream().filter(comparison -> comparison.getPreviousValue() != null && comparison.getCurrentValue() != null).toList();
    }

    private PerformanceMetricComparison metric(String label, BigDecimal previous, BigDecimal current, boolean higherIsBetter) {
        if (previous == null || current == null) {
            return new PerformanceMetricComparison(label, previous, current, null, null, ComparisonResult.UNCHANGED, higherIsBetter);
        }
        BigDecimal change = current.subtract(previous).setScale(2, RoundingMode.HALF_UP);
        BigDecimal pct = previous.compareTo(BigDecimal.ZERO) == 0
                ? null
                : change.divide(previous.abs(), 4, RoundingMode.HALF_UP).multiply(BigDecimal.valueOf(100)).setScale(2, RoundingMode.HALF_UP);
        int comparison = change.compareTo(BigDecimal.ZERO);
        ComparisonResult result;
        if (comparison == 0) {
            result = ComparisonResult.UNCHANGED;
        } else if ((comparison > 0 && higherIsBetter) || (comparison < 0 && !higherIsBetter)) {
            result = ComparisonResult.IMPROVED;
        } else {
            result = ComparisonResult.DECLINED;
        }
        return new PerformanceMetricComparison(label, previous, current, change, pct, result, higherIsBetter);
    }
}
