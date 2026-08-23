package com.tradepilot.ai.memory.service;

import com.tradepilot.ai.memory.dto.CommonBehaviorResponse;
import com.tradepilot.ai.memory.dto.SimilarTradeResponse;
import com.tradepilot.ai.memory.dto.TradingMemoryMetricsResponse;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
public class TradeMemoryAnalyticsService {
    public TradingMemoryMetricsResponse calculate(List<SimilarTradeResponse> trades) {
        List<BigDecimal> rValues = trades.stream().map(SimilarTradeResponse::resultR).filter(Objects::nonNull).sorted().toList();
        int wins = (int) rValues.stream().filter(value -> value.signum() > 0).count();
        int losses = (int) rValues.stream().filter(value -> value.signum() < 0).count();
        Map.Entry<String, Long> mistake = mostCommon(trades.stream().map(SimilarTradeResponse::mistake).filter(Objects::nonNull).toList());
        Map.Entry<String, Long> emotion = mostCommon(trades.stream().map(SimilarTradeResponse::emotion).filter(Objects::nonNull).toList());
        return new TradingMemoryMetricsResponse(
                trades.size(),
                average(rValues),
                median(rValues),
                average(trades.stream().map(SimilarTradeResponse::maeR).filter(Objects::nonNull).toList()),
                average(trades.stream().map(SimilarTradeResponse::mfeR).filter(Objects::nonNull).toList()),
                averageExitEfficiency(trades),
                wins,
                losses,
                mistake == null ? null : mistake.getKey(),
                mistake == null ? 0 : mistake.getValue().intValue(),
                emotion == null ? null : emotion.getKey(),
                emotion == null ? 0 : emotion.getValue().intValue()
        );
    }

    public CommonBehaviorResponse commonBehavior(TradingMemoryMetricsResponse metrics, TradeMemoryNormalizer normalizer) {
        if (metrics == null || metrics.mostCommonMistake() == null) {
            return null;
        }
        return new CommonBehaviorResponse(
                metrics.mostCommonMistake(),
                normalizer.label(metrics.mostCommonMistake()),
                metrics.mistakeOccurrences(),
                metrics.sampleSize(),
                metrics.mostCommonEmotion()
        );
    }

    private BigDecimal average(List<BigDecimal> values) {
        if (values == null || values.isEmpty()) return null;
        BigDecimal sum = values.stream().reduce(BigDecimal.ZERO, BigDecimal::add);
        return sum.divide(BigDecimal.valueOf(values.size()), 4, RoundingMode.HALF_UP);
    }

    private BigDecimal median(List<BigDecimal> sorted) {
        if (sorted == null || sorted.isEmpty()) return null;
        int middle = sorted.size() / 2;
        if (sorted.size() % 2 == 1) return sorted.get(middle);
        return sorted.get(middle - 1).add(sorted.get(middle)).divide(BigDecimal.valueOf(2), 4, RoundingMode.HALF_UP);
    }

    private BigDecimal averageExitEfficiency(List<SimilarTradeResponse> trades) {
        List<BigDecimal> values = trades.stream()
                .filter(t -> t.resultR() != null && t.mfeR() != null && t.resultR().signum() > 0 && t.mfeR().signum() > 0)
                .map(t -> t.resultR().divide(t.mfeR(), 4, RoundingMode.HALF_UP))
                .toList();
        return average(values);
    }

    private Map.Entry<String, Long> mostCommon(List<String> values) {
        if (values.isEmpty()) return null;
        return values.stream()
                .collect(Collectors.groupingBy(Function.identity(), Collectors.counting()))
                .entrySet()
                .stream()
                .max(Comparator.comparingLong(Map.Entry<String, Long>::getValue).thenComparing(Map.Entry::getKey))
                .orElse(null);
    }
}
