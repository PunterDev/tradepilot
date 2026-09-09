package com.tradepilot.ai.memory.vector;

import com.tradepilot.ai.memory.vector.model.TradeMemoryVectorPayload;
import com.tradepilot.ai.memory.vector.model.QdrantCollectionValidationResult;
import com.tradepilot.ai.memory.vector.model.TradingMemorySearchRequest;
import com.tradepilot.ai.memory.vector.model.VectorSearchResult;

import java.util.List;
import java.util.UUID;

public interface TradingMemoryVectorClient {
    boolean isAvailable();
    void ensureCollection();
    QdrantCollectionValidationResult validateCollection();
    void upsert(UUID pointId, List<Double> vector, TradeMemoryVectorPayload payload);
    List<VectorSearchResult> search(Long userId, List<Double> queryVector, Long excludeTradeId, int limit);
    List<VectorSearchResult> search(TradingMemorySearchRequest request);
    void delete(UUID pointId);
    default void deleteByUserId(Long userId) {
    }
    default void deleteByUserIdAndAccountId(Long userId, Long accountId) {
    }
}
