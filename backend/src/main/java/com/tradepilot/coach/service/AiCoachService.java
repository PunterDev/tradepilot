package com.tradepilot.coach.service;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.tradepilot.billing.Feature;
import com.tradepilot.billing.FeatureAccessService;
import com.tradepilot.billing.UsageLimitService;
import com.tradepilot.billing.UsageType;
import com.tradepilot.coach.dto.CoachGenerateRequest;
import com.tradepilot.coach.dto.CoachDeterministicSummary;
import com.tradepilot.coach.dto.CoachSnapshotDto;
import com.tradepilot.coach.entity.AiCoachSnapshot;
import com.tradepilot.coach.entity.CoachSnapshotStatus;
import com.tradepilot.coach.entity.CoachSnapshotType;
import com.tradepilot.coach.repository.AiCoachSnapshotRepository;
import com.tradepilot.entity.User;
import com.tradepilot.exception.ResourceNotFoundException;
import com.tradepilot.leakdetector.dto.LeakResultDto;
import com.tradepilot.leakdetector.dto.LeakType;
import com.tradepilot.leakdetector.repository.AiLeakDetectorSnapshotRepository;
import com.tradepilot.observability.ObservabilityService;
import com.tradepilot.service.TradingAccountService;
import org.springframework.stereotype.Service;
import java.text.DecimalFormat;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;

@Service
public class AiCoachService {
    @java.lang.SuppressWarnings("all")
    private static final org.slf4j.Logger log = org.slf4j.LoggerFactory.getLogger(AiCoachService.class);
    private static final DecimalFormat MONEY_FORMAT = new DecimalFormat("#,##0.00");
    private final AiCoachSnapshotRepository repository;
    private final CoachSnapshotMapper mapper;
    private final AiCoachAsyncWorker asyncWorker;
    private final TradingAccountService tradingAccountService;
    private final UsageLimitService usageLimitService;
    private final FeatureAccessService featureAccessService;
    private final ObservabilityService observabilityService;
    private final AiLeakDetectorSnapshotRepository leakDetectorSnapshotRepository;
    private final ObjectMapper objectMapper;
    private final CoachAnalyticsAggregator analyticsAggregator;
    private final CoachHashService hashService;

    public CoachSnapshotDto getDailyCoach(User user, Long accountId, LocalDate date) {
        Long userId = requireUserId(user);
        featureAccessService.requireFeature(user, Feature.DAILY_COACH);
        validateOwnedAccount(user, accountId);
        AiCoachSnapshot snapshot = findOrCreateCachedSnapshot(user, userId, accountId, CoachSnapshotType.DAILY, date, date, false);
        return enrichWithTopLeak(mapper.toDto(snapshot), userId, accountId);
    }

    public CoachSnapshotDto getWeeklyCoach(User user, Long accountId, LocalDate weekStart) {
        Long userId = requireUserId(user);
        featureAccessService.requireFeature(user, Feature.WEEKLY_COACH);
        validateOwnedAccount(user, accountId);
        LocalDate weekEnd = weekStart.plusDays(6);
        AiCoachSnapshot snapshot = findOrCreateCachedSnapshot(user, userId, accountId, CoachSnapshotType.WEEKLY, weekStart, weekEnd, false);
        return mapper.toDto(snapshot);
    }

    public CoachSnapshotDto generateDailyCoach(User user, CoachGenerateRequest request) {
        Long userId = requireUserId(user);
        featureAccessService.requireFeature(user, Feature.DAILY_COACH);
        validateOwnedAccount(user, request.getAccountId());
        LocalDate date = request.getPeriodStart();
        AiCoachSnapshot snapshot = findOrCreateCachedSnapshot(user, userId, request.getAccountId(), CoachSnapshotType.DAILY, date, date, Boolean.TRUE.equals(request.getForceRefresh()));
        return enrichWithTopLeak(mapper.toDto(snapshot), userId, request.getAccountId());
    }

    public CoachSnapshotDto generateWeeklyCoach(User user, CoachGenerateRequest request) {
        Long userId = requireUserId(user);
        featureAccessService.requireFeature(user, Feature.WEEKLY_COACH);
        validateOwnedAccount(user, request.getAccountId());
        LocalDate weekStart = request.getPeriodStart();
        LocalDate weekEnd = request.getPeriodEnd() != null ? request.getPeriodEnd() : weekStart.plusDays(6);
        AiCoachSnapshot snapshot = findOrCreateCachedSnapshot(user, userId, request.getAccountId(), CoachSnapshotType.WEEKLY, weekStart, weekEnd, Boolean.TRUE.equals(request.getForceRefresh()));
        return mapper.toDto(snapshot);
    }

    public List<CoachSnapshotDto> getSnapshots(User user, Long accountId, CoachSnapshotType type) {
        Long userId = requireUserId(user);
        featureAccessService.requireFeature(user, type == CoachSnapshotType.DAILY ? Feature.DAILY_COACH : Feature.WEEKLY_COACH);
        validateOwnedAccount(user, accountId);
        return repository.findTop20ByUserIdAndAccountIdAndSnapshotTypeOrderByPeriodStartDesc(userId, accountId, type).stream().map(mapper::toDto).toList();
    }

    private AiCoachSnapshot findOrCreateCachedSnapshot(User user, Long userId, Long accountId, CoachSnapshotType type, LocalDate periodStart, LocalDate periodEnd, boolean forceRefresh) {
        Long normalizedAccountId = accountId == null ? 0L : accountId;
        AiCoachSnapshot existing = repository.findTopByUserIdAndAccountIdAndSnapshotTypeAndPeriodStartAndPeriodEndOrderByCreatedAtDesc(userId, normalizedAccountId, type, periodStart, periodEnd).orElse(null);
        if (!forceRefresh) {
            if (existing != null) {
                if (type == CoachSnapshotType.DAILY && existing.getStatus() == CoachSnapshotStatus.COMPLETED && isCurrentSnapshot(existing)) {
                    observabilityService.recordCacheEvent("Daily Coach", true);
                    return existing;
                }
                if (existing.getStatus() == CoachSnapshotStatus.PROCESSING || existing.getStatus() == CoachSnapshotStatus.PENDING) {
                    observabilityService.recordCacheEvent(type == CoachSnapshotType.DAILY ? "Daily Coach" : "Weekly Coach", true);
                    return existing;
                }
                if (existing.getStatus() == CoachSnapshotStatus.COMPLETED && isCurrentSnapshot(existing)) {
                    observabilityService.recordCacheEvent(type == CoachSnapshotType.DAILY ? "Daily Coach" : "Weekly Coach", true);
                    return existing;
                }
            }
        }
        CoachDeterministicSummary summary = analyticsAggregator.aggregate(userId, normalizedAccountId, type, periodStart, periodEnd);
        if (summary.getTotalTrades() != null && summary.getTotalTrades() == 0) {
            observabilityService.recordCacheEvent(type == CoachSnapshotType.DAILY ? "Daily Coach" : "Weekly Coach", existing != null && !forceRefresh);
            if (type == CoachSnapshotType.DAILY) {
                log.info("Daily Coach AI skipped: no trades for selected date. userId={}, accountId={}, reviewDate={}", userId, normalizedAccountId, periodStart);
            } else {
                log.info("Weekly Coach AI skipped: no trades for selected week. userId={}, accountId={}, weekStart={}, weekEnd={}", userId, normalizedAccountId, periodStart, periodEnd);
            }
            return saveDeterministicSnapshot(existing, userId, normalizedAccountId, type, periodStart, periodEnd, summary);
        }
        observabilityService.recordCacheEvent(type == CoachSnapshotType.DAILY ? "Daily Coach" : "Weekly Coach", false);
        UsageType usageType = type == CoachSnapshotType.DAILY ? UsageType.DAILY_COACH : UsageType.WEEKLY_COACH;
        usageLimitService.enforceAiUsage(user, usageType);
        if (existing != null) {
            existing.setStatus(CoachSnapshotStatus.PENDING);
            existing.setErrorMessage(null);
            AiCoachSnapshot saved = repository.save(existing);
            usageLimitService.recordAiUsage(user, usageType, saved.getId());
            asyncWorker.generateAsync(saved.getId());
            return saved;
        }
        AiCoachSnapshot snapshot = AiCoachSnapshot.builder().userId(userId).accountId(normalizedAccountId).snapshotType(type).periodStart(periodStart).periodEnd(periodEnd).status(CoachSnapshotStatus.PENDING).build();
        AiCoachSnapshot saved = repository.save(snapshot);
        usageLimitService.recordAiUsage(user, usageType, saved.getId());
        asyncWorker.generateAsync(saved.getId());
        return saved;
    }

    private AiCoachSnapshot saveDeterministicSnapshot(AiCoachSnapshot existing, Long userId, Long accountId, CoachSnapshotType type, LocalDate periodStart, LocalDate periodEnd, CoachDeterministicSummary summary) {
        AiCoachSnapshot snapshot = existing != null ? existing : new AiCoachSnapshot();
        snapshot.setUserId(userId);
        snapshot.setAccountId(accountId);
        snapshot.setSnapshotType(type);
        snapshot.setPeriodStart(periodStart);
        snapshot.setPeriodEnd(periodEnd);
        snapshot.setStatus(CoachSnapshotStatus.COMPLETED);
        snapshot.setErrorMessage(null);
        applySummary(snapshot, summary);
        snapshot.setAiResponseJson(null);
        snapshot.setDeterministicSummaryJson(hashService.toJson(summary));
        snapshot.setReviewHash(hashService.hash(summary));
        return repository.save(snapshot);
    }

    private void applySummary(AiCoachSnapshot snapshot, CoachDeterministicSummary summary) {
        snapshot.setTotalTrades(summary.getTotalTrades());
        snapshot.setNetPnl(summary.getNetPnl());
        snapshot.setWinRate(summary.getWinRate());
        snapshot.setProfitFactor(summary.getProfitFactor());
        snapshot.setBestSetup(summary.getBestSetup());
        snapshot.setWorstSetup(summary.getWorstSetup());
        snapshot.setBestSession(summary.getBestSession());
        snapshot.setWorstSession(summary.getWorstSession());
        snapshot.setBiggestMistake(summary.getBiggestMistake());
    }

    private Long requireUserId(User user) {
        if (user == null || user.getId() == null) {
            throw new ResourceNotFoundException("User not found");
        }
        return user.getId();
    }

    private CoachSnapshotDto enrichWithTopLeak(CoachSnapshotDto dto, Long userId, Long accountId) {
        topLeak(userId, accountId).ifPresent(leak -> {
            dto.setTopLeakToWatch(leak);
            boolean appliesToday = historicalPatternAppliesToPeriod(dto, leak);
            dto.setHistoricalPatternAppliesToday(appliesToday);
            dto.setHistoricalPatternToWatch(historicalPatternMessage(leak, appliesToday));
        });
        return dto;
    }

    private boolean isCurrentSnapshot(AiCoachSnapshot snapshot) {
        try {
            CoachDeterministicSummary summary = analyticsAggregator.aggregate(snapshot.getUserId(), snapshot.getAccountId(), snapshot.getSnapshotType(), snapshot.getPeriodStart(), snapshot.getPeriodEnd());
            return hashService.hash(summary).equals(snapshot.getReviewHash());
        } catch (Exception ignored) {
            return false;
        }
    }

    private boolean historicalPatternAppliesToPeriod(CoachSnapshotDto dto, LeakResultDto leak) {
        if (dto.getSnapshotType() != CoachSnapshotType.DAILY || leak.getLeakType() == null) {
            return true;
        }
        if (leak.getLeakType() == LeakType.LONG_SHORT_BIAS) {
            String title = leak.getTitle() == null ? "" : leak.getTitle().toUpperCase();
            if (title.contains("SHORT")) {
                return dto.getShortTradeCount() != null && dto.getShortTradeCount() > 0;
            }
            if (title.contains("LONG")) {
                return dto.getLongTradeCount() != null && dto.getLongTradeCount() > 0;
            }
        }
        return true;
    }

    private String historicalPatternMessage(LeakResultDto leak, boolean appliesToday) {
        String summary = leak.getSummary() == null || leak.getSummary().isBlank() ? leak.getTitle() : leak.getSummary();
        String pattern = accountLevelPatternSummary(leak, summary);
        if (appliesToday) {
            return "Account-level pattern: " + pattern;
        }
        return "Account-level pattern: " + pattern + " This did not apply today.";
    }

    private String accountLevelPatternSummary(LeakResultDto leak, String fallback) {
        if (leak.getLeakType() == LeakType.LONG_SHORT_BIAS && leak.getTitle() != null) {
            String title = leak.getTitle().toUpperCase();
            if (title.contains("SHORT")) {
                return "Recent account history shows short-side trades have underperformed by " + money(leak.getTotalPnl()) + " across " + countLabel(leak.getTradeCount()) + ".";
            }
            if (title.contains("LONG")) {
                return "Recent account history shows long-side trades have underperformed by " + money(leak.getTotalPnl()) + " across " + countLabel(leak.getTradeCount()) + ".";
            }
        }
        return fallback;
    }

    private String money(BigDecimal value) {
        BigDecimal amount = value == null ? BigDecimal.ZERO : value.setScale(2, RoundingMode.HALF_UP);
        return (amount.compareTo(BigDecimal.ZERO) < 0 ? "-$" : "$") + MONEY_FORMAT.format(amount.abs());
    }

    private String countLabel(Integer tradeCount) {
        int count = tradeCount == null ? 0 : tradeCount;
        return count + (count == 1 ? " trade" : " trades");
    }

    private Optional<LeakResultDto> topLeak(Long userId, Long accountId) {
        return leakDetectorSnapshotRepository.findTopByUserIdAndAccountIdOrderByUpdatedAtDesc(userId, accountId).flatMap(snapshot -> {
            try {
                List<LeakResultDto> leaks = objectMapper.readValue(snapshot.getResultJson(), new TypeReference<>() {
                });
                return leaks.stream().max(Comparator.comparing((LeakResultDto leak) -> leak.getSeverity() == null ? 0 : leak.getSeverity().ordinal()).thenComparing(leak -> leak.getConfidenceScore() == null ? 0 : leak.getConfidenceScore()));
            } catch (Exception e) {
                return Optional.empty();
            }
        });
    }

    private void validateOwnedAccount(User user, Long accountId) {
        if (accountId == null || accountId <= 0) {
            throw new IllegalArgumentException("accountId is required");
        }
        tradingAccountService.getAccountForImport(user, accountId);
    }

    @java.lang.SuppressWarnings("all")
    public AiCoachService(final AiCoachSnapshotRepository repository, final CoachSnapshotMapper mapper, final AiCoachAsyncWorker asyncWorker, final TradingAccountService tradingAccountService, final UsageLimitService usageLimitService, final FeatureAccessService featureAccessService, final ObservabilityService observabilityService, final AiLeakDetectorSnapshotRepository leakDetectorSnapshotRepository, final ObjectMapper objectMapper, final CoachAnalyticsAggregator analyticsAggregator, final CoachHashService hashService) {
        this.repository = repository;
        this.mapper = mapper;
        this.asyncWorker = asyncWorker;
        this.tradingAccountService = tradingAccountService;
        this.usageLimitService = usageLimitService;
        this.featureAccessService = featureAccessService;
        this.observabilityService = observabilityService;
        this.leakDetectorSnapshotRepository = leakDetectorSnapshotRepository;
        this.objectMapper = objectMapper;
        this.analyticsAggregator = analyticsAggregator;
        this.hashService = hashService;
    }
}
