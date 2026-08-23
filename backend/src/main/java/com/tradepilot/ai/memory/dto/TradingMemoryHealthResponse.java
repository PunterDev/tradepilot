package com.tradepilot.ai.memory.dto;

import com.tradepilot.ai.memory.vector.model.QdrantCollectionValidationResult;

import java.time.OffsetDateTime;
import java.util.Map;

public record TradingMemoryHealthResponse(
        boolean featureEnabled,
        boolean v2Enabled,
        boolean memoryEnabled,
        boolean operational,
        QdrantCollectionValidationResult qdrant,
        Map<String, Object> embeddingProvider,
        Map<String, Long> memoryCounts,
        OffsetDateTime oldestPendingAt,
        OffsetDateTime lastSuccessfulIndexAt
) {
}
