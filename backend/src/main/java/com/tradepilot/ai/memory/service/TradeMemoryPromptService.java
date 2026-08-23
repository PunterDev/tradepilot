package com.tradepilot.ai.memory.service;

import com.tradepilot.ai.memory.dto.RecurringPatternResponse;
import com.tradepilot.ai.memory.dto.SimilarTradeResponse;
import com.tradepilot.ai.memory.dto.TradingMemoryAiInsightResponse;
import com.tradepilot.ai.memory.dto.TradingMemoryInsightSource;
import com.tradepilot.ai.memory.dto.TradingMemoryMetricsResponse;
import com.tradepilot.entity.Trade;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class TradeMemoryPromptService {
    public TradingMemoryAiInsightResponse buildDeterministicInsight(Trade currentTrade, TradingMemoryMetricsResponse metrics, RecurringPatternResponse pattern, List<SimilarTradeResponse> similarTrades) {
        if (similarTrades == null || similarTrades.isEmpty()) return null;
        if (pattern != null && pattern.detected()) {
            return new TradingMemoryAiInsightResponse(
                    TradingMemoryInsightSource.DETERMINISTIC,
                    "This trade matches a recurring " + pattern.title() + " pattern.",
                    "TradePilot found " + pattern.occurrences() + " comparable trades in a " + pattern.sampleSize() + "-trade sample with the same normalized mistake. These counts come from saved trade history, not vector similarity alone.",
                    "Review the supporting trades and write one execution rule before taking a similar setup again."
            );
        }
        return new TradingMemoryAiInsightResponse(
                TradingMemoryInsightSource.DETERMINISTIC,
                "Similar reviewed trades are available for comparison.",
                "The comparable cohort contains " + metrics.sampleSize() + " trades with average result " + display(metrics.averageResultR()) + ". No recurring mistake crossed the configured evidence threshold.",
                "Use the supporting trades as context, but avoid drawing strong conclusions until the sample grows."
        );
    }

    private String display(java.math.BigDecimal value) {
        return value == null ? "not available" : value.stripTrailingZeros().toPlainString() + "R";
    }
}
