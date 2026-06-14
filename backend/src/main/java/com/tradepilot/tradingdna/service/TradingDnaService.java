package com.tradepilot.tradingdna.service;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.tradepilot.analytics.TradeAnalyticsService;
import com.tradepilot.analytics.dto.BehavioralInsightDto;
import com.tradepilot.billing.Feature;
import com.tradepilot.billing.FeatureAccessService;
import com.tradepilot.billing.PlanLimitExceededException;
import com.tradepilot.billing.UsageLimitService;
import com.tradepilot.billing.UsageType;
import com.tradepilot.coach.entity.AiCoachSnapshot;
import com.tradepilot.coach.repository.AiCoachSnapshotRepository;
import com.tradepilot.config.OpenAiProperties;
import com.tradepilot.entity.AiTradeAnalysis;
import com.tradepilot.entity.AiTradeReview;
import com.tradepilot.entity.Trade;
import com.tradepilot.entity.TradeRuleViolation;
import com.tradepilot.entity.User;
import com.tradepilot.repository.AiTradeAnalysisRepository;
import com.tradepilot.repository.AiTradeReviewRepository;
import com.tradepilot.repository.TradeRepository;
import com.tradepilot.repository.TradeRuleViolationRepository;
import com.tradepilot.repository.TradingAccountRepository;
import com.tradepilot.tradingdna.dto.TradingDnaAiResult;
import com.tradepilot.tradingdna.dto.TradingDnaContext;
import com.tradepilot.tradingdna.dto.TradingDnaResponse;
import com.tradepilot.tradingdna.dto.TradingDnaTimelinePoint;
import com.tradepilot.tradingdna.entity.TradingDnaSnapshot;
import com.tradepilot.tradingdna.repository.TradingDnaSnapshotRepository;
import com.tradepilot.observability.ObservabilityService;
import com.tradepilot.service.AggregateCacheService;
import com.tradepilot.service.PerformanceCacheInvalidationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.*;
import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class TradingDnaService {
    private static final int MIN_TRADES_FOR_AI = 5;
    private static final Duration TRADING_DNA_RESPONSE_TTL = Duration.ofSeconds(45);

    private final TradeRepository tradeRepository;
    private final AiTradeReviewRepository aiTradeReviewRepository;
    private final AiTradeAnalysisRepository aiTradeAnalysisRepository;
    private final AiCoachSnapshotRepository aiCoachSnapshotRepository;
    private final TradeRuleViolationRepository tradeRuleViolationRepository;
    private final TradingDnaSnapshotRepository snapshotRepository;
    private final TradingAccountRepository tradingAccountRepository;
    private final TradeAnalyticsService tradeAnalyticsService;
    private final TradingDnaAiGeneratorService aiGeneratorService;
    private final UsageLimitService usageLimitService;
    private final FeatureAccessService featureAccessService;
    private final ObjectMapper objectMapper;
    private final ObservabilityService observabilityService;
    private final OpenAiProperties openAiProperties;
    private final AggregateCacheService aggregateCacheService;
    private final PerformanceCacheInvalidationService cacheInvalidationService;

    public TradingDnaResponse getTradingDna(
            User user,
            Long accountId,
            Integer days,
            Boolean includeAllAccounts,
            Boolean forceRefresh
    ) {
        long requestStartedAt = System.nanoTime();
        if (user == null) {
            throw new IllegalArgumentException("Authenticated user is required");
        }
        featureAccessService.requireFeature(user, Feature.TRADING_DNA);

        int timeframeDays = normalizeDays(days);
        boolean allAccounts = Boolean.TRUE.equals(includeAllAccounts);
        Long scopedAccountId = allAccounts ? null : accountId;

        log.info("Trading DNA request. currentUserId={}, accountId={}, includeAllAccounts={}, forceRefresh={}, days={}",
                user.getId(),
                accountId,
                includeAllAccounts,
                forceRefresh,
                timeframeDays
        );

        if (!allAccounts && scopedAccountId == null) {
            return emptyResponse(null, timeframeDays, false, "Select a trading account to load Trading DNA.");
        }

        if (!allAccounts && tradingAccountRepository.findByIdAndUser_Id(scopedAccountId, user.getId()).isEmpty()) {
            log.warn("Trading DNA invalid account scope. currentUserId={}, accountId={}", user.getId(), scopedAccountId);
            return emptyResponse(scopedAccountId, timeframeDays, false, "Selected account is no longer available.");
        }

        LocalDate endDate = LocalDate.now();
        LocalDate startDate = endDate.minusDays(timeframeDays - 1L);
        long fingerprintStartedAt = System.nanoTime();
        String dataHash = hashFingerprint(user.getId(), scopedAccountId, allAccounts, timeframeDays, startDate, endDate);
        long fingerprintElapsedMs = elapsedMs(fingerprintStartedAt);
        log.debug("Trading DNA fingerprint calculated. userId={}, accountId={}, includeAllAccounts={}, days={}, dataHash={}, fingerprintMs={}",
                user.getId(), scopedAccountId, allAccounts, timeframeDays, dataHash, fingerprintElapsedMs);

        TradingDnaResponse response = getTradingDnaForHash(user, scopedAccountId, timeframeDays, allAccounts, Boolean.TRUE.equals(forceRefresh), startDate, endDate, dataHash);
        log.debug("Trading DNA request completed. userId={}, accountId={}, includeAllAccounts={}, days={}, cached={}, totalMs={}",
                user.getId(), scopedAccountId, allAccounts, timeframeDays, response.getCached(), elapsedMs(requestStartedAt));
        return response;
    }

    private TradingDnaResponse getTradingDnaForHash(
            User user,
            Long scopedAccountId,
            int timeframeDays,
            boolean allAccounts,
            boolean forceRefresh,
            LocalDate startDate,
            LocalDate endDate,
            String dataHash
    ) {
        long requestStartedAt = System.nanoTime();
        long snapshotLookupStartedAt = System.nanoTime();
        log.debug("Trading DNA cache lookup start. userId={}, accountId={}, includeAllAccounts={}, days={}, dataHash={}, forceRefresh={}",
                user.getId(), scopedAccountId, allAccounts, timeframeDays, dataHash, forceRefresh);
        Optional<TradingDnaSnapshot> matchingSnapshot = findMatchingSnapshot(user.getId(), scopedAccountId, timeframeDays, allAccounts, dataHash);
        long snapshotLookupElapsedMs = elapsedMs(snapshotLookupStartedAt);

        if (!Boolean.TRUE.equals(forceRefresh) && matchingSnapshot.isPresent()) {
            observabilityService.recordCacheEvent("Trading DNA", true);
            observabilityService.recordCacheEvent("trading-dna-cache-hit", true);
            log.info("Trading DNA cache hit. userId={}, accountId={}, includeAllAccounts={}, days={}, snapshotId={}, snapshotLookupMs={}",
                    user.getId(), scopedAccountId, allAccounts, timeframeDays, matchingSnapshot.get().getId(), snapshotLookupElapsedMs);
            TradingDnaSnapshot snapshot = matchingSnapshot.get();
            long responseCacheStartedAt = System.nanoTime();
            TradingDnaResponse response = aggregateCacheService.get(
                    tradingDnaCacheKey(user.getId(), scopedAccountId, allAccounts, timeframeDays, dataHash),
                    TRADING_DNA_RESPONSE_TTL,
                    () -> {
                        long mapStartedAt = System.nanoTime();
                        TradingDnaResponse mapped = toResponse(snapshot, true);
                        log.debug("Trading DNA snapshot mapped to response. userId={}, accountId={}, snapshotId={}, mapMs={}",
                                user.getId(), scopedAccountId, snapshot.getId(), elapsedMs(mapStartedAt));
                        return mapped;
                    }
            );
            log.debug("Trading DNA cached response returned. userId={}, accountId={}, includeAllAccounts={}, days={}, responseCacheMs={}, totalMs={}",
                    user.getId(), scopedAccountId, allAccounts, timeframeDays, elapsedMs(responseCacheStartedAt), elapsedMs(requestStartedAt));
            return response;
        }
        observabilityService.recordCacheEvent("Trading DNA", false);
        observabilityService.recordCacheEvent("trading-dna-cache-miss", false);
        log.warn("Trading DNA cache miss. userId={}, accountId={}, includeAllAccounts={}, days={}, forceRefresh={}, snapshotLookupMs={}",
                user.getId(), scopedAccountId, allAccounts, timeframeDays, forceRefresh, snapshotLookupElapsedMs);

        long tradesStartedAt = System.nanoTime();
        List<Trade> trades = loadTrades(user, scopedAccountId, startDate, endDate);
        long tradesElapsedMs = elapsedMs(tradesStartedAt);
        if (trades.isEmpty()) {
            log.warn("Trading DNA has no trades. userId={}, accountId={}, includeAllAccounts={}, startDate={}, endDate={}",
                    user.getId(), scopedAccountId, allAccounts, startDate, endDate);
        }
        long contextStartedAt = System.nanoTime();
        TradingDnaContext context = buildContext(user, scopedAccountId, allAccounts, timeframeDays, startDate, endDate, trades);
        long contextElapsedMs = elapsedMs(contextStartedAt);

        long usageStartedAt = System.nanoTime();
        try {
            usageLimitService.enforceAiUsage(user, UsageType.TRADING_DNA);
        } catch (PlanLimitExceededException limitExceeded) {
            long usageElapsedMs = elapsedMs(usageStartedAt);
            Optional<TradingDnaSnapshot> fallbackCache = matchingSnapshot.isPresent()
                    ? matchingSnapshot
                    : snapshotRepository.findTopByUserIdAndAccountIdAndDaysAndIncludeAllAccountsOrderByUpdatedAtDesc(
                            user.getId(),
                            scopedAccountId,
                            timeframeDays,
                            allAccounts
                    );
            if (fallbackCache.isPresent()) {
                TradingDnaResponse response = toResponse(fallbackCache.get(), true);
                response.setLimitReached(true);
                response.setMessage("Monthly Trading DNA limit reached. Showing cached analysis.");
                log.warn("Trading DNA usage limit reached; returning cached snapshot. userId={}, accountId={}, snapshotId={}, usageCheckMs={}, tradesQueryMs={}, contextBuildMs={}, totalMs={}",
                        user.getId(), scopedAccountId, fallbackCache.get().getId(), usageElapsedMs, tradesElapsedMs, contextElapsedMs, elapsedMs(requestStartedAt));
                return response;
            }

            log.warn("Trading DNA usage limit reached without cache. userId={}, accountId={}, usageCheckMs={}, tradesQueryMs={}, contextBuildMs={}, totalMs={}",
                    user.getId(), scopedAccountId, usageElapsedMs, tradesElapsedMs, contextElapsedMs, elapsedMs(requestStartedAt));
            return limitReachedResponse(scopedAccountId, timeframeDays, allAccounts, limitExceeded.getMessage());
        }
        long usageElapsedMs = elapsedMs(usageStartedAt);

        long aiStartedAt = System.nanoTime();
        log.debug("Trading DNA OpenAI generation start. userId={}, accountId={}, includeAllAccounts={}, days={}, totalTrades={}",
                user.getId(), scopedAccountId, allAccounts, timeframeDays, context.getTotalTrades());
        TradingDnaAiResult aiResult = context.getTotalTrades() < MIN_TRADES_FOR_AI
                ? insufficientDataResult(context)
                : generate(context);
        long aiElapsedMs = elapsedMs(aiStartedAt);
        log.debug("Trading DNA OpenAI generation end. userId={}, accountId={}, includeAllAccounts={}, days={}, totalTrades={}, elapsedMs={}",
                user.getId(), scopedAccountId, allAccounts, timeframeDays, context.getTotalTrades(), aiElapsedMs);

        long existingSnapshotStartedAt = System.nanoTime();
        TradingDnaSnapshot snapshot = snapshotRepository
                .findTopByUserIdAndAccountIdAndDaysAndIncludeAllAccountsOrderByUpdatedAtDesc(
                        user.getId(),
                        scopedAccountId,
                        timeframeDays,
                        allAccounts
                )
                .orElseGet(TradingDnaSnapshot::new);
        long existingSnapshotElapsedMs = elapsedMs(existingSnapshotStartedAt);
        snapshot.setUserId(user.getId());
        snapshot.setAccountId(scopedAccountId);
        snapshot.setDays(timeframeDays);
        snapshot.setIncludeAllAccounts(allAccounts);
        snapshot.setDataHash(dataHash);
        snapshot.setStatus(context.getTotalTrades() < MIN_TRADES_FOR_AI ? "INSUFFICIENT_DATA" : "COMPLETED");
        snapshot.setTotalTrades(context.getTotalTrades());
        snapshot.setContextJson(writeJson(context));
        snapshot.setTimelineJson(writeJson(context.getTimeline()));
        snapshot.setAiResponseJson(writeJson(aiResult));
        snapshot.setModel(openAiProperties.getModel());

        long saveStartedAt = System.nanoTime();
        TradingDnaSnapshot saved = snapshotRepository.save(snapshot);
        long saveElapsedMs = elapsedMs(saveStartedAt);
        cacheInvalidationService.invalidateTradingDna(user.getId());
        usageLimitService.recordAiUsage(user, UsageType.TRADING_DNA, saved.getId());
        long responseStartedAt = System.nanoTime();
        TradingDnaResponse response = toResponse(saved, false);
        long responseElapsedMs = elapsedMs(responseStartedAt);
        log.info("Trading DNA generation completed. userId={}, accountId={}, includeAllAccounts={}, days={}, snapshotId={}, totalTrades={}, snapshotLookupMs={}, tradesQueryMs={}, contextBuildMs={}, usageCheckMs={}, aiMs={}, existingSnapshotLookupMs={}, saveMs={}, responseBuildMs={}, totalMs={}",
                user.getId(), scopedAccountId, allAccounts, timeframeDays, saved.getId(), context.getTotalTrades(), snapshotLookupElapsedMs, tradesElapsedMs, contextElapsedMs, usageElapsedMs, aiElapsedMs, existingSnapshotElapsedMs, saveElapsedMs, responseElapsedMs, elapsedMs(requestStartedAt));
        return response;
    }

    private String tradingDnaCacheKey(Long userId, Long accountId, boolean includeAllAccounts, int days, String dataHash) {
        return "trading-dna:" + userId + ":" + accountId + ":" + includeAllAccounts + ":" + days + ":" + dataHash;
    }

    private Optional<TradingDnaSnapshot> findMatchingSnapshot(Long userId, Long accountId, int days, boolean includeAllAccounts, String dataHash) {
        return snapshotRepository.findMatchingSnapshot(
                        userId,
                        accountId,
                        days,
                        includeAllAccounts,
                        dataHash,
                        PageRequest.of(0, 1)
                )
                .stream()
                .findFirst();
    }

    private TradingDnaAiResult generate(TradingDnaContext context) {
        try {
            return aiGeneratorService.generate(context);
        } catch (Exception e) {
            log.warn("Trading DNA AI generation failed. Using deterministic fallback.", e);
            return deterministicFallback(context);
        }
    }

    private List<Trade> loadTrades(User user, Long accountId, LocalDate startDate, LocalDate endDate) {
        if (accountId == null) {
            return tradeRepository.findByUserAndTradeDateBetweenOrderByTradeDateDesc(user, startDate, endDate);
        }

        return tradeRepository.findByUserAndAccount_IdAndTradeDateBetweenOrderByTradeDateDesc(
                user,
                accountId,
                startDate,
                endDate
        );
    }

    private TradingDnaContext buildContext(
            User user,
            Long accountId,
            boolean includeAllAccounts,
            int days,
            LocalDate startDate,
            LocalDate endDate,
            List<Trade> trades
    ) {
        long contextStartedAt = System.nanoTime();
        List<Long> tradeIds = trades.stream().map(Trade::getId).filter(Objects::nonNull).toList();
        Instant startInstant = startDate.atStartOfDay(ZoneId.systemDefault()).toInstant();
        Instant endInstant = endDate.plusDays(1).atStartOfDay(ZoneId.systemDefault()).toInstant();

        long reviewsStartedAt = System.nanoTime();
        List<AiTradeReview> reviews = accountId == null
                ? aiTradeReviewRepository.findByUserIdAndCreatedAtBetweenOrderByCreatedAtDesc(user.getId(), startInstant, endInstant)
                : aiTradeReviewRepository.findByUserIdAndAccountIdAndCreatedAtBetweenOrderByCreatedAtDesc(user.getId(), accountId, startInstant, endInstant);
        long reviewsElapsedMs = elapsedMs(reviewsStartedAt);

        long analysesStartedAt = System.nanoTime();
        List<AiTradeAnalysis> analyses = tradeIds.isEmpty()
                ? List.of()
                : aiTradeAnalysisRepository.findByUserIdAndTradeIdInAndCreatedAtBetweenOrderByCreatedAtDesc(
                        user.getId(),
                        tradeIds,
                        startInstant,
                        endInstant
                );
        long analysesElapsedMs = elapsedMs(analysesStartedAt);

        long coachStartedAt = System.nanoTime();
        List<AiCoachSnapshot> coachSnapshots = accountId == null
                ? aiCoachSnapshotRepository.findByUserIdAndPeriodStartGreaterThanEqualOrderByPeriodStartDesc(user.getId(), startDate)
                : aiCoachSnapshotRepository.findByUserIdAndAccountIdAndPeriodStartGreaterThanEqualOrderByPeriodStartDesc(user.getId(), accountId, startDate);
        long coachElapsedMs = elapsedMs(coachStartedAt);

        long violationsStartedAt = System.nanoTime();
        List<TradeRuleViolation> violations = tradeRuleViolationRepository.findForTradingDna(
                user.getId(),
                accountId,
                startDate,
                endDate
        );
        long violationsElapsedMs = elapsedMs(violationsStartedAt);

        BigDecimal netPnl = trades.stream().map(this::pnl).reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal winRate = percent(
                BigDecimal.valueOf(trades.stream().filter(t -> pnl(t).compareTo(BigDecimal.ZERO) > 0).count()),
                BigDecimal.valueOf(trades.size())
        );
        List<TradingDnaTimelinePoint> timeline = buildTimeline(startDate, endDate, trades, violations);
        Map<String, Long> mistakeCounts = countMistakes(trades);
        Map<String, Long> violationCounts = violations.stream()
                .collect(Collectors.groupingBy(TradeRuleViolation::getRuleTitle, LinkedHashMap::new, Collectors.counting()));

        long behavioralInsightsStartedAt = System.nanoTime();
        List<String> behavioralInsights = tradeAnalyticsService.getBehavioralInsights(user, accountId)
                .stream()
                .map(this::formatBehavioralInsight)
                .toList();
        long behavioralInsightsElapsedMs = elapsedMs(behavioralInsightsStartedAt);

        int disciplineScore = averageTimeline(timeline, TradingDnaTimelinePoint::getDisciplineScore);
        int consistencyScore = averageTimeline(timeline, TradingDnaTimelinePoint::getConsistencyScore);
        int executionScore = averageInteger(trades.stream().map(Trade::getExecutionRating).filter(Objects::nonNull).map(v -> v * 10).toList(), 70);
        int patienceScore = clamp(100 - (int) Math.min(70, detectRevengeTrades(trades) * 12L + detectOvertradeDays(trades) * 8L));

        TradingDnaContext context = TradingDnaContext.of(
                user.getId(),
                accountId,
                includeAllAccounts,
                days,
                startDate,
                endDate,
                trades.size(),
                netPnl,
                winRate,
                bestByPnl(trades, this::setup),
                worstByPnl(trades, this::setup),
                bestByPnl(trades, this::session),
                worstByPnl(trades, this::session),
                mistakeCounts,
                pnlByClassifier(trades, this::setup),
                pnlByClassifier(trades, Trade::getEmotion),
                pnlByClassifier(trades, trade -> trade.getConfidence() == null ? null : String.valueOf(trade.getConfidence())),
                violationCounts,
                journalMetadataExamples(trades),
                behavioralInsights,
                reviews.stream().map(AiTradeReview::getSummary).filter(this::hasText).limit(8).toList(),
                analyses.stream().flatMap(a -> parseList(a.getStrengthsJson()).stream()).limit(10).toList(),
                analyses.stream().flatMap(a -> parseList(a.getMistakesJson()).stream()).limit(10).toList(),
                coachSnapshots.stream().map(AiCoachSnapshot::getAiResponseJson).filter(this::hasText).limit(8).toList(),
                buildSignals(trades, mistakeCounts, violationCounts),
                timeline,
                disciplineScore,
                executionScore,
                patienceScore,
                consistencyScore
        );
        log.debug("Trading DNA context built. userId={}, accountId={}, includeAllAccounts={}, days={}, tradeCount={}, reviewsQueryMs={}, analysesQueryMs={}, coachQueryMs={}, violationsQueryMs={}, behavioralInsightsMs={}, totalMs={}",
                user.getId(), accountId, includeAllAccounts, days, trades.size(), reviewsElapsedMs, analysesElapsedMs, coachElapsedMs, violationsElapsedMs, behavioralInsightsElapsedMs, elapsedMs(contextStartedAt));
        return context;
    }

    private List<TradingDnaTimelinePoint> buildTimeline(
            LocalDate startDate,
            LocalDate endDate,
            List<Trade> trades,
            List<TradeRuleViolation> violations
    ) {
        Map<LocalDate, List<Trade>> tradesByDate = trades.stream()
                .filter(t -> t.getTradeDate() != null)
                .collect(Collectors.groupingBy(Trade::getTradeDate));
        Map<LocalDate, Long> violationsByDate = violations.stream()
                .filter(v -> v.getTradeDate() != null)
                .collect(Collectors.groupingBy(TradeRuleViolation::getTradeDate, Collectors.counting()));

        List<TradingDnaTimelinePoint> points = new ArrayList<>();
        for (LocalDate date = startDate; !date.isAfter(endDate); date = date.plusDays(1)) {
            List<Trade> dayTrades = tradesByDate.getOrDefault(date, List.of());
            long losses = dayTrades.stream().filter(t -> pnl(t).compareTo(BigDecimal.ZERO) < 0).count();
            long wins = dayTrades.stream().filter(t -> pnl(t).compareTo(BigDecimal.ZERO) > 0).count();
            int violationPenalty = violationsByDate.getOrDefault(date, 0L).intValue() * 8;
            int overtradePenalty = Math.max(0, dayTrades.size() - 4) * 6;
            int lossPenalty = (int) losses * 4;

            TradingDnaTimelinePoint point = new TradingDnaTimelinePoint();
            point.setDate(date);
            point.setTradeCount(dayTrades.size());
            point.setWinRate(percent(BigDecimal.valueOf(wins), BigDecimal.valueOf(dayTrades.size())));
            point.setDisciplineScore(clamp(100 - violationPenalty - overtradePenalty));
            point.setEmotionalStabilityScore(clamp(100 - lossPenalty - overtradePenalty - detectRevengeTrades(dayTrades) * 15));
            point.setConsistencyScore(clamp(100 - Math.abs(dayTrades.size() - 2) * 8 - violationPenalty));
            points.add(point);
        }

        return points;
    }

    private TradingDnaResponse toResponse(TradingDnaSnapshot snapshot, boolean cached) {
        TradingDnaAiResult ai = readJson(snapshot.getAiResponseJson(), TradingDnaAiResult.class, deterministicFallback(null));
        List<TradingDnaTimelinePoint> timeline = readTimeline(snapshot.getTimelineJson());

        TradingDnaResponse response = new TradingDnaResponse();
        response.setId(snapshot.getId());
        response.setAccountId(snapshot.getAccountId());
        response.setDays(snapshot.getDays());
        response.setIncludeAllAccounts(snapshot.getIncludeAllAccounts());
        response.setStatus(snapshot.getStatus());
        response.setCached(cached);
        response.setLimitReached(false);
        response.setMessage(null);
        response.setTotalTrades(snapshot.getTotalTrades());
        response.setLastGeneratedAt(snapshot.getUpdatedAt());
        response.setStrengths(nonNull(ai.getStrengths()));
        response.setWeaknesses(nonNull(ai.getWeaknesses()));
        response.setEmotionalPatterns(nonNull(ai.getEmotionalPatterns()));
        response.setRiskPatterns(nonNull(ai.getRiskPatterns()));
        response.setBestConditions(nonNull(ai.getBestConditions()));
        response.setWorstConditions(nonNull(ai.getWorstConditions()));
        response.setRecurringMistakes(nonNull(ai.getRecurringMistakes()));
        response.setImprovementTrend(ai.getImprovementTrend());
        response.setConsistencyScore(score(ai.getConsistencyScore()));
        response.setDisciplineScore(score(ai.getDisciplineScore()));
        response.setExecutionScore(score(ai.getExecutionScore()));
        response.setPatienceScore(score(ai.getPatienceScore()));
        response.setConfidenceSummary(ai.getConfidenceSummary());
        response.setCoachingSummary(ai.getCoachingSummary());
        response.setTimeline(timeline);
        return response;
    }

    private TradingDnaResponse emptyResponse(Long accountId, Integer days, boolean includeAllAccounts, String message) {
        TradingDnaAiResult ai = insufficientDataResult(null);
        TradingDnaResponse response = new TradingDnaResponse();
        response.setId(null);
        response.setAccountId(accountId);
        response.setDays(days);
        response.setIncludeAllAccounts(includeAllAccounts);
        response.setStatus("INSUFFICIENT_DATA");
        response.setCached(false);
        response.setLimitReached(false);
        response.setMessage(message);
        response.setTotalTrades(0);
        response.setLastGeneratedAt(null);
        response.setStrengths(nonNull(ai.getStrengths()));
        response.setWeaknesses(nonNull(ai.getWeaknesses()));
        response.setEmotionalPatterns(nonNull(ai.getEmotionalPatterns()));
        response.setRiskPatterns(nonNull(ai.getRiskPatterns()));
        response.setBestConditions(nonNull(ai.getBestConditions()));
        response.setWorstConditions(nonNull(ai.getWorstConditions()));
        response.setRecurringMistakes(nonNull(ai.getRecurringMistakes()));
        response.setImprovementTrend(ai.getImprovementTrend());
        response.setConsistencyScore(score(ai.getConsistencyScore()));
        response.setDisciplineScore(score(ai.getDisciplineScore()));
        response.setExecutionScore(score(ai.getExecutionScore()));
        response.setPatienceScore(score(ai.getPatienceScore()));
        response.setConfidenceSummary(ai.getConfidenceSummary());
        response.setCoachingSummary(message == null ? ai.getCoachingSummary() : message);
        response.setTimeline(List.of());
        return response;
    }

    private TradingDnaResponse limitReachedResponse(Long accountId, Integer days, boolean includeAllAccounts, String message) {
        TradingDnaResponse response = emptyResponse(accountId, days, includeAllAccounts, message);
        response.setStatus("LIMIT_REACHED");
        response.setLimitReached(true);
        response.setMessage(message);
        response.setWeaknesses(List.of());
        response.setCoachingSummary(message);
        return response;
    }

    private TradingDnaAiResult insufficientDataResult(TradingDnaContext context) {
        int trades = context == null ? 0 : context.getTotalTrades();
        return TradingDnaAiResult.builder()
                .strengths(List.of())
                .weaknesses(List.of("Not enough trade history yet for reliable Trading DNA."))
                .emotionalPatterns(List.of())
                .riskPatterns(List.of())
                .bestConditions(List.of())
                .worstConditions(List.of())
                .recurringMistakes(List.of())
                .improvementTrend("Import at least " + MIN_TRADES_FOR_AI + " trades to generate a reliable long-term trend.")
                .consistencyScore(0)
                .disciplineScore(0)
                .executionScore(0)
                .patienceScore(0)
                .confidenceSummary("Current sample size: " + trades + " trades.")
                .coachingSummary("Import more trades, then generate Trading DNA to identify recurring behavior.")
                .build();
    }

    private TradingDnaAiResult deterministicFallback(TradingDnaContext context) {
        if (context == null) {
            return insufficientDataResult(null);
        }

        return TradingDnaAiResult.builder()
                .strengths(cleanList(List.of(
                        condition("Best setup archetype: ", context.getBestSetup()),
                        condition("Best session: ", context.getBestSession()),
                        context.getWinRate().compareTo(BigDecimal.valueOf(50)) >= 0 ? "Win rate is above 50%." : null
                )))
                .weaknesses(context.getDeterministicSignals())
                .emotionalPatterns(List.of("Review clustered entries after losses for revenge trading risk."))
                .riskPatterns(List.of("Keep risk fixed while confidence changes across sessions."))
                .bestConditions(cleanList(List.of(condition("Trade more selectively during ", context.getBestSession()))))
                .worstConditions(cleanList(List.of(condition("Reduce size or skip marginal setups during ", context.getWorstSession()))))
                .recurringMistakes(context.getMistakeCounts().entrySet().stream()
                        .limit(6)
                        .map(e -> e.getKey() + " (" + e.getValue() + "x)")
                        .toList())
                .improvementTrend("Trend is calculated from consistency, discipline, win rate, and emotional stability over time.")
                .consistencyScore(context.getDeterministicConsistencyScore())
                .disciplineScore(context.getDeterministicDisciplineScore())
                .executionScore(context.getDeterministicExecutionScore())
                .patienceScore(context.getDeterministicPatienceScore())
                .confidenceSummary("Confidence should be anchored to your best setup and strongest session, not recent P&L.")
                .coachingSummary("Your next focus is repeatable execution: one best setup, one risk model, and a mandatory pause after losses.")
                .build();
    }

    private List<String> buildSignals(List<Trade> trades, Map<String, Long> mistakes, Map<String, Long> violations) {
        List<String> signals = new ArrayList<>();
        if (!mistakes.isEmpty()) {
            signals.add("Most repeated mistake: " + mistakes.entrySet().iterator().next().getKey());
        }
        List<String> metadataExamples = journalMetadataExamples(trades);
        if (!metadataExamples.isEmpty()) {
            signals.add("Journal metadata available: " + String.join("; ", metadataExamples.stream().limit(3).toList()));
        }
        if (!violations.isEmpty()) {
            signals.add("Most repeated rule violation: " + violations.entrySet().iterator().next().getKey());
        }
        if (detectRevengeTrades(trades) >= 2) {
            signals.add("Possible revenge trading detected after losing trades.");
        }
        if (detectOvertradeDays(trades) > 0) {
            signals.add("Overtrading risk detected on high-volume days.");
        }
        if (detectFridayWeakness(trades)) {
            signals.add("Friday performance is weaker than other weekdays.");
        }
        return signals;
    }

    private int detectRevengeTrades(List<Trade> trades) {
        List<Trade> sorted = trades.stream()
                .filter(t -> t.getEntryTime() != null && t.getExitTime() != null)
                .sorted(Comparator.comparing(Trade::getEntryTime))
                .toList();
        int count = 0;
        for (int i = 1; i < sorted.size(); i++) {
            Trade previous = sorted.get(i - 1);
            Trade current = sorted.get(i);
            long minutes = Duration.between(previous.getExitTime(), current.getEntryTime()).toMinutes();
            if (pnl(previous).compareTo(BigDecimal.ZERO) < 0 && minutes >= 0 && minutes <= 10) {
                count++;
            }
        }
        return count;
    }

    private int detectOvertradeDays(List<Trade> trades) {
        return (int) trades.stream()
                .filter(t -> t.getTradeDate() != null)
                .collect(Collectors.groupingBy(Trade::getTradeDate, Collectors.counting()))
                .values()
                .stream()
                .filter(count -> count > 5)
                .count();
    }

    private boolean detectFridayWeakness(List<Trade> trades) {
        BigDecimal fridayPnl = trades.stream()
                .filter(t -> t.getTradeDate() != null && t.getTradeDate().getDayOfWeek() == DayOfWeek.FRIDAY)
                .map(this::pnl)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal otherPnl = trades.stream()
                .filter(t -> t.getTradeDate() != null && t.getTradeDate().getDayOfWeek() != DayOfWeek.FRIDAY)
                .map(this::pnl)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        return fridayPnl.compareTo(BigDecimal.ZERO) < 0 && fridayPnl.compareTo(otherPnl) < 0;
    }

    private Map<String, Long> countMistakes(List<Trade> trades) {
        return trades.stream()
                .map(t -> t.getMistakeTag() == null ? null : t.getMistakeTag().name().replace("_", " "))
                .filter(this::hasText)
                .collect(Collectors.groupingBy(Function.identity(), LinkedHashMap::new, Collectors.counting()))
                .entrySet()
                .stream()
                .sorted(Map.Entry.<String, Long>comparingByValue().reversed())
                .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue, (a, b) -> a, LinkedHashMap::new));
    }

    private String hashFingerprint(Long userId, Long accountId, boolean includeAllAccounts, int days, LocalDate startDate, LocalDate endDate) {
        Object row = tradeRepository.tradingDnaFingerprint(userId, accountId, startDate, endDate);
        Object[] values = row instanceof Object[] array ? array : new Object[]{};
        return sha256(String.join("|",
                String.valueOf(userId),
                String.valueOf(accountId),
                String.valueOf(includeAllAccounts),
                String.valueOf(days),
                String.valueOf(startDate),
                String.valueOf(endDate),
                String.valueOf(valueAt(values, 0)),
                String.valueOf(valueAt(values, 1)),
                String.valueOf(valueAt(values, 2)),
                String.valueOf(valueAt(values, 3))
        ));
    }

    private Object valueAt(Object[] values, int index) {
        return index < values.length ? values[index] : "";
    }

    private String sha256(String value) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(value.getBytes(StandardCharsets.UTF_8));
            StringBuilder hex = new StringBuilder();
            for (byte b : hash) {
                hex.append(String.format("%02x", b));
            }
            return hex.toString();
        } catch (Exception e) {
            throw new IllegalStateException("Unable to hash Trading DNA context", e);
        }
    }

    private long elapsedMs(long startedAtNanos) {
        return Duration.ofNanos(System.nanoTime() - startedAtNanos).toMillis();
    }

    private String bestByPnl(List<Trade> trades, Function<Trade, String> classifier) {
        return byPnl(trades, classifier, true);
    }

    private String worstByPnl(List<Trade> trades, Function<Trade, String> classifier) {
        return byPnl(trades, classifier, false);
    }

    private String byPnl(List<Trade> trades, Function<Trade, String> classifier, boolean best) {
        Comparator<Map.Entry<String, BigDecimal>> comparator = Map.Entry.comparingByValue();
        return trades.stream()
                .filter(t -> hasText(classifier.apply(t)))
                .collect(Collectors.groupingBy(classifier, Collectors.reducing(BigDecimal.ZERO, this::pnl, BigDecimal::add)))
                .entrySet()
                .stream()
                .max(best ? comparator : comparator.reversed())
                .map(Map.Entry::getKey)
                .orElse("Not enough data");
    }

    private BigDecimal pnl(Trade trade) {
        return trade.getPnlAmount() == null ? BigDecimal.ZERO : trade.getPnlAmount();
    }

    private String setup(Trade trade) {
        return trade.getSetup() == null ? null : trade.getSetup().name().replace("_", " ");
    }

    private Map<String, BigDecimal> pnlByClassifier(List<Trade> trades, Function<Trade, String> classifier) {
        return trades.stream()
                .filter(trade -> hasText(classifier.apply(trade)))
                .collect(Collectors.groupingBy(
                        trade -> classifier.apply(trade).trim().replace("_", " "),
                        LinkedHashMap::new,
                        Collectors.reducing(BigDecimal.ZERO, this::pnl, BigDecimal::add)
                ));
    }

    private List<String> journalMetadataExamples(List<Trade> trades) {
        return trades.stream()
                .map(trade -> {
                    List<String> parts = new ArrayList<>();
                    if (trade.getSetup() != null) {
                        parts.add("setup=" + trade.getSetup().name());
                    }
                    if (trade.getConfidence() != null) {
                        parts.add("confidence=" + trade.getConfidence());
                    }
                    if (hasText(trade.getEmotion())) {
                        parts.add("emotion=" + trade.getEmotion());
                    }
                    if (hasText(trade.getNotes())) {
                        parts.add("notes=" + trade.getNotes());
                    }
                    return parts.isEmpty() ? null : trade.getSymbol() + " " + pnl(trade) + " (" + String.join(", ", parts) + ")";
                })
                .filter(this::hasText)
                .limit(12)
                .toList();
    }

    private String session(Trade trade) {
        return trade.getSession() == null ? null : trade.getSession().name().replace("_", " ");
    }

    private BigDecimal percent(BigDecimal numerator, BigDecimal denominator) {
        if (denominator == null || denominator.compareTo(BigDecimal.ZERO) == 0) {
            return BigDecimal.ZERO;
        }
        return numerator.multiply(BigDecimal.valueOf(100)).divide(denominator, 2, RoundingMode.HALF_UP);
    }

    private int averageTimeline(List<TradingDnaTimelinePoint> points, Function<TradingDnaTimelinePoint, Integer> getter) {
        List<Integer> values = points.stream()
                .filter(point -> point.getTradeCount() != null && point.getTradeCount() > 0)
                .map(getter)
                .filter(Objects::nonNull)
                .toList();
        return averageInteger(values, 0);
    }

    private int averageInteger(List<Integer> values, int fallback) {
        if (values == null || values.isEmpty()) {
            return fallback;
        }
        return clamp((int) Math.round(values.stream().mapToInt(Integer::intValue).average().orElse(fallback)));
    }

    private int score(Integer value) {
        return value == null ? 0 : clamp(value);
    }

    private int clamp(int value) {
        return Math.max(0, Math.min(100, value));
    }

    private int normalizeDays(Integer days) {
        if (days == null) {
            return 30;
        }
        return Math.max(7, Math.min(365, days));
    }

    private String formatBehavioralInsight(BehavioralInsightDto insight) {
        return insight.getTitle() + ": " + insight.getMessage();
    }

    private List<String> parseList(String json) {
        if (!hasText(json)) {
            return List.of();
        }
        try {
            return objectMapper.readValue(json, new TypeReference<List<String>>() {});
        } catch (Exception e) {
            return List.of(json);
        }
    }

    private List<TradingDnaTimelinePoint> readTimeline(String json) {
        if (!hasText(json)) {
            return List.of();
        }
        try {
            return objectMapper.readValue(json, new TypeReference<List<TradingDnaTimelinePoint>>() {});
        } catch (Exception e) {
            return List.of();
        }
    }

    private <T> T readJson(String json, Class<T> type, T fallback) {
        if (!hasText(json)) {
            return fallback;
        }
        try {
            return objectMapper.readValue(json, type);
        } catch (Exception e) {
            return fallback;
        }
    }

    private String writeJson(Object value) {
        try {
            return objectMapper.writeValueAsString(value);
        } catch (Exception e) {
            throw new IllegalStateException("Unable to serialize Trading DNA data", e);
        }
    }

    private boolean hasText(String value) {
        return value != null && !value.isBlank();
    }

    private List<String> nonNull(List<String> values) {
        return values == null ? List.of() : values;
    }

    private String condition(String prefix, String value) {
        if (!hasText(value) || "Not enough data".equals(value)) {
            return null;
        }
        return prefix + value;
    }

    private List<String> cleanList(List<String> values) {
        return values.stream().filter(this::hasText).toList();
    }
}
