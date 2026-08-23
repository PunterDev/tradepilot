package com.tradepilot.ai.memory.dto;

public record TradingMemoryAiInsightResponse(
        TradingMemoryInsightSource source,
        String headline,
        String explanation,
        String lesson
) {
}
