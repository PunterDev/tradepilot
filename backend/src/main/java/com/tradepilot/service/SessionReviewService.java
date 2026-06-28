package com.tradepilot.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.tradepilot.billing.Feature;
import com.tradepilot.billing.FeatureAccessService;
import com.tradepilot.billing.UsageLimitService;
import com.tradepilot.billing.UsageType;
import com.tradepilot.dto.session.AiSessionReviewResponse;
import com.tradepilot.dto.session.SessionMistakeResponse;
import com.tradepilot.dto.session.SessionReplayInsightsResponse;
import com.tradepilot.dto.session.SessionReviewResponse;
import com.tradepilot.dto.session.SessionTimelineTradeResponse;
import com.tradepilot.dto.trade.TradeResponse;
import com.tradepilot.entity.AiSessionReview;
import com.tradepilot.entity.Trade;
import com.tradepilot.entity.TradeRuleViolation;
import com.tradepilot.entity.TradingAccount;
import com.tradepilot.entity.User;
import com.tradepilot.enums.OutcomeType;
import com.tradepilot.enums.Side;
import com.tradepilot.observability.ObservabilityService;
import com.tradepilot.repository.AiSessionReviewRepository;
import com.tradepilot.repository.TradeRepository;
import com.tradepilot.repository.TradeRuleViolationRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.DayOfWeek;
import java.time.Duration;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

@Service
public class SessionReviewService {
    @java.lang.SuppressWarnings("all")
    private static final org.slf4j.Logger log = org.slf4j.LoggerFactory.getLogger(SessionReviewService.class);
    private static final Duration SESSION_REVIEW_TTL = Duration.ofSeconds(45);
    private static final BigDecimal SCRATCH_TRADE_DOLLAR_THRESHOLD = new BigDecimal("5.00");
    private final TradingAccountService tradingAccountService;
    private final TradeService tradeService;
    private final TradeRepository tradeRepository;
    private final TradeRuleViolationRepository violationRepository;
    private final AiSessionReviewRepository aiSessionReviewRepository;
    private final AiSessionReviewGeneratorService generatorService;
    private final UsageLimitService usageLimitService;
    private final FeatureAccessService featureAccessService;
    private final ObjectMapper objectMapper;
    private final ObservabilityService observabilityService;
    private final AggregateCacheService aggregateCacheService;
    private final PerformanceCacheInvalidationService cacheInvalidationService;

    @Transactional(readOnly = true)
    public SessionReviewResponse getSessionReview(User user, Long accountId, LocalDate date) {
        featureAccessService.requireFeature(user, Feature.SESSION_REVIEW);
        log.info("Session review requested. userId={}, accountId={}, date={}", user.getId(), accountId, date);
        SessionData data = loadSessionData(user, accountId, date);
        String inputHash = reviewHash(data);
        return aggregateCacheService.get(sessionReviewCacheKey(user.getId(), accountId, date, inputHash), SESSION_REVIEW_TTL, () -> {
            AiSessionReviewResponse cachedReview = data.totalTrades() == 0 ? null : aiSessionReviewRepository.findTopByUserIdAndAccountIdAndSessionDateOrderByCreatedAtDesc(user.getId(), accountId, date).map(this::mapReview).orElse(null);
            log.trace("Session review loaded. userId={}, accountId={}, date={}, tradeCount={}, cachedAiReview={}, inputHash={}", user.getId(), accountId, date, data.totalTrades(), cachedReview != null, inputHash);
            return buildResponse(data, cachedReview, cachedReview != null);
        });
    }

    public SessionReviewResponse generateSessionReview(User user, Long accountId, LocalDate date, boolean forceRefresh) {
        featureAccessService.requireFeature(user, Feature.SESSION_REVIEW);
        long requestStartedAt = System.nanoTime();
        log.info("AI session review generation requested. userId={}, accountId={}, date={}, forceRefresh={}", user.getId(), accountId, date, forceRefresh);
        log.debug("Session review cache lookup flow started. userId={}, accountId={}, date={}, forceRefresh={}", user.getId(), accountId, date, forceRefresh);
        long loadStartedAt = System.nanoTime();
        SessionData data = loadSessionData(user, accountId, date);
        long loadElapsedMs = elapsedMs(loadStartedAt);
        long hashStartedAt = System.nanoTime();
        String inputHash = reviewHash(data);
        long hashElapsedMs = elapsedMs(hashStartedAt);
        if (data.totalTrades() == 0) {
            observabilityService.recordCacheEvent("Session Review", false);
            log.info("AI skipped for Session Review: no trades. userId={}, accountId={}, date={}", user.getId(), accountId, date);
            return buildResponse(data, null, false);
        }
        long cacheLookupStartedAt = System.nanoTime();
        log.debug("Session review AI cache lookup start. userId={}, accountId={}, date={}, inputHash={}, loadSessionMs={}, hashMs={}", user.getId(), accountId, date, inputHash, loadElapsedMs, hashElapsedMs);
        var cached = aiSessionReviewRepository.findTopByUserIdAndAccountIdAndSessionDateAndReviewHashOrderByCreatedAtDesc(user.getId(), accountId, date, inputHash);
        long cacheLookupElapsedMs = elapsedMs(cacheLookupStartedAt);
        if (!forceRefresh && cached.isPresent()) {
            observabilityService.recordCacheEvent("Session Review", true);
            observabilityService.recordCacheEvent("session-review-cache-hit", true);
            log.info("AI session review cache hit. userId={}, accountId={}, date={}, reviewId={}, inputHash={}, cacheLookupMs={}, totalMs={}", user.getId(), accountId, date, cached.get().getId(), inputHash, cacheLookupElapsedMs, elapsedMs(requestStartedAt));
            long responseStartedAt = System.nanoTime();
            SessionReviewResponse response = buildResponse(data, mapReview(cached.get()), true);
            log.debug("Session review cached response built. userId={}, accountId={}, date={}, responseBuildMs={}, totalMs={}", user.getId(), accountId, date, elapsedMs(responseStartedAt), elapsedMs(requestStartedAt));
            return response;
        }
        observabilityService.recordCacheEvent("Session Review", false);
        observabilityService.recordCacheEvent("session-review-cache-miss", false);
        log.warn("AI session review cache miss. userId={}, accountId={}, date={}, forceRefresh={}, inputHash={}, cacheLookupMs={}", user.getId(), accountId, date, forceRefresh, inputHash, cacheLookupElapsedMs);
        usageLimitService.enforceAiUsage(user, UsageType.SESSION_REVIEW);
        long aiStartedAt = System.nanoTime();
        log.debug("Session review OpenAI generation start. userId={}, accountId={}, date={}, inputHash={}", user.getId(), accountId, date, inputHash);
        AiSessionReviewResponse generated = generatorService.generate(user.getId(), date, data.tradeResponses(), data.totalPnl(), data.winRate(), data.mistakeSummary(), data.disciplineScore());
        long aiElapsedMs = elapsedMs(aiStartedAt);
        log.debug("Session review OpenAI generation end. userId={}, accountId={}, date={}, inputHash={}, elapsedMs={}", user.getId(), accountId, date, inputHash, aiElapsedMs);
        long existingReviewStartedAt = System.nanoTime();
        AiSessionReview review = aiSessionReviewRepository.findTopByUserIdAndAccountIdAndSessionDateOrderByCreatedAtDesc(user.getId(), accountId, date).orElseGet(AiSessionReview::new);
        long existingReviewElapsedMs = elapsedMs(existingReviewStartedAt);
        review.setUserId(user.getId());
        review.setAccountId(accountId);
        review.setSessionDate(date);
        review.setReviewHash(inputHash);
        review.setSummary(generated.getSummary());
        review.setWhatWentWell(generated.getWhatWentWell());
        review.setWhatHurtPerformance(generated.getWhatHurtPerformance());
        review.setBiggestMistake(generated.getBiggestMistake());
        review.setBestSetup(generated.getBestSetup());
        review.setWorstDecision(generated.getWorstDecision());
        review.setEmotionalPattern(generated.getEmotionalPattern());
        review.setExecutionPattern(generated.getExecutionPattern());
        review.setRiskManagementPattern(generated.getRiskManagementPattern());
        review.setOneRuleForTomorrow(generated.getOneRuleForTomorrow());
        review.setCreatedAt(LocalDateTime.now());
        long saveStartedAt = System.nanoTime();
        AiSessionReview saved = aiSessionReviewRepository.save(review);
        long saveElapsedMs = elapsedMs(saveStartedAt);
        cacheInvalidationService.invalidateSessionReview(user.getId());
        usageLimitService.recordAiUsage(user, UsageType.SESSION_REVIEW, saved.getId());
        log.info("AI session review generation completed. userId={}, accountId={}, date={}, reviewId={}, loadSessionMs={}, hashMs={}, cacheLookupMs={}, aiMs={}, existingReviewLookupMs={}, saveMs={}, totalMs={}", user.getId(), accountId, date, saved.getId(), loadElapsedMs, hashElapsedMs, cacheLookupElapsedMs, aiElapsedMs, existingReviewElapsedMs, saveElapsedMs, elapsedMs(requestStartedAt));
        return buildResponse(data, mapReview(saved), false);
    }

    private SessionData loadSessionData(User user, Long accountId, LocalDate date) {
        long totalStartedAt = System.nanoTime();
        long accountStartedAt = System.nanoTime();
        TradingAccount account = tradingAccountService.getAccountForImport(user, accountId);
        long accountElapsedMs = elapsedMs(accountStartedAt);
        long tradesStartedAt = System.nanoTime();
        List<Trade> trades = tradeRepository.findByUserAndAccount_IdAndTradeDateOrderByEntryTimeAscIdAsc(user, accountId, date);
        long tradesElapsedMs = elapsedMs(tradesStartedAt);
        if (trades.isEmpty()) {
            log.warn("No trades found for session review. userId={}, accountId={}, date={}", user.getId(), accountId, date);
        }
        long mapTradesStartedAt = System.nanoTime();
        List<TradeResponse> tradeResponses = trades.stream().map(tradeService::mapToResponse).toList();
        long mapTradesElapsedMs = elapsedMs(mapTradesStartedAt);
        BigDecimal totalPnl = trades.stream().map(this::pnl).reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal grossProfit = trades.stream().map(this::pnl).filter(value -> value.compareTo(BigDecimal.ZERO) > 0).reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal grossLoss = trades.stream().map(this::pnl).filter(value -> value.compareTo(BigDecimal.ZERO) < 0).reduce(BigDecimal.ZERO, BigDecimal::add);
        long wins = trades.stream().filter(trade -> isMeaningfulWinner(pnl(trade))).count();
        long losses = trades.stream().filter(trade -> isMeaningfulLoser(pnl(trade))).count();
        long breakeven = trades.stream().filter(trade -> isScratchPnl(pnl(trade))).count();
        BigDecimal winRate = trades.isEmpty() ? BigDecimal.ZERO : BigDecimal.valueOf(wins * 100.0 / trades.size()).setScale(2, RoundingMode.HALF_UP);
        BigDecimal largestWinner = trades.stream().map(this::pnl).filter(this::isMeaningfulWinner).max(BigDecimal::compareTo).orElse(BigDecimal.ZERO);
        BigDecimal largestLoser = trades.stream().map(this::pnl).filter(this::isMeaningfulLoser).min(BigDecimal::compareTo).orElse(BigDecimal.ZERO);
        BigDecimal averageWinner = averagePnl(trades.stream().map(this::pnl).filter(this::isMeaningfulWinner).toList());
        BigDecimal averageLoser = averagePnl(trades.stream().map(this::pnl).filter(this::isMeaningfulLoser).toList());
        BigDecimal profitFactor = grossLoss.compareTo(BigDecimal.ZERO) == 0 ? (grossProfit.compareTo(BigDecimal.ZERO) > 0 ? BigDecimal.valueOf(99) : BigDecimal.ZERO) : grossProfit.divide(grossLoss.abs(), 2, RoundingMode.HALF_UP);
        TradeResponse bestTrade = selectBestTrade(tradeResponses);
        TradeResponse worstTrade = selectWorstTrade(tradeResponses);
        Map<String, Long> mistakeSummary = buildMistakeSummary(trades);
        long violationsStartedAt = System.nanoTime();
        List<TradeRuleViolation> violations = violationRepository.findByUserIdAndTradeDateOrderByCreatedAtDesc(user.getId(), date).stream().filter(violation -> trades.stream().anyMatch(trade -> trade.getId().equals(violation.getTradeId()))).toList();
        long violationsElapsedMs = elapsedMs(violationsStartedAt);
        int disciplineScore = Math.max(0, 100 - (violations.size() * 12) - (int) trades.stream().filter(t -> Boolean.FALSE.equals(t.getFollowedPlan())).count() * 8 - (int) trades.stream().filter(t -> t.getMistakeTag() != null).count() * 4);
        List<SessionTimelineTradeResponse> timelineTrades = trades.stream().map(this::mapTimelineTrade).toList();
        List<SessionMistakeResponse> detectedMistakes = detectMistakes(trades, violations, date);
        SessionReplayInsightsResponse replayInsights = buildReplayInsights(trades);
        long previousDayStartedAt = System.nanoTime();
        LocalDate previousTradingDay = tradeRepository.findPreviousTradingDate(user, accountId, date);
        long previousDayElapsedMs = elapsedMs(previousDayStartedAt);
        long nextDayStartedAt = System.nanoTime();
        LocalDate nextTradingDay = tradeRepository.findNextTradingDate(user, accountId, date);
        long nextDayElapsedMs = elapsedMs(nextDayStartedAt);
        log.debug("Session review data loaded. userId={}, accountId={}, date={}, tradeCount={}, accountMs={}, tradesQueryMs={}, mapTradesMs={}, violationsQueryMs={}, previousDayQueryMs={}, nextDayQueryMs={}, totalMs={}", user.getId(), accountId, date, trades.size(), accountElapsedMs, tradesElapsedMs, mapTradesElapsedMs, violationsElapsedMs, previousDayElapsedMs, nextDayElapsedMs, elapsedMs(totalStartedAt));
        return new SessionData(account, date, trades, tradeResponses, timelineTrades, trades.size(), Math.toIntExact(wins), Math.toIntExact(losses), Math.toIntExact(breakeven), totalPnl, grossProfit, grossLoss, largestWinner, largestLoser, averageWinner, averageLoser, winRate, profitFactor, bestTrade, worstTrade, mistakeSummary, detectedMistakes, replayInsights, buildStory(trades, totalPnl, bestTrade, worstTrade, detectedMistakes), grade(totalPnl, winRate, profitFactor, disciplineScore, trades.size()), bestTradeWhy(bestTrade), worstTradeWhy(worstTrade, detectedMistakes), disciplineScore, previousTradingDay, nextTradingDay);
    }

    private SessionReviewResponse buildResponse(SessionData data, AiSessionReviewResponse review, boolean cached) {
        SessionReviewResponse response = new SessionReviewResponse();
        response.setAccountId(data.account().getId());
        response.setDate(data.date());
        response.setSessionDate(data.date());
        response.setAccount(tradingAccountService.mapToResponse(data.account()));
        response.setTrades(data.tradeResponses());
        response.setTimelineTrades(data.timelineTrades());
        response.setTotalTrades(data.totalTrades());
        response.setWinners(data.winners());
        response.setLosers(data.losers());
        response.setBreakevenTrades(data.breakevenTrades());
        response.setNetPnl(data.totalPnl());
        response.setGrossProfit(data.grossProfit());
        response.setGrossLoss(data.grossLoss());
        response.setLargestWinner(data.largestWinner());
        response.setLargestLoser(data.largestLoser());
        response.setAverageWinner(data.averageWinner());
        response.setAverageLoser(data.averageLoser());
        response.setTotalPnl(data.totalPnl());
        response.setWinRate(data.winRate());
        response.setProfitFactor(data.profitFactor());
        response.setSessionGrade(data.sessionGrade());
        response.setSessionStory(data.sessionStory());
        response.setBestTradeWhy(data.bestTradeWhy());
        response.setWorstTradeWhy(data.worstTradeWhy());
        response.setBestTrade(data.bestTrade());
        response.setWorstTrade(data.worstTrade());
        response.setMistakeSummary(data.mistakeSummary());
        response.setDetectedMistakes(data.detectedMistakes());
        response.setReplayInsights(data.replayInsights());
        response.setSessionDisciplineScore(data.disciplineScore());
        response.setAiSessionReview(review);
        response.setAiReviewCached(cached);
        response.setAiGenerated(review != null);
        response.setNoTrades(data.totalTrades() == 0);
        response.setCreditsConsumed(review == null || cached ? 0 : 2);
        response.setPreviousTradingDay(data.previousTradingDay());
        response.setNextTradingDay(data.nextTradingDay());
        return response;
    }

    private Map<String, Long> buildMistakeSummary(List<Trade> trades) {
        Map<String, Long> summary = new LinkedHashMap<>();
        trades.stream().filter(trade -> trade.getMistakeTag() != null).forEach(trade -> summary.merge(trade.getMistakeTag().name(), 1L, Long::sum));
        return summary;
    }

    private AiSessionReviewResponse mapReview(AiSessionReview review) {
        AiSessionReviewResponse response = new AiSessionReviewResponse();
        response.setId(review.getId());
        response.setSummary(review.getSummary());
        response.setWhatWentWell(review.getWhatWentWell());
        response.setWhatHurtPerformance(review.getWhatHurtPerformance());
        response.setBiggestMistake(review.getBiggestMistake());
        response.setBestSetup(review.getBestSetup());
        response.setWorstDecision(review.getWorstDecision());
        response.setEmotionalPattern(review.getEmotionalPattern());
        response.setExecutionPattern(review.getExecutionPattern());
        response.setRiskManagementPattern(review.getRiskManagementPattern());
        response.setOneRuleForTomorrow(review.getOneRuleForTomorrow());
        response.setGeneratedAt(review.getCreatedAt());
        return response;
    }

    private SessionTimelineTradeResponse mapTimelineTrade(Trade trade) {
        SessionTimelineTradeResponse response = new SessionTimelineTradeResponse();
        response.setTradeId(trade.getId());
        response.setSymbol(trade.getSymbol());
        response.setSide(parseSideLabel(trade.getSide()));
        response.setEntryTime(trade.getEntryTime());
        response.setExitTime(trade.getExitTime());
        response.setQuantity(trade.getQuantity());
        response.setPnl(pnl(trade));
        response.setSetup(trade.getSetup());
        response.setConfidence(trade.getConfidence());
        response.setEmotion(trade.getEmotion());
        response.setNotes(trade.getNotes());
        response.setOutcome(isScratchPnl(pnl(trade)) ? OutcomeType.BE : trade.getOutcome());
        return response;
    }

    private BigDecimal pnl(Trade trade) {
        return trade.getPnlAmount() == null ? BigDecimal.ZERO : trade.getPnlAmount();
    }

    private String parseSideLabel(String side) {
        if (side == null || side.isBlank()) {
            return Side.UNKNOWN.name();
        }
        try {
            return Side.valueOf(side.trim().toUpperCase()).name();
        } catch (IllegalArgumentException ignored) {
            return Side.UNKNOWN.name();
        }
    }

    private BigDecimal averagePnl(List<BigDecimal> values) {
        if (values.isEmpty()) {
            return BigDecimal.ZERO;
        }
        BigDecimal total = values.stream().filter(Objects::nonNull).reduce(BigDecimal.ZERO, BigDecimal::add);
        return total.divide(BigDecimal.valueOf(values.size()), 2, RoundingMode.HALF_UP);
    }

    private boolean isScratchPnl(BigDecimal value) {
        return value != null && value.abs().compareTo(SCRATCH_TRADE_DOLLAR_THRESHOLD) <= 0;
    }

    private boolean isMeaningfulWinner(BigDecimal value) {
        return value != null && value.compareTo(SCRATCH_TRADE_DOLLAR_THRESHOLD) > 0;
    }

    private boolean isMeaningfulLoser(BigDecimal value) {
        return value != null && value.compareTo(SCRATCH_TRADE_DOLLAR_THRESHOLD.negate()) < 0;
    }

    private TradeResponse selectBestTrade(List<TradeResponse> trades) {
        return trades.stream().filter(trade -> trade.getPnlAmount() != null).filter(trade -> isMeaningfulWinner(trade.getPnlAmount())).max(Comparator.comparing(TradeResponse::getPnlAmount)).orElse(null);
    }

    private TradeResponse selectWorstTrade(List<TradeResponse> trades) {
        return trades.stream().filter(trade -> trade.getPnlAmount() != null).filter(trade -> isMeaningfulLoser(trade.getPnlAmount())).min(Comparator.comparing(TradeResponse::getPnlAmount)).orElse(null);
    }

    private List<SessionMistakeResponse> detectMistakes(List<Trade> trades, List<TradeRuleViolation> violations, LocalDate date) {
        List<SessionMistakeResponse> mistakes = new ArrayList<>();
        List<Trade> sorted = trades.stream().filter(t -> t.getEntryTime() != null).sorted(Comparator.comparing(Trade::getEntryTime)).toList();
        long revengeTrades = 0;
        for (int i = 1; i < sorted.size(); i++) {
            Trade previous = sorted.get(i - 1);
            Trade current = sorted.get(i);
            if (previous.getExitTime() != null && pnl(previous).compareTo(BigDecimal.ZERO) < 0 && Duration.between(previous.getExitTime(), current.getEntryTime()).toMinutes() <= 10) {
                revengeTrades++;
            }
        }
        if (revengeTrades > 0) {
            mistakes.add(mistake("REVENGE_TRADING", "Revenge trading risk", revengeTrades + " trade(s) happened shortly after a loss.", "HIGH"));
        }
        if (trades.size() > 5) {
            mistakes.add(mistake("OVERTRADING", "Overtrading", trades.size() + " trades in one session. Decision quality often fades after trade five.", "MEDIUM"));
        }
        boolean sizedUpAfterWin = false;
        boolean sizedDownAfterLoss = false;
        for (int i = 1; i < sorted.size(); i++) {
            Trade previous = sorted.get(i - 1);
            Trade current = sorted.get(i);
            int previousQty = previous.getQuantity() == null ? 0 : previous.getQuantity();
            int currentQty = current.getQuantity() == null ? 0 : current.getQuantity();
            if (pnl(previous).compareTo(BigDecimal.ZERO) > 0 && currentQty > previousQty) {
                sizedUpAfterWin = true;
            }
            if (pnl(previous).compareTo(BigDecimal.ZERO) < 0 && currentQty < previousQty) {
                sizedDownAfterLoss = true;
            }
        }
        if (sizedUpAfterWin) {
            mistakes.add(mistake("SIZING_UP_AFTER_WIN", "Sized up after a win", "Contract size increased after a winning trade.", "MEDIUM"));
        }
        if (sizedDownAfterLoss) {
            mistakes.add(mistake("SIZING_DOWN_AFTER_LOSS", "Sized down after a loss", "Size was reduced after a losing trade. Check whether fear changed execution.", "LOW"));
        }
        long outsidePlan = trades.stream().filter(t -> Boolean.FALSE.equals(t.getFollowedPlan())).count() + violations.size();
        if (outsidePlan > 0) {
            mistakes.add(mistake("OUTSIDE_PLAN", "Trading outside plan", outsidePlan + " plan or rule violation(s) detected.", "HIGH"));
        }
        if (date.getDayOfWeek() == DayOfWeek.FRIDAY && trades.stream().map(this::pnl).reduce(BigDecimal.ZERO, BigDecimal::add).compareTo(BigDecimal.ZERO) < 0) {
            mistakes.add(mistake("FRIDAY_ISSUE", "Friday weakness", "This was a losing Friday session. Reduce risk when end-of-week conditions get thin.", "MEDIUM"));
        }
        if (isMonthlyOpex(date) && trades.stream().map(this::pnl).reduce(BigDecimal.ZERO, BigDecimal::add).compareTo(BigDecimal.ZERO) < 0) {
            mistakes.add(mistake("OPEX_ISSUE", "OPEX issue", "Negative performance on monthly OPEX Friday. Consider stricter filters.", "MEDIUM"));
        }
        return mistakes;
    }

    private SessionMistakeResponse mistake(String type, String title, String description, String severity) {
        return SessionMistakeResponse.builder().type(type).title(title).description(description).severity(severity).build();
    }

    private boolean isMonthlyOpex(LocalDate date) {
        return date.getDayOfWeek() == DayOfWeek.FRIDAY && date.getDayOfMonth() >= 15 && date.getDayOfMonth() <= 21;
    }

    private SessionReplayInsightsResponse buildReplayInsights(List<Trade> trades) {
        List<Trade> sorted = trades.stream().filter(t -> t.getEntryTime() != null).sorted(Comparator.comparing(Trade::getEntryTime)).toList();
        Trade longest = sorted.stream().filter(t -> t.getEntryTime() != null && t.getExitTime() != null).max(Comparator.comparingLong(this::holdMinutes)).orElse(null);
        Trade shortest = sorted.stream().filter(t -> t.getEntryTime() != null && t.getExitTime() != null).min(Comparator.comparingLong(this::holdMinutes)).orElse(null);
        Map<Integer, BigDecimal> pnlByHour = sorted.stream().collect(Collectors.groupingBy(t -> t.getEntryTime().getHour(), LinkedHashMap::new, Collectors.reducing(BigDecimal.ZERO, this::pnl, BigDecimal::add)));
        Map.Entry<Integer, BigDecimal> bestHour = pnlByHour.entrySet().stream().filter(entry -> isMeaningfulWinner(entry.getValue())).max(Map.Entry.comparingByValue()).orElse(null);
        Map.Entry<Integer, BigDecimal> worstHour = pnlByHour.entrySet().stream().filter(entry -> entry.getValue().compareTo(BigDecimal.ZERO) < 0).min(Map.Entry.comparingByValue()).orElse(null);
        return SessionReplayInsightsResponse.of(sorted.isEmpty() ? null : mapTimelineTrade(sorted.get(0)), sorted.isEmpty() ? null : mapTimelineTrade(sorted.get(sorted.size() - 1)), sorted.stream().filter(t -> isMeaningfulWinner(pnl(t))).max(Comparator.comparing(this::pnl)).map(this::mapTimelineTrade).orElse(null), sorted.stream().filter(t -> isMeaningfulLoser(pnl(t))).min(Comparator.comparing(this::pnl)).map(this::mapTimelineTrade).orElse(null), longest == null ? null : mapTimelineTrade(longest), shortest == null ? null : mapTimelineTrade(shortest), longest == null ? null : Math.toIntExact(holdMinutes(longest)), shortest == null ? null : Math.toIntExact(holdMinutes(shortest)), bestHour == null ? null : bestHour.getKey(), bestHour == null ? null : bestHour.getValue(), worstHour == null ? null : worstHour.getKey(), worstHour == null ? null : worstHour.getValue(), sorted.isEmpty() ? null : sorted.get(0).getEntryTime(), sorted.isEmpty() ? null : sorted.get(sorted.size() - 1).getEntryTime());
    }

    private long holdMinutes(Trade trade) {
        return Math.max(0, Duration.between(trade.getEntryTime(), trade.getExitTime()).toMinutes());
    }

    private String grade(BigDecimal pnl, BigDecimal winRate, BigDecimal profitFactor, int disciplineScore, int tradeCount) {
        int score = disciplineScore;
        score += pnl.compareTo(BigDecimal.ZERO) > 0 ? 8 : pnl.compareTo(BigDecimal.ZERO) < 0 ? -12 : 0;
        score += winRate.compareTo(BigDecimal.valueOf(60)) >= 0 ? 8 : winRate.compareTo(BigDecimal.valueOf(40)) < 0 ? -8 : 0;
        score += profitFactor.compareTo(BigDecimal.valueOf(1.5)) >= 0 ? 8 : profitFactor.compareTo(BigDecimal.ONE) < 0 ? -8 : 0;
        score -= Math.max(0, tradeCount - 5) * 3;
        if (score >= 96) return "A+";
        if (score >= 88) return "A";
        if (score >= 78) return "B";
        if (score >= 66) return "C";
        if (score >= 52) return "D";
        return "F";
    }

    private String buildStory(List<Trade> trades, BigDecimal totalPnl, TradeResponse bestTrade, TradeResponse worstTrade, List<SessionMistakeResponse> mistakes) {
        if (trades.isEmpty()) {
            return "No trades were found for this session. Select a trading day with executions to review the film.";
        }
        String start = pnl(trades.get(0)).compareTo(BigDecimal.ZERO) >= 0 ? "Session started constructively with an early trade that did not put the day under pressure." : "Session started under pressure after the first trade closed red.";
        String best;
        if (bestTrade == null) {
            Trade scratchWinner = largestScratchWinner(trades);
            if (scratchWinner != null) {
                best = " The only positive trade was essentially a scratch winner on " + scratchWinner.getSymbol() + " " + parseSideLabel(scratchWinner.getSide()) + " for " + pnl(scratchWinner) + " and did not materially affect the session.";
            } else {
                best = trades.size() == 1 && pnl(trades.get(0)).compareTo(BigDecimal.ZERO) < 0 ? " The session was defined by a single losing trade." : " There was no meaningful winning trade in this session.";
            }
        } else {
            best = " The strongest trade was " + bestTrade.getSymbol() + " " + bestTrade.getSide() + " for " + bestTrade.getPnlAmount() + ".";
        }
        String worst = worstTrade == null ? "" : " The largest damage came from " + worstTrade.getSymbol() + " " + worstTrade.getSide() + " for " + worstTrade.getPnlAmount() + ".";
        String ending = totalPnl.compareTo(BigDecimal.ZERO) >= 0 ? " The session finished positive, so the key is protecting the behaviors that created clean expectancy." : " The session finished negative, so the review should focus on the specific decision that changed the tone.";
        String mistake = mistakes.isEmpty() ? "" : " Detected issue: " + mistakes.get(0).getTitle() + ".";
        return start + best + worst + mistake + ending;
    }

    private String bestTradeWhy(TradeResponse trade) {
        if (trade == null) {
            return "No meaningful winning trade was found. Scratch results are useful context, but not strong enough to offset session risk.";
        }
        return "It produced the strongest P&L in the session and is the trade to reverse-engineer for repeatable execution.";
    }

    private Trade largestScratchWinner(List<Trade> trades) {
        return trades.stream().filter(trade -> pnl(trade).compareTo(BigDecimal.ZERO) > 0).filter(trade -> isScratchPnl(pnl(trade))).max(Comparator.comparing(this::pnl)).orElse(null);
    }

    private String worstTradeWhy(TradeResponse trade, List<SessionMistakeResponse> mistakes) {
        if (trade == null) {
            return "No losing trade was found.";
        }
        if (!mistakes.isEmpty()) {
            return "It did the most damage and connects to the session\'s main detected issue: " + mistakes.get(0).getTitle() + ".";
        }
        return "It did the most damage. Review entry location, confirmation, and whether risk was predefined.";
    }

    private String reviewHash(SessionData data) {
        try {
            String raw = objectMapper.writeValueAsString(Map.of("date", data.date(), "accountId", data.account().getId(), "trades", data.tradeResponses(), "version", "session-review-v1"));
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(raw.getBytes(StandardCharsets.UTF_8));
            StringBuilder hex = new StringBuilder();
            for (byte b : hash) {
                hex.append(String.format("%02x", b));
            }
            return hex.toString();
        } catch (Exception error) {
            log.warn("Session review hash generation failed. accountId={}, date={}", data.account().getId(), data.date(), error);
            return "hash-error";
        }
    }

    private String sessionReviewCacheKey(Long userId, Long accountId, LocalDate date, String inputHash) {
        return "session-review:" + userId + ":" + accountId + ":" + date + ":" + inputHash;
    }

    private long elapsedMs(long startedAtNanos) {
        return Duration.ofNanos(System.nanoTime() - startedAtNanos).toMillis();
    }


    private record SessionData(TradingAccount account, LocalDate date, List<Trade> trades, List<TradeResponse> tradeResponses, List<SessionTimelineTradeResponse> timelineTrades, Integer totalTrades, Integer winners, Integer losers, Integer breakevenTrades, BigDecimal totalPnl, BigDecimal grossProfit, BigDecimal grossLoss, BigDecimal largestWinner, BigDecimal largestLoser, BigDecimal averageWinner, BigDecimal averageLoser, BigDecimal winRate, BigDecimal profitFactor, TradeResponse bestTrade, TradeResponse worstTrade, Map<String, Long> mistakeSummary, List<SessionMistakeResponse> detectedMistakes, SessionReplayInsightsResponse replayInsights, String sessionStory, String sessionGrade, String bestTradeWhy, String worstTradeWhy, Integer disciplineScore, LocalDate previousTradingDay, LocalDate nextTradingDay) {
    }

    @java.lang.SuppressWarnings("all")
    public SessionReviewService(final TradingAccountService tradingAccountService, final TradeService tradeService, final TradeRepository tradeRepository, final TradeRuleViolationRepository violationRepository, final AiSessionReviewRepository aiSessionReviewRepository, final AiSessionReviewGeneratorService generatorService, final UsageLimitService usageLimitService, final FeatureAccessService featureAccessService, final ObjectMapper objectMapper, final ObservabilityService observabilityService, final AggregateCacheService aggregateCacheService, final PerformanceCacheInvalidationService cacheInvalidationService) {
        this.tradingAccountService = tradingAccountService;
        this.tradeService = tradeService;
        this.tradeRepository = tradeRepository;
        this.violationRepository = violationRepository;
        this.aiSessionReviewRepository = aiSessionReviewRepository;
        this.generatorService = generatorService;
        this.usageLimitService = usageLimitService;
        this.featureAccessService = featureAccessService;
        this.objectMapper = objectMapper;
        this.observabilityService = observabilityService;
        this.aggregateCacheService = aggregateCacheService;
        this.cacheInvalidationService = cacheInvalidationService;
    }
}
