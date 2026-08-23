package com.tradepilot.tradereplay.service;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.tradepilot.billing.Feature;
import com.tradepilot.config.FeatureFlagService;
import com.tradepilot.entity.Trade;
import com.tradepilot.entity.User;
import com.tradepilot.exception.ResourceNotFoundException;
import com.tradepilot.featureusage.FeatureKey;
import com.tradepilot.featureusage.FeatureUsageAction;
import com.tradepilot.featureusage.FeatureUsageEventType;
import com.tradepilot.featureusage.FeatureUsageService;
import com.tradepilot.repository.TradeRepository;
import com.tradepilot.tradereplay.dto.*;
import com.tradepilot.tradereplay.entity.*;
import com.tradepilot.tradereplay.repository.MarketCandleRepository;
import com.tradepilot.tradereplay.repository.TradeReplayInsightRepository;
import com.tradepilot.tradereplay.repository.TradeReplayRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import java.time.Duration;
import java.time.Instant;
import java.time.LocalDateTime;
import java.util.*;

@Service
public class TradeReplayService {
    private static final Logger log = LoggerFactory.getLogger(TradeReplayService.class);
    private static final String INTERVAL = "1m";
    private static final String ANALYSIS_VERSION = "trade-replay-deterministic-v1";
    private static final int MAX_RETURNED_CANDLES = 400;
    private final FeatureFlagService featureFlagService;
    private final MarketDataProperties properties;
    private final TradeRepository tradeRepository;
    private final TradeReplayRepository replayRepository;
    private final MarketCandleRepository candleRepository;
    private final TradeReplayInsightRepository insightRepository;
    private final FuturesContractResolver contractResolver;
    private final HistoricalMarketDataProviderRegistry providerRegistry;
    private final TradeReplayAnalysisEngine analysisEngine;
    private final ObjectMapper objectMapper;
    private final FeatureUsageService featureUsageService;
    private final FuturesContractSymbolNormalizer contractSymbolNormalizer;

    public TradeReplayService(FeatureFlagService featureFlagService, MarketDataProperties properties, TradeRepository tradeRepository, TradeReplayRepository replayRepository, MarketCandleRepository candleRepository, TradeReplayInsightRepository insightRepository, FuturesContractResolver contractResolver, HistoricalMarketDataProviderRegistry providerRegistry, TradeReplayAnalysisEngine analysisEngine, ObjectMapper objectMapper, FeatureUsageService featureUsageService, FuturesContractSymbolNormalizer contractSymbolNormalizer) {
        this.featureFlagService = featureFlagService;
        this.properties = properties;
        this.tradeRepository = tradeRepository;
        this.replayRepository = replayRepository;
        this.candleRepository = candleRepository;
        this.insightRepository = insightRepository;
        this.contractResolver = contractResolver;
        this.providerRegistry = providerRegistry;
        this.analysisEngine = analysisEngine;
        this.objectMapper = objectMapper;
        this.featureUsageService = featureUsageService;
        this.contractSymbolNormalizer = contractSymbolNormalizer;
    }

    @Transactional(readOnly = true)
    public TradeReplayResponse getReplay(User user, Long tradeId, boolean includeCandles) {
        requireEnabled();
        Trade trade = ownedTrade(user, tradeId);
        if (!DemoReplayEligibility.isReplayAllowed(trade)) {
            log.debug("Trade Replay demo eligibility. tradeId={}, externalTradeKey={}, replayCompatible=false, action=BLOCKED_BEFORE_PROVIDER",
                    trade.getId(), trade.getExternalTradeKey());
            return demoUnavailableResponse(trade);
        }
        Optional<TradeReplay> replay = replayRepository.findByUserIdAndTradeId(user.getId(), tradeId);
        if (replay.isEmpty()) {
            return emptyResponse(trade);
        }
        featureUsageService.track(user, FeatureKey.TRADE_REPLAY, FeatureUsageAction.TRADE_REPLAY_VIEWED, FeatureUsageEventType.PAGE_VIEW, trade.getAccount() == null ? null : trade.getAccount().getId(), null);
        return toResponse(replay.get(), includeCandles);
    }

    @Transactional
    public TradeReplayResponse generate(User user, Long tradeId, boolean force) {
        requireEnabled();
        Trade trade = ownedTrade(user, tradeId);
        if (!DemoReplayEligibility.isReplayAllowed(trade)) {
            log.debug("Trade Replay demo eligibility. tradeId={}, externalTradeKey={}, replayCompatible=false, action=BLOCKED_BEFORE_PROVIDER",
                    trade.getId(), trade.getExternalTradeKey());
            return markDemoUnavailable(trade);
        }
        TradeReplay replay = createOrUpdateReplay(trade);
        featureUsageService.track(user, FeatureKey.TRADE_REPLAY, force ? FeatureUsageAction.TRADE_REPLAY_REGENERATED : FeatureUsageAction.TRADE_REPLAY_REQUESTED, FeatureUsageEventType.USER_ACTION, trade.getAccount() == null ? null : trade.getAccount().getId(), null);
        if (force || replay.getStatus() != TradeReplayStatus.READY) {
            processReplay(replay.getId(), force);
        }
        return replayRepository.findByUserIdAndTradeId(user.getId(), tradeId).map(r -> toResponse(r, true)).orElseGet(() -> emptyResponse(trade));
    }

    @Transactional(readOnly = true)
    public List<ReplayCandleResponse> candles(User user, Long tradeId) {
        requireEnabled();
        Trade trade = ownedTrade(user, tradeId);
        if (!DemoReplayEligibility.isReplayAllowed(trade)) {
            log.debug("Trade Replay demo eligibility. tradeId={}, externalTradeKey={}, replayCompatible=false, action=BLOCKED_BEFORE_PROVIDER",
                    trade.getId(), trade.getExternalTradeKey());
            return List.of();
        }
        TradeReplay replay = replayRepository.findByUserIdAndTradeId(user.getId(), tradeId).orElseThrow(() -> new ResourceNotFoundException("Trade replay not found"));
        if (replay.getStatus() != TradeReplayStatus.READY || replay.getResolvedProviderSymbol() == null) {
            return List.of();
        }
        return loadCandles(replay).stream().limit(MAX_RETURNED_CANDLES).map(this::toCandleResponse).toList();
    }

    @Transactional(readOnly = true)
    public ContractCandidatesResponse contractCandidates(User user, Long tradeId) {
        requireEnabled();
        Trade trade = ownedTrade(user, tradeId);
        if (!DemoReplayEligibility.isReplayAllowed(trade)) {
            return new ContractCandidatesResponse(trade.getId(), null, brokerSymbol(trade), List.of());
        }
        String root = contractSymbolNormalizer.rootInstrument(trade.getSymbol());
        if (root == null) {
            root = contractSymbolNormalizer.rootInstrument(trade.getOriginalBrokerSymbol());
        }
        LocalDateTime executionTime = trade.getEntryTime() == null ? trade.getExitTime() : trade.getEntryTime();
        List<ContractCandidateResponse> candidates = contractSymbolNormalizer.candidates(root, executionTime).stream()
                .map(candidate -> new ContractCandidateResponse(candidate.label(), candidate.rootInstrument(), candidate.normalizedSymbol(), candidate.monthCode(), candidate.month(), candidate.year(), candidate.mostLikely()))
                .toList();
        return new ContractCandidatesResponse(trade.getId(), root, brokerSymbol(trade), candidates);
    }

    @Transactional
    public TradeReplayResponse confirmContract(User user, Long tradeId, ConfirmReplayContractRequest request) {
        requireEnabled();
        Trade trade = ownedTrade(user, tradeId);
        if (!DemoReplayEligibility.isReplayAllowed(trade)) {
            log.debug("Trade Replay demo eligibility. tradeId={}, externalTradeKey={}, replayCompatible=false, action=BLOCKED_BEFORE_PROVIDER",
                    trade.getId(), trade.getExternalTradeKey());
            return markDemoUnavailable(trade);
        }
        if (request == null || request.contractSymbol() == null || request.contractSymbol().isBlank()) {
            throw new IllegalArgumentException("contractSymbol is required.");
        }
        ContractSymbolNormalizationResult normalized = contractSymbolNormalizer.normalize(request.contractSymbol(), trade.getEntryTime());
        if (!normalized.valid()) {
            throw new IllegalArgumentException(normalized.message());
        }
        String root = contractSymbolNormalizer.rootInstrument(trade.getSymbol());
        if (root != null && !root.equals(normalized.rootInstrument())) {
            throw new IllegalArgumentException("Contract must match the trade instrument.");
        }
        trade.setSymbol(normalized.rootInstrument());
        trade.setContractSymbol(normalized.normalizedContractSymbol());
        trade.setContractSymbolSource(ContractSymbolSource.USER_CONFIRMED.name());
        trade.setContractSymbolConfirmedAt(LocalDateTime.now());
        if (trade.getOriginalBrokerSymbol() == null || trade.getOriginalBrokerSymbol().isBlank()) {
            trade.setOriginalBrokerSymbol(request.contractSymbol().trim());
        }
        tradeRepository.save(trade);
        replayRepository.findByUserIdAndTradeId(user.getId(), tradeId).ifPresent(replay -> {
            replay.setFailureCode(null);
            replay.setSanitizedFailureMessage(null);
            replay.setResolvedProviderSymbol(null);
            replay.setProvider(null);
            replayRepository.save(replay);
        });
        return generate(user, tradeId, true);
    }

    @Transactional
    public TradeReplayResetResult resetReplayForDev(User user, Long tradeId) {
        Trade trade = ownedTrade(user, tradeId);
        log.info("Dev Trade Replay reset started. tradeId={}, userId={}", tradeId, user.getId());
        Optional<TradeReplay> replay = replayRepository.findByUserIdAndTradeId(user.getId(), trade.getId());
        if (replay.isEmpty()) {
            log.info("Dev Trade Replay reset completed. tradeId={}, userId={}, replaysDeleted=0, insightsDeleted=0, cacheEntriesEvicted=0",
                    tradeId, user.getId());
            return new TradeReplayResetResult(tradeId, 0, 0, 0, true);
        }
        Long replayId = replay.get().getId();
        long insightsDeleted = insightRepository.countByReplayIdIn(List.of(replayId));
        if (insightsDeleted > 0) {
            insightRepository.deleteByReplayId(replayId);
        }
        replayRepository.delete(replay.get());
        log.info("Dev Trade Replay reset completed. tradeId={}, userId={}, replaysDeleted=1, insightsDeleted={}, cacheEntriesEvicted=0",
                tradeId, user.getId(), insightsDeleted);
        return new TradeReplayResetResult(tradeId, 1, insightsDeleted, 0, true);
    }

    public void initializeAfterCommit(List<Trade> trades) {
        if (!featureFlagService.isEnabled(Feature.TRADE_REPLAY) || trades == null || trades.isEmpty()) return;
        List<Long> tradeIds = trades.stream().map(Trade::getId).filter(Objects::nonNull).toList();
        if (tradeIds.isEmpty()) return;
        Runnable task = () -> tradeIds.forEach(this::initializeSafely);
        if (TransactionSynchronizationManager.isActualTransactionActive()) {
            TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
                @Override
                public void afterCommit() {
                    task.run();
                }
            });
        } else {
            task.run();
        }
    }

    @Transactional
    public void processRunnableReplays() {
        if (!properties.isWorkerEnabled() || !featureFlagService.isEnabled(Feature.TRADE_REPLAY)) return;
        List<TradeReplay> runnable = replayRepository.findRunnable(List.of(TradeReplayStatus.PENDING, TradeReplayStatus.WAITING_FOR_MARKET_DATA, TradeReplayStatus.FAILED), Instant.now(), PageRequest.of(0, 10));
        for (TradeReplay replay : runnable) {
            try {
                processReplay(replay.getId(), false);
            } catch (RuntimeException ex) {
                log.warn("Replay worker failed safely. replayId={}, reason={}", replay.getId(), ex.getClass().getSimpleName());
            }
        }
    }

    @Transactional
    public void processReplay(Long replayId, boolean force) {
        TradeReplay replay = replayRepository.findById(replayId).orElseThrow(() -> new ResourceNotFoundException("Trade replay not found"));
        if (!force && replay.getStatus() == TradeReplayStatus.READY) return;
        Trade trade = replay.getTrade();
        if (!DemoReplayEligibility.isReplayAllowed(trade)) {
            log.debug("Trade Replay demo eligibility. tradeId={}, externalTradeKey={}, replayCompatible=false, action=BLOCKED_BEFORE_PROVIDER",
                    trade.getId(), trade.getExternalTradeKey());
            replay.setStatus(TradeReplayStatus.DEMO_REPLAY_UNAVAILABLE);
            replay.setFailureCode(DemoReplayEligibility.UNAVAILABLE_CODE);
            replay.setSanitizedFailureMessage(DemoReplayEligibility.UNAVAILABLE_MESSAGE);
            replay.setProvider(null);
            replay.setResolvedProviderSymbol(null);
            replayRepository.save(replay);
            return;
        }
        prepareReplayFields(replay, trade);
        if (replay.getStatus() == TradeReplayStatus.UNSUPPORTED_INSTRUMENT || replay.getStatus() == TradeReplayStatus.CONTRACT_RESOLUTION_FAILED) {
            replayRepository.save(replay);
            return;
        }
        Instant now = Instant.now();
        if (replay.getEligibleAt() != null && now.isBefore(replay.getEligibleAt())) {
            replay.setStatus(TradeReplayStatus.WAITING_FOR_MARKET_DATA);
            replay.setFailureCode(null);
            replay.setSanitizedFailureMessage("Replay will be eligible after T+1 historical data is available.");
            replayRepository.save(replay);
            track(replay, FeatureUsageAction.TRADE_REPLAY_WAITING_FOR_DATA);
            return;
        }
        HistoricalMarketDataProvider provider = providerRegistry.activeProvider();
        replay.setProvider(provider.providerName());
        if ("disabled".equals(provider.providerName())) {
            log.warn(
                    "Trade Replay provider unavailable. tradeId={}, replayId={}, resolvedContract={}, configuredProvider={}, activeProvider={}, databentoProviderAvailable={}, databentoKeyConfigured={}",
                    trade.getId(),
                    replay.getId(),
                    replay.getResolvedProviderSymbol(),
                    providerRegistry.configuredProvider(),
                    provider.providerName(),
                    providerRegistry.databentoProviderAvailable(),
                    providerRegistry.databentoApiKeyConfigured()
            );
            replay.setStatus(TradeReplayStatus.PROVIDER_NOT_CONFIGURED);
            replay.setFailureCode("PROVIDER_NOT_CONFIGURED");
            replay.setSanitizedFailureMessage("Historical market data provider is not configured.");
            replayRepository.save(replay);
            return;
        }
        List<MarketCandle> cached = properties.isCacheEnabled() ? loadCandles(replay) : List.of();
        if (cached.size() < expectedMinimumCandles(replay)) {
            replay.setStatus(TradeReplayStatus.FETCHING_DATA);
            replayRepository.saveAndFlush(replay);
            track(replay, FeatureUsageAction.TRADE_REPLAY_PROVIDER_REQUESTED);
            log.info(
                    "Trade Replay market data request prepared. tradeId={}, replayId={}, symbol={}, contractSymbol={}, contractSymbolSource={}, originalBrokerSymbol={}, resolvedProviderSymbol={}, entryTime={}, exitTime={}, tradeTimeZone={}, entryUtc={}, exitUtc={}, requestStartUtc={}, requestEndUtc={}, provider={}, dataset={}, schema={}",
                    trade.getId(),
                    replay.getId(),
                    trade.getSymbol(),
                    trade.getContractSymbol(),
                    trade.getContractSymbolSource(),
                    trade.getOriginalBrokerSymbol(),
                    replay.getResolvedProviderSymbol(),
                    trade.getEntryTime(),
                    trade.getExitTime(),
                    TradeReplayTime.TRADE_TIME_ZONE,
                    replay.getEntryTimestamp(),
                    replay.getFinalExitTimestamp(),
                    replay.getWindowStart(),
                    replay.getWindowEnd(),
                    provider.providerName(),
                    properties.getDatabento().getDataset(),
                    properties.getDatabento().getSchema()
            );
            HistoricalCandleResult result = provider.getCandles(new MarketDataRequest(
                    canonicalRoot(replay.getResolvedProviderSymbol()),
                    replay.getResolvedProviderSymbol(),
                    replay.getWindowStart(),
                    replay.getWindowEnd(),
                    INTERVAL,
                    Map.of("dataset", properties.getDatabento().getDataset(), "schema", properties.getDatabento().getSchema())
            ));
            if (!result.success()) {
                failReplay(replay, result.failureCode(), result.sanitizedMessage());
                track(replay, FeatureUsageAction.TRADE_REPLAY_PROVIDER_FAILED);
                return;
            }
            if (properties.isCacheEnabled()) {
                saveCandles(provider.providerName(), replay.getResolvedProviderSymbol(), result.candles());
                cached = loadCandles(replay);
            } else {
                cached = result.candles().stream().map(candle -> toMarketCandle(provider.providerName(), replay.getResolvedProviderSymbol(), candle)).toList();
            }
        }
        if (cached.size() < expectedMinimumCandles(replay)) {
            replay.setStatus(TradeReplayStatus.NOT_ENOUGH_DATA);
            replay.setFailureCode("NOT_ENOUGH_DATA");
            replay.setSanitizedFailureMessage("Not enough one-minute candles were available to reconstruct this replay.");
            replayRepository.save(replay);
            return;
        }
        replay.setStatus(TradeReplayStatus.ANALYZING);
        replayRepository.saveAndFlush(replay);
        TradeReplayAnalysis analysis = analysisEngine.analyze(trade, cached.stream().map(this::toHistorical).toList(), canonicalRoot(replay.getResolvedProviderSymbol()));
        replay.setMetricsJson(writeJson(analysis.metrics()));
        replay.setAiSummary(analysis.summary());
        replay.setReflectionQuestion(analysis.reflectionQuestion());
        replay.setDeterministicAnalysisVersion(ANALYSIS_VERSION);
        replay.setGeneratedAt(Instant.now());
        replay.setFailureCode(null);
        replay.setSanitizedFailureMessage(null);
        replay.setStatus(TradeReplayStatus.READY);
        TradeReplay saved = replayRepository.save(replay);
        insightRepository.deleteByReplayId(saved.getId());
        insightRepository.saveAll(analysis.insights().stream().map(draft -> toInsight(saved, draft)).toList());
        track(saved, FeatureUsageAction.TRADE_REPLAY_GENERATED);
    }

    @Transactional
    public void backfill(Long userId, Instant since, Instant until, int maxBatchSize, boolean dryRun) {
        // Admin-safe hook for future controller/CLI use. Deliberately requires explicit limits.
        if (maxBatchSize <= 0 || maxBatchSize > 500 || userId == null) {
            throw new IllegalArgumentException("Backfill requires a userId and maxBatchSize between 1 and 500.");
        }
    }

    private void initializeSafely(Long tradeId) {
        try {
            tradeRepository.findById(tradeId).ifPresent(this::createOrUpdateReplay);
        } catch (RuntimeException ex) {
            log.warn("Replay after-import initialization failed safely. tradeId={}, reason={}", tradeId, ex.getClass().getSimpleName());
        }
    }

    private TradeReplay createOrUpdateReplay(Trade trade) {
        TradeReplay replay = replayRepository.findByUserIdAndTradeId(trade.getUser().getId(), trade.getId()).orElseGet(TradeReplay::new);
        if (replay.getId() == null) {
            replay.setUser(trade.getUser());
            replay.setTrade(trade);
        }
        prepareReplayFields(replay, trade);
        try {
            return replayRepository.saveAndFlush(replay);
        } catch (DataIntegrityViolationException ex) {
            return replayRepository.findByUserIdAndTradeId(trade.getUser().getId(), trade.getId()).orElseThrow(() -> ex);
        }
    }

    private void prepareReplayFields(TradeReplay replay, Trade trade) {
        replay.setOriginalBrokerSymbol(brokerSymbol(trade));
        replay.setCandleInterval(INTERVAL);
        replay.setEntryTimestamp(trade.getEntryTime() == null ? null : TradeReplayTime.toInstant(trade.getEntryTime()));
        replay.setFinalExitTimestamp(trade.getExitTime() == null ? null : TradeReplayTime.toInstant(trade.getExitTime()));
        if (replay.getEntryTimestamp() == null || replay.getFinalExitTimestamp() == null || !replay.getFinalExitTimestamp().isAfter(replay.getEntryTimestamp())) {
            replay.setStatus(TradeReplayStatus.CONTRACT_RESOLUTION_FAILED);
            replay.setFailureCode("MISSING_EXECUTION_TIMES");
            replay.setSanitizedFailureMessage("Replay requires valid entry and exit timestamps.");
            return;
        }
        ContractResolutionResult resolution = contractResolver.resolve(trade);
        if (!resolution.resolved()) {
            replay.setStatus("UNSUPPORTED_INSTRUMENT".equals(resolution.failureCode()) ? TradeReplayStatus.UNSUPPORTED_INSTRUMENT : TradeReplayStatus.CONTRACT_RESOLUTION_FAILED);
            replay.setFailureCode(resolution.failureCode());
            replay.setSanitizedFailureMessage(resolution.message());
            return;
        }
        replay.setResolvedProviderSymbol(resolution.providerSymbol());
        replay.setOriginalBrokerSymbol(resolution.originalBrokerSymbol());
        log.info(
                "Trade Replay contract resolved. tradeId={}, requestedContract={}, resolvedContract={}, resolutionSource={}, resolutionReason={}, expectedContract={}, storedContractValidForDate={}",
                trade.getId(),
                resolution.requestedContract(),
                resolution.providerSymbol(),
                resolution.resolutionSource(),
                resolution.resolutionReason(),
                resolution.expectedContract(),
                resolution.storedContractValidForDate()
        );
        Instant start = replay.getEntryTimestamp().minus(Duration.ofMinutes(properties.getPreTradeMinutes()));
        Instant desiredEnd = replay.getFinalExitTimestamp().plus(Duration.ofMinutes(properties.getPostTradeMinutes()));
        Instant maxEnd = start.plus(Duration.ofHours(properties.getMaxWindowHours()));
        replay.setWindowStart(start);
        replay.setWindowEnd(desiredEnd.isAfter(maxEnd) ? maxEnd : desiredEnd);
        replay.setEligibleAt(replay.getFinalExitTimestamp().plus(Duration.ofHours(properties.getReplayDelayHours())));
        if (replay.getStatus() == null || replay.getStatus() == TradeReplayStatus.PROVIDER_NOT_CONFIGURED || replay.getStatus() == TradeReplayStatus.FAILED || replay.getStatus() == TradeReplayStatus.CONTRACT_RESOLUTION_FAILED) {
            replay.setStatus(TradeReplayStatus.PENDING);
        }
    }

    private void saveCandles(String provider, String contractSymbol, List<HistoricalCandle> candles) {
        Set<Instant> existing = new HashSet<>(candleRepository.findByProviderAndContractSymbolAndIntervalAndTimestampGreaterThanEqualAndTimestampLessThanOrderByTimestampAsc(
                provider, contractSymbol, INTERVAL, candles.stream().map(HistoricalCandle::timestamp).min(Instant::compareTo).orElse(Instant.EPOCH), candles.stream().map(HistoricalCandle::timestamp).max(Instant::compareTo).orElse(Instant.EPOCH).plusSeconds(60)
        ).stream().map(MarketCandle::getTimestamp).toList());
        List<MarketCandle> toSave = new ArrayList<>();
        for (HistoricalCandle candle : candles) {
            if (!existing.add(candle.timestamp())) continue;
            MarketCandle entity = new MarketCandle();
            entity.setProvider(provider);
            entity.setContractSymbol(contractSymbol);
            entity.setInterval(INTERVAL);
            entity.setTimestamp(candle.timestamp());
            entity.setOpen(candle.open());
            entity.setHigh(candle.high());
            entity.setLow(candle.low());
            entity.setClose(candle.close());
            entity.setVolume(candle.volume());
            toSave.add(entity);
        }
        if (!toSave.isEmpty()) {
            try {
                candleRepository.saveAll(toSave);
            } catch (DataIntegrityViolationException ex) {
                log.info("Duplicate candle insert ignored. provider={}, contract={}", provider, contractSymbol);
            }
        }
    }

    private MarketCandle toMarketCandle(String provider, String contractSymbol, HistoricalCandle candle) {
        MarketCandle entity = new MarketCandle();
        entity.setProvider(provider);
        entity.setContractSymbol(contractSymbol);
        entity.setInterval(INTERVAL);
        entity.setTimestamp(candle.timestamp());
        entity.setOpen(candle.open());
        entity.setHigh(candle.high());
        entity.setLow(candle.low());
        entity.setClose(candle.close());
        entity.setVolume(candle.volume());
        return entity;
    }

    private void failReplay(TradeReplay replay, String code, String message) {
        replay.setStatus("PROVIDER_NOT_CONFIGURED".equals(code) ? TradeReplayStatus.PROVIDER_NOT_CONFIGURED : TradeReplayStatus.FAILED);
        replay.setFailureCode(code);
        replay.setSanitizedFailureMessage(message);
        if (retryableProviderFailure(code)) {
            replay.setRetryCount(replay.getRetryCount() + 1);
            replay.setNextRetryAt(Instant.now().plus(Duration.ofMinutes(Math.min(60, Math.max(5, replay.getRetryCount() * 5L)))));
        } else {
            replay.setNextRetryAt(null);
        }
        replayRepository.save(replay);
    }

    private boolean retryableProviderFailure(String code) {
        return "PROVIDER_UNAVAILABLE".equals(code) || "PROVIDER_RATE_LIMITED".equals(code);
    }

    private Trade ownedTrade(User user, Long tradeId) {
        if (user == null) throw new ResourceNotFoundException("Trade not found");
        return tradeRepository.findByIdAndUser(tradeId, user).orElseThrow(() -> new ResourceNotFoundException("Trade not found"));
    }

    private void requireEnabled() {
        featureFlagService.requireEnabled(Feature.TRADE_REPLAY);
    }

    private int expectedMinimumCandles(TradeReplay replay) {
        if (replay.getWindowStart() == null || replay.getWindowEnd() == null) return Integer.MAX_VALUE;
        long minutes = Math.max(1, Duration.between(replay.getWindowStart(), replay.getWindowEnd()).toMinutes());
        return (int) Math.min(10, Math.max(3, minutes / 12));
    }

    private List<MarketCandle> loadCandles(TradeReplay replay) {
        if (replay.getProvider() == null || replay.getResolvedProviderSymbol() == null || replay.getWindowStart() == null || replay.getWindowEnd() == null) return List.of();
        return candleRepository.findByProviderAndContractSymbolAndIntervalAndTimestampGreaterThanEqualAndTimestampLessThanOrderByTimestampAsc(
                replay.getProvider(), replay.getResolvedProviderSymbol(), INTERVAL, replay.getWindowStart(), replay.getWindowEnd()
        );
    }

    private TradeReplayResponse toResponse(TradeReplay replay, boolean includeCandles) {
        List<MarketCandle> candles = includeCandles && replay.getStatus() == TradeReplayStatus.READY ? loadCandles(replay).stream().limit(MAX_RETURNED_CANDLES).toList() : List.of();
        List<ReplayInsightResponse> insights = insightRepository.findByReplayIdOrderByDisplayOrderAsc(replay.getId()).stream().map(this::toInsightResponse).toList();
        return new TradeReplayResponse(
                replay.getTrade().getId(),
                replay.getId(),
                replay.getStatus(),
                replay.getEligibleAt(),
                replay.getOriginalBrokerSymbol(),
                replay.getResolvedProviderSymbol(),
                replay.getProvider(),
                replay.getCandleInterval(),
                replay.getWindowStart(),
                replay.getWindowEnd(),
                replay.getGeneratedAt(),
                replay.getFailureCode(),
                replay.getSanitizedFailureMessage(),
                markers(replay.getTrade()),
                candles.stream().map(this::toCandleResponse).toList(),
                readMap(replay.getMetricsJson()),
                insights,
                replay.getAiSummary(),
                replay.getReflectionQuestion(),
                dataQualityWarnings(replay, candles)
        );
    }

    private TradeReplayResponse emptyResponse(Trade trade) {
        return new TradeReplayResponse(trade.getId(), null, null, null, trade.getSymbol(), null, null, INTERVAL, null, null, null, null, "Replay has not been requested for this trade.", markers(trade), List.of(), Map.of(), List.of(), null, null, List.of());
    }

    private TradeReplayResponse demoUnavailableResponse(Trade trade) {
        return new TradeReplayResponse(
                trade.getId(),
                null,
                TradeReplayStatus.DEMO_REPLAY_UNAVAILABLE,
                null,
                brokerSymbol(trade),
                null,
                null,
                INTERVAL,
                null,
                null,
                null,
                DemoReplayEligibility.UNAVAILABLE_CODE,
                DemoReplayEligibility.UNAVAILABLE_MESSAGE,
                markers(trade),
                List.of(),
                Map.of(),
                List.of(),
                null,
                null,
                List.of()
        );
    }

    private TradeReplayResponse markDemoUnavailable(Trade trade) {
        TradeReplay replay = replayRepository.findByUserIdAndTradeId(trade.getUser().getId(), trade.getId()).orElseGet(TradeReplay::new);
        if (replay.getId() == null) {
            replay.setUser(trade.getUser());
            replay.setTrade(trade);
        }
        replay.setOriginalBrokerSymbol(brokerSymbol(trade));
        replay.setCandleInterval(INTERVAL);
        replay.setStatus(TradeReplayStatus.DEMO_REPLAY_UNAVAILABLE);
        replay.setFailureCode(DemoReplayEligibility.UNAVAILABLE_CODE);
        replay.setSanitizedFailureMessage(DemoReplayEligibility.UNAVAILABLE_MESSAGE);
        replay.setProvider(null);
        replay.setResolvedProviderSymbol(null);
        replay.setMetricsJson(null);
        replay.setAiSummary(null);
        replay.setReflectionQuestion(null);
        TradeReplay saved = replayRepository.saveAndFlush(replay);
        insightRepository.deleteByReplayId(saved.getId());
        return toResponse(saved, false);
    }

    private List<ReplayMarkerResponse> markers(Trade trade) {
        List<ReplayMarkerResponse> markers = new ArrayList<>();
        if (trade.getEntryTime() != null) {
            markers.add(new ReplayMarkerResponse("ENTRY", TradeReplayTime.toInstant(trade.getEntryTime()), trade.getEntryPrice(), trade.getQuantity(), "Entry"));
        }
        if (trade.getExitTime() != null) {
            markers.add(new ReplayMarkerResponse("FINAL_EXIT", TradeReplayTime.toInstant(trade.getExitTime()), trade.getExitPrice(), trade.getQuantity(), "Final exit"));
        }
        return markers;
    }

    private ReplayCandleResponse toCandleResponse(MarketCandle candle) {
        return new ReplayCandleResponse(candle.getTimestamp(), candle.getOpen(), candle.getHigh(), candle.getLow(), candle.getClose(), candle.getVolume());
    }

    private HistoricalCandle toHistorical(MarketCandle candle) {
        return new HistoricalCandle(candle.getTimestamp(), candle.getOpen(), candle.getHigh(), candle.getLow(), candle.getClose(), candle.getVolume());
    }

    private TradeReplayInsight toInsight(TradeReplay replay, TradeReplayAnalysis.InsightDraft draft) {
        TradeReplayInsight insight = new TradeReplayInsight();
        insight.setReplay(replay);
        insight.setType(draft.type());
        insight.setEventTimestamp(draft.timestamp());
        insight.setEventPrice(draft.price());
        insight.setSeverity(draft.severity());
        insight.setDeterministicTitle(draft.title());
        insight.setDeterministicExplanation(draft.explanation());
        insight.setSupportingMetricsJson(writeJson(draft.supportingMetrics()));
        insight.setDisplayOrder(draft.order());
        return insight;
    }

    private ReplayInsightResponse toInsightResponse(TradeReplayInsight insight) {
        return new ReplayInsightResponse(insight.getId(), insight.getType(), insight.getEventTimestamp(), insight.getEventPrice(), insight.getSeverity(), insight.getDeterministicTitle(), insight.getDeterministicExplanation(), readMap(insight.getSupportingMetricsJson()), insight.getAiExplanation(), insight.getDisplayOrder(), insight.getDisplayOrder() <= 5);
    }

    private List<String> dataQualityWarnings(TradeReplay replay, List<MarketCandle> candles) {
        if (replay.getStatus() == TradeReplayStatus.READY && candles.size() >= MAX_RETURNED_CANDLES) {
            return List.of("Replay candles were capped to the maximum safe response size.");
        }
        return List.of();
    }

    private String canonicalRoot(String symbol) {
        if (symbol == null) return null;
        if (symbol.startsWith("MES")) return "MES";
        if (symbol.startsWith("MNQ")) return "MNQ";
        if (symbol.startsWith("ES")) return "ES";
        if (symbol.startsWith("NQ")) return "NQ";
        return symbol;
    }

    private String brokerSymbol(Trade trade) {
        if (trade.getOriginalBrokerSymbol() != null && !trade.getOriginalBrokerSymbol().isBlank()) return trade.getOriginalBrokerSymbol();
        if (trade.getContractSymbol() != null && !trade.getContractSymbol().isBlank()) return trade.getContractSymbol();
        return trade.getSymbol();
    }

    private String writeJson(Object value) {
        try {
            return objectMapper.writeValueAsString(value == null ? Map.of() : value);
        } catch (Exception ex) {
            return "{}";
        }
    }

    private Map<String, Object> readMap(String json) {
        if (json == null || json.isBlank()) return Map.of();
        try {
            return objectMapper.readValue(json, new TypeReference<>() {});
        } catch (Exception ex) {
            return Map.of();
        }
    }

    private void track(TradeReplay replay, FeatureUsageAction action) {
        try {
            featureUsageService.track(replay.getUser(), FeatureKey.TRADE_REPLAY, action, FeatureUsageEventType.USER_ACTION, replay.getTrade().getAccount() == null ? null : replay.getTrade().getAccount().getId(), null);
        } catch (RuntimeException ignored) {
            // FeatureUsageService already fails open, but replay should stay insulated.
        }
    }
}
