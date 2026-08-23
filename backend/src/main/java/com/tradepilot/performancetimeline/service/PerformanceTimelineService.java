package com.tradepilot.performancetimeline.service;

import com.fasterxml.jackson.databind.JavaType;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.tradepilot.billing.Feature;
import com.tradepilot.billing.FeatureAccessService;
import com.tradepilot.billing.UsageLimitService;
import com.tradepilot.billing.UsageType;
import com.tradepilot.entity.Trade;
import com.tradepilot.entity.TradingAccount;
import com.tradepilot.entity.User;
import com.tradepilot.performancetimeline.dto.PerformanceCategoryScores;
import com.tradepilot.performancetimeline.dto.PerformanceMetricComparison;
import com.tradepilot.performancetimeline.dto.PerformanceMilestoneResponse;
import com.tradepilot.performancetimeline.dto.PerformanceTimelineMetrics;
import com.tradepilot.performancetimeline.dto.PerformanceTimelineMonthResponse;
import com.tradepilot.performancetimeline.dto.PerformanceTimelineNarrativeResponse;
import com.tradepilot.performancetimeline.dto.PerformanceTimelineResponse;
import com.tradepilot.performancetimeline.entity.PerformanceTimelineSnapshot;
import com.tradepilot.performancetimeline.entity.PerformanceTrend;
import com.tradepilot.performancetimeline.entity.TimelineDataStatus;
import com.tradepilot.performancetimeline.repository.PerformanceTimelineSnapshotRepository;
import com.tradepilot.repository.TradeRepository;
import com.tradepilot.repository.TradingAccountRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.Month;
import java.time.YearMonth;
import java.time.format.TextStyle;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
public class PerformanceTimelineService {
    private static final int MONTH_LIMIT = 12;
    private static final int MIN_FULL_CHAPTER_TRADES = 5;
    private static final String PERIOD_TYPE = "MONTHLY";

    private final FeatureAccessService featureAccessService;
    private final UsageLimitService usageLimitService;
    private final TradingAccountRepository tradingAccountRepository;
    private final TradeRepository tradeRepository;
    private final PerformanceTimelineSnapshotRepository snapshotRepository;
    private final PerformanceTimelineMetricsService metricsService;
    private final PerformanceEvolutionScoreService scoreService;
    private final PerformanceTimelineComparisonService comparisonService;
    private final PerformanceTimelineNarrativeService narrativeService;
    private final PerformanceTimelineHashService hashService;
    private final PerformanceMilestoneService milestoneService;
    private final ObjectMapper objectMapper;

    public PerformanceTimelineService(FeatureAccessService featureAccessService, UsageLimitService usageLimitService, TradingAccountRepository tradingAccountRepository, TradeRepository tradeRepository, PerformanceTimelineSnapshotRepository snapshotRepository, PerformanceTimelineMetricsService metricsService, PerformanceEvolutionScoreService scoreService, PerformanceTimelineComparisonService comparisonService, PerformanceTimelineNarrativeService narrativeService, PerformanceTimelineHashService hashService, PerformanceMilestoneService milestoneService, ObjectMapper objectMapper) {
        this.featureAccessService = featureAccessService;
        this.usageLimitService = usageLimitService;
        this.tradingAccountRepository = tradingAccountRepository;
        this.tradeRepository = tradeRepository;
        this.snapshotRepository = snapshotRepository;
        this.metricsService = metricsService;
        this.scoreService = scoreService;
        this.comparisonService = comparisonService;
        this.narrativeService = narrativeService;
        this.hashService = hashService;
        this.milestoneService = milestoneService;
        this.objectMapper = objectMapper;
    }

    @Transactional
    public PerformanceTimelineResponse getTimeline(User user, Long accountId) {
        return load(user, accountId, false);
    }

    @Transactional
    public PerformanceTimelineResponse regenerate(User user, Long accountId) {
        return load(user, accountId, true);
    }

    private PerformanceTimelineResponse load(User user, Long accountId, boolean forceRefresh) {
        featureAccessService.requireFeature(user, Feature.PERFORMANCE_TIMELINE);
        TradingAccount account = tradingAccountRepository.findByIdAndUser_Id(accountId, user.getId()).orElseThrow(() -> new IllegalArgumentException("Trading account not found."));
        List<Trade> allTrades = tradeRepository.findByUserAndAccount_IdOrderByTradeDateAscEntryTimeAscIdAsc(user, accountId);
        List<YearMonth> periods = periodsToAnalyze(allTrades);
        Map<YearMonth, List<Trade>> tradesByMonth = allTrades.stream().collect(Collectors.groupingBy(trade -> YearMonth.from(trade.getTradeDate())));
        boolean hasEligibleMonth = periods.stream().anyMatch(period -> tradesByMonth.getOrDefault(period, List.of()).size() >= MIN_FULL_CHAPTER_TRADES);
        if (forceRefresh && hasEligibleMonth) {
            usageLimitService.enforceAiUsage(user, UsageType.PERFORMANCE_TIMELINE);
        }

        List<PerformanceTimelineMonthResponse> chronological = new ArrayList<>();
        boolean allCached = !forceRefresh;
        PerformanceTimelineMetrics previousEligibleMetrics = null;
        Integer previousEligibleScore = null;
        for (YearMonth period : periods) {
            List<Trade> monthTrades = tradesByMonth.getOrDefault(period, List.of()).stream().sorted(Comparator.comparing(Trade::getTradeDate).thenComparing(trade -> trade.getEntryTime() == null ? trade.getTradeDate().atStartOfDay() : trade.getEntryTime()).thenComparing(trade -> trade.getId() == null ? 0L : trade.getId())).toList();
            LocalDate start = period.atDay(1);
            LocalDate end = period.atEndOfMonth();
            String dataHash = hashService.hash(accountId, start, end, monthTrades, account);
            Optional<PerformanceTimelineSnapshot> cached = snapshotRepository.findByUserIdAndAccountIdAndPeriodTypeAndPeriodStart(user.getId(), accountId, PERIOD_TYPE, start);
            PerformanceTimelineMonthResponse month = null;
            if (!forceRefresh && cached.isPresent() && dataHash.equals(cached.get().getDataHash())) {
                month = fromSnapshot(cached.get());
            }
            if (month == null) {
                allCached = false;
                month = buildMonth(user, account, period, start, end, monthTrades, dataHash, previousEligibleMetrics, previousEligibleScore, forceRefresh && hasEligibleMonth);
                saveSnapshot(user, accountId, month);
            }
            if (month.getStatus() == TimelineDataStatus.READY) {
                previousEligibleMetrics = month.getMetrics();
                previousEligibleScore = month.getEvolutionScore();
            }
            chronological.add(month);
        }
        List<PerformanceTimelineMonthResponse> newestFirst = chronological.stream().sorted(Comparator.comparing(PerformanceTimelineMonthResponse::getPeriodStart).reversed()).toList();
        List<PerformanceMilestoneResponse> milestones = milestoneService.updateMilestones(user.getId(), accountId, newestFirst);
        if (forceRefresh && hasEligibleMonth) {
            usageLimitService.recordAiUsage(user, UsageType.PERFORMANCE_TIMELINE, null);
        }
        return summarize(accountId, newestFirst, milestones, allCached && !newestFirst.isEmpty(), forceRefresh && hasEligibleMonth ? 3 : 0);
    }

    private List<YearMonth> periodsToAnalyze(List<Trade> allTrades) {
        YearMonth latest = allTrades.stream().map(Trade::getTradeDate).filter(Objects::nonNull).max(LocalDate::compareTo).map(YearMonth::from).orElse(YearMonth.now());
        List<YearMonth> periods = new ArrayList<>();
        for (int i = MONTH_LIMIT - 1; i >= 0; i--) {
            periods.add(latest.minusMonths(i));
        }
        return periods;
    }

    private PerformanceTimelineMonthResponse buildMonth(User user, TradingAccount account, YearMonth period, LocalDate start, LocalDate end, List<Trade> trades, String dataHash, PerformanceTimelineMetrics previousEligibleMetrics, Integer previousEligibleScore, boolean allowAiNarrative) {
        TimelineDataStatus status = trades.isEmpty() ? TimelineDataStatus.NO_ACTIVITY : trades.size() < MIN_FULL_CHAPTER_TRADES ? TimelineDataStatus.LIMITED_DATA : TimelineDataStatus.READY;
        PerformanceTimelineMetrics metrics = metricsService.calculate(trades, account);
        PerformanceCategoryScores scores = null;
        Integer evolutionScore = null;
        if (status == TimelineDataStatus.READY) {
            scores = scoreService.categoryScores(metrics);
            evolutionScore = scoreService.evolutionScore(scores);
        }
        List<PerformanceMetricComparison> comparisons = status == TimelineDataStatus.READY ? comparisonService.compare(previousEligibleMetrics, metrics, previousEligibleScore, evolutionScore) : List.of();
        Integer scoreChange = previousEligibleScore == null || evolutionScore == null ? null : evolutionScore - previousEligibleScore;
        PerformanceTimelineNarrativeResponse narrative = narrativeService.generateNarrative(user.getId(), status, metrics, scoreChange, allowAiNarrative);

        PerformanceTimelineMonthResponse response = new PerformanceTimelineMonthResponse();
        response.setMonth(period.getMonth().getDisplayName(TextStyle.FULL, Locale.US));
        response.setYear(period.getYear());
        response.setPeriodStart(start);
        response.setPeriodEnd(end);
        response.setStatus(status);
        response.setTradeCount(trades.size());
        response.setMetrics(metrics);
        response.setComparison(comparisons);
        response.setCategoryScores(scores);
        response.setEvolutionScore(evolutionScore);
        response.setScoreChange(scoreChange);
        response.setScoreConfidence(scoreService.confidence(trades.size()));
        response.setNarrative(narrative);
        response.setMilestones(List.of());
        response.setDataHash(dataHash);
        response.setMessage(status == TimelineDataStatus.READY ? null : status == TimelineDataStatus.NO_ACTIVITY ? "No trading activity this month." : "Limited data: at least five trades are needed for a full chapter.");
        return response;
    }

    private void saveSnapshot(User user, Long accountId, PerformanceTimelineMonthResponse month) {
        PerformanceTimelineSnapshot snapshot = snapshotRepository.findByUserIdAndAccountIdAndPeriodTypeAndPeriodStart(user.getId(), accountId, PERIOD_TYPE, month.getPeriodStart()).orElseGet(PerformanceTimelineSnapshot::new);
        snapshot.setUserId(user.getId());
        snapshot.setAccountId(accountId);
        snapshot.setPeriodType(PERIOD_TYPE);
        snapshot.setPeriodStart(month.getPeriodStart());
        snapshot.setPeriodEnd(month.getPeriodEnd());
        snapshot.setDataStatus(month.getStatus());
        snapshot.setTradeCount(month.getTradeCount());
        snapshot.setMetricsJson(write(month.getMetrics()));
        snapshot.setComparisonJson(write(month.getComparison()));
        snapshot.setCategoryScoresJson(write(month.getCategoryScores()));
        snapshot.setEvolutionScore(month.getEvolutionScore());
        snapshot.setScoreConfidence(month.getScoreConfidence());
        snapshot.setNarrativeJson(write(month.getNarrative()));
        snapshot.setAiModel(month.getNarrative() != null && month.getNarrative().isAiGenerated() ? "openai" : "deterministic-v1");
        snapshot.setPromptVersion(hashService.promptVersion());
        snapshot.setDataHash(month.getDataHash());
        snapshot.setGeneratedAt(LocalDateTime.now());
        snapshotRepository.save(snapshot);
    }

    private PerformanceTimelineMonthResponse fromSnapshot(PerformanceTimelineSnapshot snapshot) {
        PerformanceTimelineMonthResponse response = new PerformanceTimelineMonthResponse();
        Month month = snapshot.getPeriodStart().getMonth();
        response.setMonth(month.getDisplayName(TextStyle.FULL, Locale.US));
        response.setYear(snapshot.getPeriodStart().getYear());
        response.setPeriodStart(snapshot.getPeriodStart());
        response.setPeriodEnd(snapshot.getPeriodEnd());
        response.setStatus(snapshot.getDataStatus());
        response.setTradeCount(snapshot.getTradeCount());
        response.setMetrics(read(snapshot.getMetricsJson(), PerformanceTimelineMetrics.class));
        response.setComparison(readComparisonList(snapshot.getComparisonJson()));
        response.setCategoryScores(read(snapshot.getCategoryScoresJson(), PerformanceCategoryScores.class));
        response.setEvolutionScore(snapshot.getEvolutionScore());
        response.setScoreConfidence(snapshot.getScoreConfidence());
        response.setNarrative(read(snapshot.getNarrativeJson(), PerformanceTimelineNarrativeResponse.class));
        response.setMilestones(List.of());
        response.setDataHash(snapshot.getDataHash());
        response.setMessage(snapshot.getDataStatus() == TimelineDataStatus.READY ? null : snapshot.getDataStatus() == TimelineDataStatus.NO_ACTIVITY ? "No trading activity this month." : "Limited data: at least five trades are needed for a full chapter.");
        return response;
    }

    private PerformanceTimelineResponse summarize(Long accountId, List<PerformanceTimelineMonthResponse> months, List<PerformanceMilestoneResponse> milestones, boolean cached, int creditCost) {
        List<PerformanceTimelineMonthResponse> ready = months.stream().filter(month -> month.getEvolutionScore() != null).toList();
        PerformanceTimelineMonthResponse latestReady = ready.isEmpty() ? null : ready.get(0);
        PerformanceTimelineMonthResponse previousReady = ready.size() > 1 ? ready.get(1) : null;
        PerformanceTimelineResponse response = new PerformanceTimelineResponse();
        response.setAccountId(accountId);
        response.setGeneratedAt(LocalDateTime.now());
        response.setFromDate(months.isEmpty() ? null : months.get(months.size() - 1).getPeriodStart());
        response.setToDate(months.isEmpty() ? null : months.get(0).getPeriodEnd());
        response.setCurrentEvolutionScore(latestReady == null ? null : latestReady.getEvolutionScore());
        response.setPreviousEvolutionScore(previousReady == null ? null : previousReady.getEvolutionScore());
        response.setScoreChange(latestReady == null ? null : latestReady.getScoreChange());
        response.setHighestEvolutionScore(ready.stream().map(PerformanceTimelineMonthResponse::getEvolutionScore).max(Integer::compareTo).orElse(null));
        response.setOverallTrend(trend(response.getScoreChange(), ready.size()));
        response.setStrongestCategory(latestReady == null ? null : category(latestReady.getCategoryScores(), true));
        response.setWeakestCategory(latestReady == null ? null : category(latestReady.getCategoryScores(), false));
        response.setLatestFocus(latestReady == null || latestReady.getNarrative() == null ? "Import more trades to build your performance journey." : latestReady.getNarrative().getNextFocus());
        response.setMonths(months);
        response.setMilestones(milestones);
        response.setCreditCost(creditCost);
        response.setCached(cached);
        return response;
    }

    private PerformanceTrend trend(Integer scoreChange, int readyMonths) {
        if (readyMonths < 2 || scoreChange == null) return PerformanceTrend.LIMITED_DATA;
        if (scoreChange >= 5) return PerformanceTrend.IMPROVING;
        if (scoreChange <= -5) return PerformanceTrend.DECLINING;
        return PerformanceTrend.STABLE;
    }

    private String category(PerformanceCategoryScores scores, boolean strongest) {
        if (scores == null) return null;
        Map<String, Integer> values = Map.of(
                "Discipline", scores.getDiscipline(),
                "Risk Management", scores.getRiskManagement(),
                "Execution", scores.getExecution(),
                "Consistency", scores.getConsistency(),
                "Psychology", scores.getPsychology()
        );
        return values.entrySet().stream().sorted(strongest ? Map.Entry.<String, Integer>comparingByValue().reversed() : Map.Entry.comparingByValue()).map(Map.Entry::getKey).findFirst().orElse(null);
    }

    private String write(Object value) {
        try {
            return objectMapper.writeValueAsString(value);
        } catch (Exception e) {
            throw new IllegalStateException("Unable to serialize performance timeline snapshot", e);
        }
    }

    private <T> T read(String json, Class<T> type) {
        try {
            return json == null ? null : objectMapper.readValue(json, type);
        } catch (Exception e) {
            return null;
        }
    }

    private List<PerformanceMetricComparison> readComparisonList(String json) {
        try {
            JavaType type = objectMapper.getTypeFactory().constructCollectionType(List.class, PerformanceMetricComparison.class);
            return objectMapper.readValue(json == null ? "[]" : json, type);
        } catch (Exception e) {
            return List.of();
        }
    }
}
