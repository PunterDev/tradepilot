package com.tradepilot.ai.memory.service;

import com.tradepilot.ai.memory.config.TradingMemoryProperties;
import com.tradepilot.ai.memory.entity.EmbeddingStatus;
import com.tradepilot.ai.memory.entity.TradeMemory;
import com.tradepilot.ai.memory.entity.TradeMemoryErrorCode;
import com.tradepilot.ai.memory.repository.TradeMemoryRepository;
import com.tradepilot.ai.memory.vector.TradeEmbeddingService;
import com.tradepilot.ai.memory.vector.TradingMemoryVectorClient;
import com.tradepilot.ai.memory.vector.model.QdrantCollectionValidationResult;
import com.tradepilot.ai.memory.vector.model.TradeMemoryVectorPayload;
import com.tradepilot.config.FeatureFlagService;
import com.tradepilot.billing.Feature;
import com.tradepilot.entity.AiTradeReview;
import com.tradepilot.entity.Trade;
import com.tradepilot.exception.ResourceNotFoundException;
import com.tradepilot.featureusage.FeatureKey;
import com.tradepilot.featureusage.FeatureUsageAction;
import com.tradepilot.featureusage.FeatureUsageEventType;
import com.tradepilot.featureusage.FeatureUsageService;
import com.tradepilot.repository.AiTradeReviewRepository;
import com.tradepilot.repository.TradeRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

@Service
public class TradeMemoryIndexService {
    private static final Logger log = LoggerFactory.getLogger(TradeMemoryIndexService.class);
    private final TradingMemoryProperties properties;
    private final FeatureFlagService featureFlagService;
    private final TradeRepository tradeRepository;
    private final AiTradeReviewRepository reviewRepository;
    private final TradeMemoryRepository memoryRepository;
    private final TradeMemoryBuilder builder;
    private final TradeEmbeddingService embeddingService;
    private final TradingMemoryVectorClient vectorClient;
    private final FeatureUsageService featureUsageService;
    private final TradeMemoryErrorClassifier errorClassifier;

    public TradeMemoryIndexService(TradingMemoryProperties properties, FeatureFlagService featureFlagService, TradeRepository tradeRepository, AiTradeReviewRepository reviewRepository, TradeMemoryRepository memoryRepository, TradeMemoryBuilder builder, TradeEmbeddingService embeddingService, TradingMemoryVectorClient vectorClient, FeatureUsageService featureUsageService, TradeMemoryErrorClassifier errorClassifier) {
        this.properties = properties;
        this.featureFlagService = featureFlagService;
        this.tradeRepository = tradeRepository;
        this.reviewRepository = reviewRepository;
        this.memoryRepository = memoryRepository;
        this.builder = builder;
        this.embeddingService = embeddingService;
        this.vectorClient = vectorClient;
        this.featureUsageService = featureUsageService;
        this.errorClassifier = errorClassifier;
    }

    @Async("aiTaskExecutor")
    public void indexAfterReviewAsync(Long userId, Long tradeId) {
        try {
            indexTradeMemory(userId, tradeId, false);
        } catch (RuntimeException ex) {
            log.warn("Trading Memory async index failed. userId={}, tradeId={}, reason={}", userId, tradeId, ex.getClass().getSimpleName());
        }
    }

    @Transactional
    public TradeMemory indexTradeMemory(Long userId, Long tradeId, boolean force) {
        if (!properties.isEnabled() || !featureFlagService.isEnabled(Feature.TRADING_MEMORY)) {
            return null;
        }
        Trade trade = tradeRepository.findByIdAndUser_Id(tradeId, userId).orElseThrow(() -> new ResourceNotFoundException("Trade not found"));
        AiTradeReview review = reviewRepository.findTopByUserIdAndTradeIdAndStatusOrderByCreatedAtDesc(userId, tradeId, "COMPLETED").orElse(null);
        TradeMemoryDocument document = builder.build(trade, review);
        TradeMemory memory = memoryRepository.findByTradeIdAndUserId(tradeId, userId).orElseGet(TradeMemory::new);
        boolean existing = memory.getId() != null;
        boolean unchanged = existing && !force
                && document.contentHash().equals(memory.getContentHash())
                && Integer.valueOf(document.memoryVersion()).equals(memory.getMemoryVersion())
                && memory.getEmbeddingStatus() == EmbeddingStatus.INDEXED;
        if (unchanged) {
            log.info("Trading Memory index skipped unchanged. userId={}, tradeId={}", userId, tradeId);
            return memory;
        }
        if (memory.getVectorPointId() == null) memory.setVectorPointId(UUID.randomUUID());
        memory.setTradeId(tradeId);
        memory.setUserId(userId);
        memory.setAccountId(trade.getAccount() == null ? null : trade.getAccount().getId());
        memory.setMemoryVersion(document.memoryVersion());
        memory.setSummary(document.summary());
        memory.setSetupNormalized(document.setupNormalized());
        memory.setEmotionNormalized(document.emotionNormalized());
        memory.setMistakeNormalized(document.mistakeNormalized());
        memory.setSetupSource(document.setupSource());
        memory.setEmotionSource(document.emotionSource());
        memory.setMistakeSource(document.mistakeSource());
        memory.setEntryReasonSummary(trade.getNotes());
        memory.setExitReasonSummary(null);
        memory.setMarketContextSummary(null);
        memory.setSourceReviewId(review == null ? null : review.getId());
        memory.setContentHash(document.contentHash());
        memory.setEmbeddingStatus(EmbeddingStatus.PENDING);
        memory.setLastError(null);
        memory = memoryRepository.saveAndFlush(memory);
        try {
            OffsetDateTime attemptedAt = OffsetDateTime.now();
            memory.setLastAttemptedAt(attemptedAt);
            memory.setAttemptCount((memory.getAttemptCount() == null ? 0 : memory.getAttemptCount()) + 1);
            QdrantCollectionValidationResult validation = vectorClient.validateCollection();
            if (!validation.operational()) {
                throw new IllegalStateException(String.join("; ", validation.errors()));
            }
            List<Double> vector = embeddingService.createEmbedding(document.content());
            vectorClient.upsert(memory.getVectorPointId(), vector, payload(memory, trade));
            memory.setEmbeddingStatus(EmbeddingStatus.INDEXED);
            memory.setEmbeddedAt(OffsetDateTime.now());
            memory.setLastError(null);
            memory.setLastErrorCode(null);
            memory.setLastErrorMessage(null);
            memory.setNextRetryAt(null);
            featureUsageService.track(trade.getUser(), FeatureKey.TRADING_MEMORY, FeatureUsageAction.TRADING_MEMORY_INDEX_SUCCEEDED, FeatureUsageEventType.USER_ACTION, trade.getAccount() == null ? null : trade.getAccount().getId(), null);
            log.info("Trading Memory indexed. userId={}, tradeId={}, memoryId={}", userId, tradeId, memory.getId());
        } catch (RuntimeException ex) {
            TradeMemoryErrorCode code = errorClassifier.classify(ex);
            memory.setEmbeddingStatus(EmbeddingStatus.FAILED);
            memory.setLastError(ex.getClass().getSimpleName());
            memory.setLastErrorCode(code);
            memory.setLastErrorMessage(errorClassifier.sanitize(ex.getMessage()));
            memory.setNextRetryAt(nextRetryAt(memory, code));
            featureUsageService.track(trade.getUser(), FeatureKey.TRADING_MEMORY, FeatureUsageAction.TRADING_MEMORY_INDEX_FAILED, FeatureUsageEventType.USER_ACTION, trade.getAccount() == null ? null : trade.getAccount().getId(), null);
            log.warn("Trading Memory indexing failed. userId={}, tradeId={}, code={}, reason={}", userId, tradeId, code, ex.getClass().getSimpleName());
        }
        return memoryRepository.save(memory);
    }

    @Transactional
    public void markStale(Long userId, Long tradeId) {
        memoryRepository.findByTradeIdAndUserId(tradeId, userId).ifPresent(memory -> {
            memory.setEmbeddingStatus(EmbeddingStatus.STALE);
            memory.setNextRetryAt(OffsetDateTime.now());
            memoryRepository.save(memory);
        });
    }

    @Transactional
    public void deleteMemory(Long userId, Long tradeId) {
        memoryRepository.findByTradeIdAndUserId(tradeId, userId).ifPresent(memory -> {
            try {
                vectorClient.delete(memory.getVectorPointId());
            } catch (RuntimeException ex) {
                log.warn("Trading Memory vector delete failed. userId={}, tradeId={}, reason={}", userId, tradeId, ex.getClass().getSimpleName());
            }
            memoryRepository.delete(memory);
        });
    }

    @Transactional
    public void deleteUserMemories(Long userId) {
        if (userId == null) return;
        try {
            vectorClient.deleteByUserId(userId);
        } catch (RuntimeException ex) {
            log.warn("Trading Memory user vector delete failed. userId={}, reason={}", userId, ex.getClass().getSimpleName());
        }
        memoryRepository.deleteByUserId(userId);
    }

    @Transactional
    public void deleteAccountMemories(Long userId, Long accountId) {
        if (userId == null || accountId == null) return;
        try {
            vectorClient.deleteByUserIdAndAccountId(userId, accountId);
        } catch (RuntimeException ex) {
            log.warn("Trading Memory account vector delete failed. userId={}, accountId={}, reason={}", userId, accountId, ex.getClass().getSimpleName());
        }
        memoryRepository.deleteByUserIdAndAccountId(userId, accountId);
    }

    private OffsetDateTime nextRetryAt(TradeMemory memory, TradeMemoryErrorCode code) {
        if (!properties.isRetryEnabled() || !errorClassifier.retryable(code)) return null;
        int attempts = memory.getAttemptCount() == null ? 0 : memory.getAttemptCount();
        if (attempts >= properties.getRetryMaxAttempts()) {
            log.warn("trading_memory_index_retry_exhausted memoryId={}, tradeId={}, attempts={}, code={}", memory.getId(), memory.getTradeId(), attempts, code);
            return null;
        }
        long delay = Math.min(
                properties.getRetryMaxDelaySeconds(),
                Math.round(properties.getRetryInitialDelaySeconds() * Math.pow(2, Math.max(0, attempts - 1)))
        );
        return OffsetDateTime.now().plusSeconds(delay);
    }

    private TradeMemoryVectorPayload payload(TradeMemory memory, Trade trade) {
        return new TradeMemoryVectorPayload(
                memory.getVectorPointId(),
                memory.getTradeId(),
                memory.getUserId(),
                memory.getAccountId(),
                trade.getSymbol(),
                trade.getSide(),
                memory.getSetupNormalized(),
                memory.getMistakeNormalized(),
                memory.getEmotionNormalized(),
                trade.getSession() == null ? null : trade.getSession().name(),
                trade.getTradeDate(),
                memory.getMemoryVersion()
        );
    }
}
