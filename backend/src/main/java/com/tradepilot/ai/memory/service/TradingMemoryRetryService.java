package com.tradepilot.ai.memory.service;

import com.tradepilot.ai.memory.config.TradingMemoryProperties;
import com.tradepilot.ai.memory.dto.TradingMemoryRetryRequest;
import com.tradepilot.ai.memory.dto.TradingMemoryRetryResponse;
import com.tradepilot.ai.memory.entity.EmbeddingStatus;
import com.tradepilot.ai.memory.entity.TradeMemory;
import com.tradepilot.ai.memory.entity.TradeMemoryErrorCode;
import com.tradepilot.ai.memory.repository.TradeMemoryRepository;
import com.tradepilot.featureusage.FeatureKey;
import com.tradepilot.featureusage.FeatureUsageAction;
import com.tradepilot.featureusage.FeatureUsageEventType;
import com.tradepilot.featureusage.FeatureUsageService;
import com.tradepilot.repository.UserRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;

import java.time.OffsetDateTime;
import java.util.EnumSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

@Service
public class TradingMemoryRetryService {
    private static final Logger log = LoggerFactory.getLogger(TradingMemoryRetryService.class);
    private final TradingMemoryProperties properties;
    private final TradeMemoryRepository memoryRepository;
    private final TradeMemoryIndexService indexService;
    private final TradeMemoryErrorClassifier errorClassifier;
    private final UserRepository userRepository;
    private final FeatureUsageService featureUsageService;

    public TradingMemoryRetryService(TradingMemoryProperties properties, TradeMemoryRepository memoryRepository, TradeMemoryIndexService indexService, TradeMemoryErrorClassifier errorClassifier, UserRepository userRepository, FeatureUsageService featureUsageService) {
        this.properties = properties;
        this.memoryRepository = memoryRepository;
        this.indexService = indexService;
        this.errorClassifier = errorClassifier;
        this.userRepository = userRepository;
        this.featureUsageService = featureUsageService;
    }

    public TradingMemoryRetryResponse retryFailed(TradingMemoryRetryRequest request) {
        boolean dryRun = request == null || !Boolean.FALSE.equals(request.dryRun());
        int limit = request == null || request.limit() == null ? properties.getRetryBatchSize() : Math.min(Math.max(request.limit(), 1), Math.max(1, properties.getRetryBatchSize()));
        Set<TradeMemoryErrorCode> allowed = errorCodes(request == null ? null : request.errorCodes());
        List<TradeMemory> candidates = memoryRepository.findByEmbeddingStatusAndNextRetryAtLessThanEqualOrderByUpdatedAtAsc(EmbeddingStatus.FAILED, OffsetDateTime.now(), PageRequest.of(0, limit));
        long eligible = 0;
        long retried = 0;
        long succeeded = 0;
        long failed = 0;
        long skipped = 0;
        for (TradeMemory memory : candidates) {
            if (!eligible(memory, allowed)) {
                skipped++;
                continue;
            }
            eligible++;
            if (dryRun) continue;
            retried++;
            TradeMemory updated = indexService.indexTradeMemory(memory.getUserId(), memory.getTradeId(), true);
            if (updated != null && updated.getEmbeddingStatus() == EmbeddingStatus.INDEXED) succeeded++;
            else failed++;
        }
        if (!dryRun) {
            log.info("trading_memory_index_retry_scheduled eligible={}, retried={}, succeeded={}, failed={}", eligible, retried, succeeded, failed);
        }
        return new TradingMemoryRetryResponse(dryRun, eligible, retried, succeeded, failed, skipped);
    }

    public TradingMemoryRetryResponse retryOne(UUID memoryId) {
        TradeMemory memory = memoryRepository.findById(memoryId).orElseThrow(() -> new IllegalArgumentException("Trade memory not found."));
        if (!eligible(memory, EnumSet.allOf(TradeMemoryErrorCode.class))) {
            return new TradingMemoryRetryResponse(false, 0, 0, 0, 0, 1);
        }
        TradeMemory updated = indexService.indexTradeMemory(memory.getUserId(), memory.getTradeId(), true);
        userRepository.findById(memory.getUserId()).ifPresent(user -> featureUsageService.track(user, FeatureKey.TRADING_MEMORY, FeatureUsageAction.TRADING_MEMORY_REGENERATED, FeatureUsageEventType.USER_ACTION, null, null));
        boolean ok = updated != null && updated.getEmbeddingStatus() == EmbeddingStatus.INDEXED;
        return new TradingMemoryRetryResponse(false, 1, 1, ok ? 1 : 0, ok ? 0 : 1, 0);
    }

    private boolean eligible(TradeMemory memory, Set<TradeMemoryErrorCode> allowed) {
        if (memory.getEmbeddingStatus() != EmbeddingStatus.FAILED) return false;
        if ((memory.getAttemptCount() == null ? 0 : memory.getAttemptCount()) >= properties.getRetryMaxAttempts()) return false;
        if (memory.getLastErrorCode() != null && !errorClassifier.retryable(memory.getLastErrorCode())) return false;
        return memory.getLastErrorCode() == null || allowed.contains(memory.getLastErrorCode());
    }

    private Set<TradeMemoryErrorCode> errorCodes(List<String> raw) {
        if (raw == null || raw.isEmpty()) return EnumSet.allOf(TradeMemoryErrorCode.class);
        EnumSet<TradeMemoryErrorCode> codes = EnumSet.noneOf(TradeMemoryErrorCode.class);
        for (String value : raw) {
            try {
                codes.add(TradeMemoryErrorCode.valueOf(value));
            } catch (RuntimeException ignored) {
            }
        }
        return codes.isEmpty() ? EnumSet.allOf(TradeMemoryErrorCode.class) : codes;
    }
}
