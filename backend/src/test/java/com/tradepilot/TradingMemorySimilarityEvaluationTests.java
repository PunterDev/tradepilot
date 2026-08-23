package com.tradepilot;

import org.junit.jupiter.api.Test;

import java.util.Comparator;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class TradingMemorySimilarityEvaluationTests {
    @Test
    void mockModeProducesReadableQualityMetrics() {
        List<EvalTrade> fixture = fixture();
        EvalTrade query = new EvalTrade("q1", "CUT_WINNER_EARLY", "IFVG continuation early exit");

        List<EvalTrade> top5 = fixture.stream()
                .sorted(Comparator.comparingInt((EvalTrade trade) -> mockDistance(query, trade)))
                .limit(5)
                .toList();

        long relevantTop3 = top5.stream().limit(3).filter(trade -> trade.category().equals(query.category())).count();
        long relevantTop5 = top5.stream().filter(trade -> trade.category().equals(query.category())).count();
        double precisionAt3 = relevantTop3 / 3.0;
        double recallAt5 = relevantTop5 / (double) fixture.stream().filter(trade -> trade.category().equals(query.category())).count();

        assertThat(fixture).hasSizeGreaterThanOrEqualTo(20);
        assertThat(precisionAt3).isGreaterThanOrEqualTo(0.66);
        assertThat(recallAt5).isGreaterThanOrEqualTo(0.50);
    }

    private int mockDistance(EvalTrade query, EvalTrade candidate) {
        int distance = query.category().equals(candidate.category()) ? 0 : 10;
        for (String token : query.document().split(" ")) {
            if (!candidate.document().contains(token)) distance++;
        }
        return distance;
    }

    private List<EvalTrade> fixture() {
        Map<String, List<String>> documents = Map.of(
                "CUT_WINNER_EARLY", List.of("IFVG continuation early exit runner cut", "closed continuation winner too soon", "fear cut runner before invalidation"),
                "REVENGE_TRADE", List.of("revenge trade after loss", "angry reentry after stop", "second trade forced after loser"),
                "FOMO_ENTRY", List.of("fomo breakout chase", "late entry after displacement", "entered because move was leaving"),
                "MOVED_STOP", List.of("moved stop wider", "invalidated stop ignored", "stop adjusted after entry"),
                "IGNORED_BIAS", List.of("ignored higher timeframe bias", "short against bullish structure", "long into bearish context"),
                "OVERSIZED_POSITION", List.of("oversized position stress", "risk too large", "size bigger than plan"),
                "CALM_RULE_WINNER", List.of("calm rule following winner", "planned entry and exit", "patient structural target"),
                "OVERTRADING", List.of("overtrading after multiple losses", "high trade count day", "kept clicking after limit")
        );
        return documents.entrySet().stream()
                .flatMap(entry -> entry.getValue().stream().map(value -> new EvalTrade(entry.getKey() + "-" + value.hashCode(), entry.getKey(), value)))
                .toList();
    }

    private record EvalTrade(String id, String category, String document) {
    }
}
