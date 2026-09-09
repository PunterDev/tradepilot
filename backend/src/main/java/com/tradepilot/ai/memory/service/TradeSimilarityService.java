package com.tradepilot.ai.memory.service;

import com.tradepilot.ai.memory.config.TradingMemoryProperties;
import com.tradepilot.ai.memory.dto.*;
import com.tradepilot.ai.memory.entity.EmbeddingStatus;
import com.tradepilot.ai.memory.entity.TradeMemory;
import com.tradepilot.ai.memory.repository.TradeMemoryRepository;
import com.tradepilot.ai.memory.vector.TradeEmbeddingService;
import com.tradepilot.ai.memory.vector.TradingMemoryVectorClient;
import com.tradepilot.ai.memory.vector.model.TradingMemorySearchRequest;
import com.tradepilot.ai.memory.vector.model.VectorSearchResult;
import com.tradepilot.billing.Feature;
import com.tradepilot.billing.FeatureAccessService;
import com.tradepilot.config.FeatureFlagService;
import com.tradepilot.entity.Trade;
import com.tradepilot.entity.User;
import com.tradepilot.exception.ResourceNotFoundException;
import com.tradepilot.featureusage.FeatureKey;
import com.tradepilot.featureusage.FeatureUsageAction;
import com.tradepilot.featureusage.FeatureUsageEventType;
import com.tradepilot.featureusage.FeatureUsageService;
import com.tradepilot.repository.TradeRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
public class TradeSimilarityService {
    private static final Logger log = LoggerFactory.getLogger(TradeSimilarityService.class);
    private final TradingMemoryProperties properties;
    private final FeatureFlagService featureFlagService;
    private final FeatureAccessService featureAccessService;
    private final TradeRepository tradeRepository;
    private final TradeMemoryRepository memoryRepository;
    private final TradeMemoryIndexService indexService;
    private final TradeEmbeddingService embeddingService;
    private final TradingMemoryVectorClient vectorClient;
    private final TradeMemoryAnalyticsService analyticsService;
    private final RecurringPatternDetector patternDetector;
    private final TradeMemoryPromptService promptService;
    private final TradeMemoryNormalizer normalizer;
    private final FeatureUsageService featureUsageService;

    public TradeSimilarityService(TradingMemoryProperties properties, FeatureFlagService featureFlagService, FeatureAccessService featureAccessService, TradeRepository tradeRepository, TradeMemoryRepository memoryRepository, TradeMemoryIndexService indexService, TradeEmbeddingService embeddingService, TradingMemoryVectorClient vectorClient, TradeMemoryAnalyticsService analyticsService, RecurringPatternDetector patternDetector, TradeMemoryPromptService promptService, TradeMemoryNormalizer normalizer, FeatureUsageService featureUsageService) {
        this.properties = properties;
        this.featureFlagService = featureFlagService;
        this.featureAccessService = featureAccessService;
        this.tradeRepository = tradeRepository;
        this.memoryRepository = memoryRepository;
        this.indexService = indexService;
        this.embeddingService = embeddingService;
        this.vectorClient = vectorClient;
        this.analyticsService = analyticsService;
        this.patternDetector = patternDetector;
        this.promptService = promptService;
        this.normalizer = normalizer;
        this.featureUsageService = featureUsageService;
    }

    @Transactional(readOnly = true)
    public TradingMemoryResponse getTradingMemory(User user, Long tradeId, Integer limit) {
        return getTradingMemory(user, tradeId, limit, false, false, false, null);
    }

    @Transactional(readOnly = true)
    public TradingMemoryResponse getTradingMemory(User user, Long tradeId, Integer limit, boolean sameSetup, boolean sameInstrument, boolean sameDirection, Integer lookbackDays) {
        if (!properties.isEnabled() || !featureFlagService.isEnabled(Feature.TRADING_MEMORY)) {
            return disabled(tradeId);
        }
        featureAccessService.requireFeature(user, Feature.TRADING_MEMORY);
        Trade currentTrade = tradeRepository.findByIdAndUser_Id(tradeId, user.getId()).orElseThrow(() -> new ResourceNotFoundException("Trade not found"));
        TradeMemory memory = memoryRepository.findByTradeIdAndUserId(tradeId, user.getId()).orElse(null);
        if (memory == null || memory.getEmbeddingStatus() == EmbeddingStatus.STALE) {
            return new TradingMemoryResponse(tradeId, TradingMemoryStatus.PENDING, null, 0, null, null, null, List.of(), null, "Building this trade's memory.", false);
        }
        if (memory.getEmbeddingStatus() == EmbeddingStatus.FAILED) {
            return new TradingMemoryResponse(tradeId, TradingMemoryStatus.FAILED, memory.getEmbeddedAt(), 0, null, null, null, List.of(), null, "Trading Memory is temporarily unavailable.", false);
        }
        if (memory.getEmbeddingStatus() != EmbeddingStatus.INDEXED) {
            return new TradingMemoryResponse(tradeId, TradingMemoryStatus.PENDING, null, 0, null, null, null, List.of(), null, "Building this trade's memory.", false);
        }
        if (!vectorClient.validateCollection().operational()) {
            return new TradingMemoryResponse(tradeId, TradingMemoryStatus.NOT_AVAILABLE, memory.getEmbeddedAt(), 0, null, null, null, List.of(), null, "Trading Memory is temporarily unavailable.", false);
        }
        try {
            int safeLimit = safeLimit(limit);
            List<Double> queryVector = embeddingService.createEmbedding(memory.getSummary());
            List<VectorSearchResult> candidates = vectorClient.search(searchRequest(user, currentTrade, queryVector, tradeId, safeLimit, sameSetup, sameInstrument, sameDirection, lookbackDays));
            List<VectorSearchResult> filtered = candidates.stream()
                    .filter(result -> result.score() >= properties.getSimilarityThreshold())
                    .filter(result -> !Objects.equals(result.tradeId(), tradeId))
                    .toList();
            List<Long> ids = filtered.stream().map(VectorSearchResult::tradeId).distinct().toList();
            Map<Long, Trade> tradesById = tradeRepository.findByUser_IdAndIdIn(user.getId(), ids).stream().collect(Collectors.toMap(Trade::getId, Function.identity()));
            List<TradeMemory> hydratedMemories = currentTrade.getAccount() == null
                    ? memoryRepository.findByUserIdAndTradeIdIn(user.getId(), ids)
                    : memoryRepository.findByUserIdAndAccountIdAndTradeIdIn(user.getId(), currentTrade.getAccount().getId(), ids);
            Map<Long, TradeMemory> memoriesByTradeId = hydratedMemories.stream().collect(Collectors.toMap(TradeMemory::getTradeId, Function.identity()));
            int removedDuringHydration = ids.size() - tradesById.size();
            if (removedDuringHydration > 0) {
                log.warn("trading_memory_cross_user_candidate_removed userId={}, tradeId={}, removed={}", user.getId(), tradeId, removedDuringHydration);
            }
            Map<Long, Double> scores = filtered.stream().collect(Collectors.toMap(VectorSearchResult::tradeId, VectorSearchResult::score, Math::max));
            List<SimilarTradeResponse> similar = ids.stream()
                    .map(id -> toSimilarTrade(tradesById.get(id), memoriesByTradeId.get(id), scores.get(id)))
                    .filter(Objects::nonNull)
                    .filter(candidate -> matchesRequestedFilters(currentTrade, candidate, sameSetup, sameInstrument, sameDirection, lookbackDays))
                    .limit(safeLimit)
                    .toList();
            if (similar.isEmpty()) {
                return new TradingMemoryResponse(tradeId, TradingMemoryStatus.NOT_ENOUGH_DATA, memory.getEmbeddedAt(), 0, null, null, null, List.of(), null, "No strong historical matches were found for this trade.", false);
            }
            TradingMemoryMetricsResponse metrics = analyticsService.calculate(similar);
            RecurringPatternResponse pattern = patternDetector.detect(similar);
            TradingMemoryAiInsightResponse insight = promptService.buildDeterministicInsight(currentTrade, metrics, pattern, similar);
            if (pattern.detected()) {
                featureUsageService.track(user, FeatureKey.TRADING_MEMORY, FeatureUsageAction.TRADING_MEMORY_PATTERN_DETECTED, FeatureUsageEventType.USER_ACTION, currentTrade.getAccount() == null ? null : currentTrade.getAccount().getId(), null);
            }
            featureUsageService.track(user, FeatureKey.TRADING_MEMORY, FeatureUsageAction.TRADING_MEMORY_SEARCH_COMPLETED, FeatureUsageEventType.USER_ACTION, currentTrade.getAccount() == null ? null : currentTrade.getAccount().getId(), null);
            return new TradingMemoryResponse(tradeId, TradingMemoryStatus.READY, memory.getEmbeddedAt(), similar.size(), metrics, analyticsService.commonBehavior(metrics, normalizer), pattern, similar, insight, null, true);
        } catch (RuntimeException ex) {
            log.warn("Trading Memory retrieval failed. userId={}, tradeId={}, reason={}", user.getId(), tradeId, ex.getClass().getSimpleName());
            return new TradingMemoryResponse(tradeId, TradingMemoryStatus.NOT_AVAILABLE, memory.getEmbeddedAt(), 0, null, null, null, List.of(), null, "Trading Memory is temporarily unavailable. Your trade review is still available.", false);
        }
    }

    public TradeMemoryStatusResponse regenerate(User user, Long tradeId, boolean force) {
        featureFlagService.requireEnabled(Feature.TRADING_MEMORY);
        featureAccessService.requireFeature(user, Feature.TRADING_MEMORY);
        TradeMemory memory = indexService.indexTradeMemory(user.getId(), tradeId, force);
        featureUsageService.track(user, FeatureKey.TRADING_MEMORY, FeatureUsageAction.TRADING_MEMORY_REGENERATED, FeatureUsageEventType.USER_ACTION, null, null);
        if (memory == null) return new TradeMemoryStatusResponse(tradeId, TradingMemoryStatus.DISABLED, null, "Trading Memory is disabled.");
        TradingMemoryStatus status = statusFor(memory.getEmbeddingStatus());
        return new TradeMemoryStatusResponse(tradeId, status, memory.getEmbeddedAt(), status == TradingMemoryStatus.READY ? "Trading Memory is ready." : "Trading Memory is being prepared.");
    }

    public TradingMemoryResponse similarTrades(User user, Long tradeId, Integer limit) {
        return getTradingMemory(user, tradeId, limit);
    }

    public TradingMemoryResponse similarTrades(User user, Long tradeId, Integer limit, boolean sameSetup, boolean sameInstrument, boolean sameDirection, Integer lookbackDays) {
        return getTradingMemory(user, tradeId, limit, sameSetup, sameInstrument, sameDirection, lookbackDays);
    }

    private SimilarTradeResponse toSimilarTrade(Trade trade, TradeMemory memory, Double score) {
        if (trade == null || memory == null) return null;
        return new SimilarTradeResponse(
                trade.getId(),
                trade.getTradeDate(),
                trade.getSymbol(),
                trade.getSide(),
                memory.getSetupSource(),
                resultR(trade),
                null,
                null,
                memory.getMistakeNormalized(),
                memory.getEmotionNormalized(),
                score == null ? null : BigDecimal.valueOf(score).setScale(4, RoundingMode.HALF_UP).doubleValue(),
                memory.getSummary()
        );
    }

    private BigDecimal resultR(Trade trade) {
        if (trade.getPnlR() != null) return trade.getPnlR();
        BigDecimal risk = trade.getRiskAmount();
        if ((risk == null || risk.signum() <= 0) && trade.getAccount() != null) {
            risk = trade.getAccount().getDefaultRiskAmount();
        }
        if (risk == null || risk.signum() <= 0 || trade.getPnlAmount() == null) return null;
        return trade.getPnlAmount().divide(risk, 4, RoundingMode.HALF_UP);
    }

    private int safeLimit(Integer limit) {
        if (limit == null || limit <= 0) return properties.getDefaultLimit();
        return Math.min(limit, properties.getMaxLimit());
    }

    private TradingMemorySearchRequest searchRequest(User user, Trade currentTrade, List<Double> queryVector, Long tradeId, int safeLimit, boolean sameSetup, boolean sameInstrument, boolean sameDirection, Integer lookbackDays) {
        String setup = null;
        String instrument = null;
        String direction = null;
        java.time.LocalDate tradeDateFrom = null;
        if (sameSetup && currentTrade.getSetup() != null) setup = currentTrade.getSetup().name();
        if (sameInstrument) instrument = currentTrade.getSymbol();
        if (sameDirection) direction = currentTrade.getSide();
        if (lookbackDays != null && lookbackDays > 0 && currentTrade.getTradeDate() != null) {
            tradeDateFrom = currentTrade.getTradeDate().minusDays(lookbackDays);
        }
        Long accountId = currentTrade.getAccount() == null ? null : currentTrade.getAccount().getId();
        return new TradingMemorySearchRequest(user.getId(), queryVector, tradeId, Math.min(properties.getMaxLimit() * 3, safeLimit * 3), accountId, setup, instrument, direction, null, tradeDateFrom);
    }

    private boolean matchesRequestedFilters(Trade currentTrade, SimilarTradeResponse candidate, boolean sameSetup, boolean sameInstrument, boolean sameDirection, Integer lookbackDays) {
        if (sameInstrument && !Objects.equals(normalize(currentTrade.getSymbol()), normalize(candidate.instrument()))) {
            return false;
        }
        if (sameDirection && !Objects.equals(normalize(currentTrade.getSide()), normalize(candidate.direction()))) {
            return false;
        }
        if (sameSetup) {
            String currentSetup = currentTrade.getSetup() == null ? null : currentTrade.getSetup().name();
            if (!Objects.equals(normalize(currentSetup), normalize(candidate.setup()))) {
                return false;
            }
        }
        if (lookbackDays != null && lookbackDays > 0 && currentTrade.getTradeDate() != null && candidate.tradeDate() != null) {
            if (candidate.tradeDate().isBefore(currentTrade.getTradeDate().minusDays(lookbackDays))) {
                return false;
            }
        }
        return true;
    }

    private String normalize(String value) {
        return value == null ? null : value.trim().toUpperCase(Locale.ROOT);
    }

    private TradingMemoryStatus statusFor(EmbeddingStatus status) {
        if (status == EmbeddingStatus.INDEXED) {
            return TradingMemoryStatus.READY;
        }
        if (status == EmbeddingStatus.FAILED) {
            return TradingMemoryStatus.FAILED;
        }
        return TradingMemoryStatus.PENDING;
    }

    private TradingMemoryResponse disabled(Long tradeId) {
        return new TradingMemoryResponse(tradeId, TradingMemoryStatus.DISABLED, null, 0, null, null, null, List.of(), null, "Trading Memory is disabled.", false);
    }
}
