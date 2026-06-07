package com.tradepilot.analytics;

import com.tradepilot.analytics.dto.*;
import com.tradepilot.entity.User;
import com.tradepilot.enums.OutcomeType;
import com.tradepilot.enums.SessionType;
import com.tradepilot.observability.ObservabilityService;
import com.tradepilot.repository.TradeRepository;
import com.tradepilot.service.AggregateCacheService;
import com.tradepilot.service.TradingAccountService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.*;

@Service
@RequiredArgsConstructor
@Slf4j
public class TradeAnalyticsService {
    private static final Duration ANALYTICS_TTL = Duration.ofSeconds(45);

    private final TradeRepository tradeRepository;
    private final TradingAccountService tradingAccountService;
    private final ObservabilityService observabilityService;
    private final AggregateCacheService aggregateCacheService;

    @Transactional(readOnly = true)
    public FridayOpexAnalyticsResponse getFridayOpexAnalytics(User user, Long accountId) {
        validate(user, accountId);
        return aggregateCacheService.get(analyticsCacheKey("friday-opex", user, accountId), ANALYTICS_TTL, () -> buildFridayOpexAnalytics(user, accountId));
    }

    private FridayOpexAnalyticsResponse buildFridayOpexAnalytics(User user, Long accountId) {
        Map<String, AnalyticsMetricDto> buckets = new HashMap<>();
        observabilityService.timeQuery("friday_opex_summary", () -> tradeRepository.aggregateFridayOpex(user.getId(), accountId)).forEach(row ->
                buckets.put(String.valueOf(row[0]), mapAnalyticsMetric(row, 1))
        );

        AnalyticsMetricDto opex = buckets.getOrDefault("OPEX", emptyMetric());
        AnalyticsMetricDto nonOpex = buckets.getOrDefault("NON_OPEX", emptyMetric());
        AnalyticsMetricDto allFridays = combine(opex, nonOpex);

        return FridayOpexAnalyticsResponse.of(
                allFridays,
                opex,
                nonOpex,
                buildFridayOpexInsight(opex, nonOpex)
        );
    }

    @Transactional(readOnly = true)
    public List<SessionPerformanceDto> getSessionPerformance(User user, Long accountId) {
        validate(user, accountId);
        return observabilityService.timeQuery("session_performance_summary", () -> tradeRepository.aggregateBySession(user.getId(), accountId, null, null))
                .stream()
                .map(row -> {
                    AnalyticsMetricDto metrics = mapStatsMetric(row);
                    return SessionPerformanceDto.of(
                            String.valueOf(row[0]),
                            metrics.getTradeCount(),
                            metrics.getTotalPnl(),
                            metrics.getWinRate(),
                            metrics.getAvgPnl()
                    );
                })
                .toList();
    }

    @Transactional(readOnly = true)
    public List<HeatmapCellDto> getHeatmap(User user, Long accountId) {
        validate(user, accountId);
        return aggregateCacheService.get(analyticsCacheKey("heatmap", user, accountId), ANALYTICS_TTL, () -> buildHeatmap(user, accountId));
    }

    private List<HeatmapCellDto> buildHeatmap(User user, Long accountId) {
        List<TradeRepository.HeatmapTradeProjection> trades = observabilityService.timeQuery(
                "heatmap_summary",
                () -> tradeRepository.findHeatmapTrades(user.getId(), accountId)
        );
        Map<String, HeatmapAccumulator> grouped = new LinkedHashMap<>();
        long missingTimeCount = 0;

        for (TradeRepository.HeatmapTradeProjection trade : trades) {
            if (trade.getTradeDate() == null) {
                continue;
            }
            if (trade.getEntryTime() == null) {
                missingTimeCount++;
            }

            String weekday = weekdayName(String.valueOf(trade.getTradeDate().getDayOfWeek().getValue()));
            String bucket = deriveSessionBucket(trade.getEntryTime() == null ? null : trade.getEntryTime().toLocalTime(), trade.getSession());
            grouped.computeIfAbsent(weekday + "::" + bucket, ignored -> new HeatmapAccumulator(weekday, bucket))
                    .add(trade);
        }

        List<HeatmapCellDto> cells = grouped.values().stream()
                .map(HeatmapAccumulator::toDto)
                .sorted(Comparator
                        .comparing((HeatmapCellDto cell) -> daySort(cell.getDayOfWeek()))
                        .thenComparing(cell -> sessionSort(cell.getSessionBucket())))
                .toList();
        log.debug("Heatmap summary built. userId={}, accountId={}, cellCount={}, missingTimeTrades={}",
                user.getId(), accountId, cells.size(), missingTimeCount);
        return cells;
    }

    @Transactional(readOnly = true)
    public List<CalendarSummaryDto> getCalendarSummary(User user, Long accountId) {
        validate(user, accountId);
        return observabilityService.timeQuery("calendar_summary", () -> tradeRepository.aggregateDailyPnl(user.getId(), accountId, null, null))
                .stream()
                .map(row -> CalendarSummaryDto.of(
                        localDateValue(row[0]),
                        longValue(row[1]),
                        decimalValue(row[2]),
                        longValue(row[3]),
                        longValue(row[4]),
                        longValue(row[5])
                ))
                .toList();
    }

    @Transactional(readOnly = true)
    public List<BehavioralInsightDto> getBehavioralInsights(User user, Long accountId) {
        validate(user, accountId);
        return aggregateCacheService.get(analyticsCacheKey("behavioral-insights", user, accountId), ANALYTICS_TTL, () -> buildBehavioralInsights(user, accountId));
    }

    private List<BehavioralInsightDto> buildBehavioralInsights(User user, Long accountId) {
        List<BehavioralInsightDto> insights = new ArrayList<>();

        FridayOpexAnalyticsResponse friday = getFridayOpexAnalytics(user, accountId);
        List<SessionPerformanceDto> sessions = getSessionPerformance(user, accountId);

        if (friday.getOpexFridays().getTotalPnl().compareTo(BigDecimal.ZERO) < 0) {
            insights.add(BehavioralInsightDto.of(
                    "FRIDAY_OPEX",
                    "HIGH",
                    "OPEX Fridays are underperforming",
                    "Your OPEX Friday performance is negative. Consider reducing size or skipping trades on third Fridays."
            ));
        }

        addSessionInsights(insights, sessions);

        return insights;
    }

    private void addSessionInsights(List<BehavioralInsightDto> insights, List<SessionPerformanceDto> sessions) {
        if (sessions == null || sessions.isEmpty()) {
            return;
        }

        if (sessions.size() == 1) {
            SessionPerformanceDto primary = sessions.get(0);
            insights.add(BehavioralInsightDto.of(
                    "SESSION_PRIMARY",
                    "LOW",
                    "Primary trading session detected",
                    "Most of your trading activity is during " + primary.getSession()
                            + " with total PnL " + formatCurrency(primary.getTotalPnl())
                            + " and " + primary.getTradeCount() + " trades."
            ));
            return;
        }

        SessionPerformanceDto best = sessions.stream()
                .max(Comparator.comparing(SessionPerformanceDto::getTotalPnl))
                .orElse(null);
        SessionPerformanceDto worst = sessions.stream()
                .min(Comparator.comparing(SessionPerformanceDto::getTotalPnl))
                .orElse(null);

        if (best == null || worst == null) {
            return;
        }

        insights.add(BehavioralInsightDto.of(
                "SESSION_BEST",
                "LOW",
                "Best trading session detected",
                "Your strongest session is " + best.getSession()
                        + " with total PnL " + formatCurrency(best.getTotalPnl()) + "."
        ));

        if (Objects.equals(best.getSession(), worst.getSession())) {
            return;
        }

        if (worst.getTotalPnl().compareTo(BigDecimal.ZERO) < 0) {
            insights.add(BehavioralInsightDto.of(
                    "SESSION_WEAKEST",
                    "MEDIUM",
                    "Weakest trading session detected",
                    "Your weakest session is " + worst.getSession()
                            + " with total PnL " + formatCurrency(worst.getTotalPnl()) + "."
            ));
            return;
        }

        insights.add(BehavioralInsightDto.of(
                "SESSION_LOWEST_PROFITABLE",
                "LOW",
                "Lowest-performing profitable session",
                "Your lowest-performing profitable session is " + worst.getSession()
                        + " with total PnL " + formatCurrency(worst.getTotalPnl()) + "."
        ));
    }

    private String formatCurrency(BigDecimal amount) {
        BigDecimal safeAmount = amount == null ? BigDecimal.ZERO : amount.setScale(2, RoundingMode.HALF_UP);
        String prefix = safeAmount.compareTo(BigDecimal.ZERO) < 0 ? "-$" : "$";
        return prefix + safeAmount.abs().toPlainString();
    }

    private String analyticsCacheKey(String report, User user, Long accountId) {
        return "analytics:" + user.getId() + ":" + report + ":" + accountId;
    }

    private void validate(User user, Long accountId) {
        if (user == null) {
            throw new IllegalArgumentException("Authenticated user is required");
        }

        if (accountId != null) {
            tradingAccountService.getAccountForImport(user, accountId);
        }
    }

    private AnalyticsMetricDto mapStatsMetric(Object[] row) {
        long trades = longValue(row[1]);
        BigDecimal totalPnl = decimalValue(row[5]);
        return AnalyticsMetricDto.of(
                Math.toIntExact(trades),
                totalPnl,
                decimalValue(row[7]),
                trades == 0 ? BigDecimal.ZERO : totalPnl.divide(BigDecimal.valueOf(trades), 2, RoundingMode.HALF_UP),
                BigDecimal.ZERO,
                BigDecimal.ZERO
        );
    }

    private AnalyticsMetricDto mapAnalyticsMetric(Object[] row, int offset) {
        return AnalyticsMetricDto.of(
                intValue(row[offset]),
                decimalValue(row[offset + 1]),
                decimalValue(row[offset + 2]),
                decimalValue(row[offset + 3]),
                decimalValue(row[offset + 4]),
                decimalValue(row[offset + 5])
        );
    }

    private AnalyticsMetricDto emptyMetric() {
        return AnalyticsMetricDto.of(0, BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO);
    }

    private AnalyticsMetricDto combine(AnalyticsMetricDto left, AnalyticsMetricDto right) {
        int tradeCount = left.getTradeCount() + right.getTradeCount();
        BigDecimal totalPnl = left.getTotalPnl().add(right.getTotalPnl());
        BigDecimal weightedWins = left.getWinRate().multiply(BigDecimal.valueOf(left.getTradeCount()))
                .add(right.getWinRate().multiply(BigDecimal.valueOf(right.getTradeCount())));
        return AnalyticsMetricDto.of(
                tradeCount,
                totalPnl,
                tradeCount == 0 ? BigDecimal.ZERO : weightedWins.divide(BigDecimal.valueOf(tradeCount), 2, RoundingMode.HALF_UP),
                tradeCount == 0 ? BigDecimal.ZERO : totalPnl.divide(BigDecimal.valueOf(tradeCount), 2, RoundingMode.HALF_UP),
                BigDecimal.ZERO,
                BigDecimal.ZERO
        );
    }

    private String buildFridayOpexInsight(AnalyticsMetricDto opex, AnalyticsMetricDto nonOpex) {
        if (opex.getTradeCount() == 0) {
            return "No OPEX Friday trades found yet.";
        }

        if (opex.getTotalPnl().compareTo(nonOpex.getTotalPnl()) < 0) {
            return "OPEX Fridays are underperforming compared to normal Fridays.";
        }

        return "OPEX Fridays are not currently worse than normal Fridays.";
    }

    private String deriveSessionBucket(LocalTime entryTime, SessionType fallbackSession) {
        if (entryTime != null) {
            if (!entryTime.isBefore(LocalTime.of(9, 30)) && entryTime.isBefore(LocalTime.of(9, 45))) {
                return "NY_OPEN_FIRST_15";
            }
            if (!entryTime.isBefore(LocalTime.of(9, 45)) && entryTime.isBefore(LocalTime.of(11, 0))) {
                return "NY_AM";
            }
            if (!entryTime.isBefore(LocalTime.of(11, 0)) && entryTime.isBefore(LocalTime.of(12, 30))) {
                return "LATE_MORNING";
            }
            if (!entryTime.isBefore(LocalTime.of(12, 30)) && entryTime.isBefore(LocalTime.of(13, 30))) {
                return "LUNCH";
            }
            if (!entryTime.isBefore(LocalTime.of(13, 30)) && entryTime.isBefore(LocalTime.of(16, 0))) {
                return "PM_SESSION";
            }
            return "OTHER";
        }

        if (fallbackSession == SessionType.NY_AM) {
            return "NY_AM";
        }
        if (fallbackSession == SessionType.NY_PM) {
            return "PM_SESSION";
        }
        return "OTHER";
    }

    private int daySort(String day) {
        return switch (day) {
            case "MONDAY" -> 1;
            case "TUESDAY" -> 2;
            case "WEDNESDAY" -> 3;
            case "THURSDAY" -> 4;
            case "FRIDAY" -> 5;
            case "SATURDAY" -> 6;
            case "SUNDAY" -> 7;
            default -> 8;
        };
    }

    private int sessionSort(String session) {
        return switch (session) {
            case "NY_OPEN_FIRST_15" -> 1;
            case "NY_AM" -> 2;
            case "LATE_MORNING" -> 3;
            case "LUNCH" -> 4;
            case "PM_SESSION" -> 5;
            case "OTHER" -> 6;
            default -> 7;
        };
    }

    private int intValue(Object value) {
        return value instanceof Number number ? number.intValue() : 0;
    }

    private long longValue(Object value) {
        return value instanceof Number number ? number.longValue() : 0L;
    }

    private BigDecimal decimalValue(Object value) {
        if (value instanceof BigDecimal decimal) {
            return decimal.setScale(2, RoundingMode.HALF_UP);
        }
        if (value instanceof Number number) {
            return BigDecimal.valueOf(number.doubleValue()).setScale(2, RoundingMode.HALF_UP);
        }
        return BigDecimal.ZERO;
    }

    private String weekdayName(String value) {
        return switch (value) {
            case "1", "1.0" -> "MONDAY";
            case "2", "2.0" -> "TUESDAY";
            case "3", "3.0" -> "WEDNESDAY";
            case "4", "4.0" -> "THURSDAY";
            case "5", "5.0" -> "FRIDAY";
            case "6", "6.0" -> "SATURDAY";
            case "7", "7.0" -> "SUNDAY";
            default -> "UNKNOWN";
        };
    }

    private LocalDate localDateValue(Object value) {
        if (value instanceof LocalDate date) {
            return date;
        }
        if (value instanceof java.sql.Date date) {
            return date.toLocalDate();
        }
        return LocalDate.parse(String.valueOf(value));
    }

    private static class HeatmapAccumulator {
        private final String dayOfWeek;
        private final String sessionBucket;
        private int tradeCount;
        private int wins;
        private int losses;
        private BigDecimal totalPnl = BigDecimal.ZERO;

        private HeatmapAccumulator(String dayOfWeek, String sessionBucket) {
            this.dayOfWeek = dayOfWeek;
            this.sessionBucket = sessionBucket;
        }

        private void add(TradeRepository.HeatmapTradeProjection trade) {
            tradeCount++;
            if (trade.getOutcome() == OutcomeType.WIN) {
                wins++;
            } else if (trade.getOutcome() == OutcomeType.LOSS) {
                losses++;
            }
            if (trade.getPnlAmount() != null) {
                totalPnl = totalPnl.add(trade.getPnlAmount());
            }
        }

        private HeatmapCellDto toDto() {
            BigDecimal winRate = tradeCount == 0
                    ? BigDecimal.ZERO
                    : BigDecimal.valueOf(wins)
                    .multiply(BigDecimal.valueOf(100))
                    .divide(BigDecimal.valueOf(tradeCount), 2, RoundingMode.HALF_UP);
            return HeatmapCellDto.of(dayOfWeek, sessionBucket, tradeCount, wins, losses, totalPnl, winRate);
        }
    }
}
