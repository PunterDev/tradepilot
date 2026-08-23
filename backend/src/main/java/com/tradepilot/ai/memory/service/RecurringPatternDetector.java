package com.tradepilot.ai.memory.service;

import com.tradepilot.ai.memory.config.TradingMemoryProperties;
import com.tradepilot.ai.memory.dto.RecurringPatternResponse;
import com.tradepilot.ai.memory.dto.SimilarTradeResponse;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
public class RecurringPatternDetector {
    private static final Set<String> SUPPORTED = Set.of(
            "CUT_WINNER_EARLY",
            "REVENGE_TRADE",
            "FOMO_ENTRY",
            "OVERTRADING",
            "MOVED_STOP",
            "IGNORED_BIAS",
            "OVERSIZED_POSITION",
            "TRADED_OUTSIDE_SESSION"
    );
    private final TradingMemoryProperties properties;
    private final TradeMemoryNormalizer normalizer;

    public RecurringPatternDetector(TradingMemoryProperties properties, TradeMemoryNormalizer normalizer) {
        this.properties = properties;
        this.normalizer = normalizer;
    }

    public RecurringPatternResponse detect(List<SimilarTradeResponse> similarTrades) {
        int sampleSize = similarTrades == null ? 0 : similarTrades.size();
        if (sampleSize < properties.getMinPatternSample()) {
            return RecurringPatternResponse.none(sampleSize);
        }
        Map.Entry<String, Long> mostCommon = similarTrades.stream()
                .map(SimilarTradeResponse::mistake)
                .filter(Objects::nonNull)
                .filter(SUPPORTED::contains)
                .collect(Collectors.groupingBy(Function.identity(), Collectors.counting()))
                .entrySet()
                .stream()
                .max(Comparator.comparingLong(Map.Entry<String, Long>::getValue))
                .orElse(null);
        if (mostCommon == null || mostCommon.getValue() < properties.getMinPatternOccurrences()) {
            return RecurringPatternResponse.none(sampleSize);
        }
        BigDecimal occurrenceRate = BigDecimal.valueOf(mostCommon.getValue())
                .multiply(BigDecimal.valueOf(100))
                .divide(BigDecimal.valueOf(sampleSize), 2, RoundingMode.HALF_UP);
        if (occurrenceRate.compareTo(BigDecimal.valueOf(60)) < 0) {
            return RecurringPatternResponse.none(sampleSize);
        }
        List<SimilarTradeResponse> supporting = similarTrades.stream().filter(t -> mostCommon.getKey().equals(t.mistake())).toList();
        BigDecimal avgRealized = average(supporting.stream().map(SimilarTradeResponse::resultR).filter(Objects::nonNull).toList());
        BigDecimal avgMfe = average(supporting.stream().map(SimilarTradeResponse::mfeR).filter(Objects::nonNull).toList());
        List<BigDecimal> opportunities = supporting.stream()
                .filter(t -> "CUT_WINNER_EARLY".equals(t.mistake()))
                .filter(t -> t.resultR() != null && t.mfeR() != null)
                .map(t -> t.mfeR().subtract(t.resultR()).max(BigDecimal.ZERO))
                .toList();
        BigDecimal avgOpportunity = average(opportunities);
        BigDecimal totalOpportunity = opportunities.isEmpty() ? null : opportunities.stream().reduce(BigDecimal.ZERO, BigDecimal::add);
        String label = normalizer.label(mostCommon.getKey());
        return new RecurringPatternResponse(
                true,
                mostCommon.getKey(),
                label,
                mostCommon.getValue().intValue(),
                sampleSize,
                occurrenceRate,
                avgRealized,
                avgMfe,
                avgOpportunity,
                totalOpportunity,
                "You repeated " + label.toLowerCase() + " in " + mostCommon.getValue() + " of " + sampleSize + " comparable trades.",
                supporting.stream().map(SimilarTradeResponse::tradeId).toList()
        );
    }

    private BigDecimal average(List<BigDecimal> values) {
        if (values == null || values.isEmpty()) return null;
        BigDecimal sum = values.stream().reduce(BigDecimal.ZERO, BigDecimal::add);
        return sum.divide(BigDecimal.valueOf(values.size()), 4, RoundingMode.HALF_UP);
    }
}
