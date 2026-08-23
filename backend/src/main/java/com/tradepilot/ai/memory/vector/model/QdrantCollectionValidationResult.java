package com.tradepilot.ai.memory.vector.model;

import java.time.OffsetDateTime;
import java.util.List;

public record QdrantCollectionValidationResult(
        boolean reachable,
        boolean collectionExists,
        boolean vectorSizeValid,
        boolean distanceMetricValid,
        boolean payloadIndexesValid,
        boolean operational,
        List<String> errors,
        OffsetDateTime checkedAt
) {
    public static QdrantCollectionValidationResult unavailable(String message) {
        return new QdrantCollectionValidationResult(false, false, false, false, false, false, List.of(message), OffsetDateTime.now());
    }
}
