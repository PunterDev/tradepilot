package com.tradepilot.ai.memory.vector.model;

public record VectorSearchResult(
        Long tradeId,
        double score
) {
}
