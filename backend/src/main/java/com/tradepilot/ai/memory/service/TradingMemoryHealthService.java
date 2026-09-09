package com.tradepilot.ai.memory.service;

import com.tradepilot.ai.memory.config.TradingMemoryProperties;
import com.tradepilot.ai.memory.dto.TradingMemoryHealthResponse;
import com.tradepilot.ai.memory.entity.EmbeddingStatus;
import com.tradepilot.ai.memory.repository.TradeMemoryRepository;
import com.tradepilot.ai.memory.vector.TradingMemoryVectorClient;
import com.tradepilot.ai.memory.vector.model.QdrantCollectionValidationResult;
import com.tradepilot.billing.Feature;
import com.tradepilot.config.FeatureFlagService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.LinkedHashMap;
import java.util.Map;

@Service
public class TradingMemoryHealthService {
    private final TradingMemoryProperties properties;
    private final FeatureFlagService featureFlagService;
    private final TradingMemoryVectorClient vectorClient;
    private final TradeMemoryRepository memoryRepository;

    public TradingMemoryHealthService(TradingMemoryProperties properties, FeatureFlagService featureFlagService, TradingMemoryVectorClient vectorClient, TradeMemoryRepository memoryRepository) {
        this.properties = properties;
        this.featureFlagService = featureFlagService;
        this.vectorClient = vectorClient;
        this.memoryRepository = memoryRepository;
    }

    @Transactional(readOnly = true)
    public TradingMemoryHealthResponse health() {
        boolean v2Enabled = featureFlagService.isEnabled(Feature.TRADING_MEMORY);
        boolean memoryEnabled = properties.isEnabled();
        QdrantCollectionValidationResult qdrant = memoryEnabled && v2Enabled ? vectorClient.validateCollection() : QdrantCollectionValidationResult.unavailable("Trading Memory is disabled.");
        Map<String, Object> embedding = new LinkedHashMap<>();
        embedding.put("configured", properties.getEmbeddingModel() != null && !properties.getEmbeddingModel().isBlank());
        embedding.put("model", properties.getEmbeddingModel());
        embedding.put("expectedVectorSize", properties.getVectorSize());
        Map<String, Long> counts = new LinkedHashMap<>();
        for (EmbeddingStatus status : EmbeddingStatus.values()) {
            counts.put(status.name().toLowerCase(java.util.Locale.ROOT), memoryRepository.countByEmbeddingStatus(status));
        }
        return new TradingMemoryHealthResponse(
                memoryEnabled && v2Enabled,
                v2Enabled,
                memoryEnabled,
                memoryEnabled && v2Enabled && qdrant.operational(),
                qdrant,
                embedding,
                counts,
                memoryRepository.oldestByStatus(EmbeddingStatus.PENDING),
                memoryRepository.latestEmbeddedAtByStatus(EmbeddingStatus.INDEXED)
        );
    }
}
