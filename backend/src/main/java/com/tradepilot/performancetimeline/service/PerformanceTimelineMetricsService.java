package com.tradepilot.performancetimeline.service;

import com.tradepilot.entity.Trade;
import com.tradepilot.entity.TradingAccount;
import com.tradepilot.performancetimeline.dto.PerformanceTimelineMetrics;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Duration;
import java.time.LocalDate;
import java.time.temporal.WeekFields;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

@Service
public class PerformanceTimelineMetricsService {
    private static final BigDecimal SCRATCH_THRESHOLD_DOLLARS = new BigDecimal("5.00");
    private static final BigDecimal OVERSIZED_RISK_MULTIPLIER = new BigDecimal("1.50");

    public PerformanceTimelineMetrics calculate(List<Trade> trades, TradingAccount account) {
        PerformanceTimelineMetrics metrics = new PerformanceTimelineMetrics();
        metrics.setTotalTrades(trades.size());
        metrics.setNetPnl(round(sumPnl(trades), 2));
        List<BigDecimal> rMultiples = trades.stream().map(trade -> rMultiple(trade, account)).filter(Objects::nonNull).toList();
        metrics.setTotalR(rMultiples.isEmpty() ? null : round(sum(rMultiples), 2));
        metrics.setAverageR(rMultiples.isEmpty() ? null : average(rMultiples, 2));
        metrics.setWinRate(percent(count(trades, trade -> pnl(trade).compareTo(BigDecimal.ZERO) > 0), trades.size()));
        metrics.setProfitFactor(profitFactor(trades));
        List<BigDecimal> winners = rMultiples.stream().filter(value -> value.compareTo(BigDecimal.ZERO) > 0).toList();
        List<BigDecimal> losers = rMultiples.stream().filter(value -> value.compareTo(BigDecimal.ZERO) < 0).toList();
        metrics.setAverageWinnerR(winners.isEmpty() ? null : average(winners, 2));
        metrics.setAverageLoserR(losers.isEmpty() ? null : average(losers, 2));
        metrics.setLargestWinR(winners.stream().max(Comparator.naturalOrder()).map(value -> round(value, 2)).orElse(null));
        metrics.setLargestLossR(losers.stream().min(Comparator.naturalOrder()).map(value -> round(value, 2)).orElse(null));

        Map<LocalDate, List<Trade>> byDay = trades.stream().collect(Collectors.groupingBy(Trade::getTradeDate));
        List<BigDecimal> dailyPnl = byDay.values().stream().map(this::sumPnl).toList();
        metrics.setWinningDays((int) dailyPnl.stream().filter(value -> value.compareTo(BigDecimal.ZERO) > 0).count());
        metrics.setLosingDays((int) dailyPnl.stream().filter(value -> value.compareTo(BigDecimal.ZERO) < 0).count());
        metrics.setProfitableTradingDayPercent(percent(metrics.getWinningDays(), byDay.size()));
        metrics.setAverageTradesPerActiveDay(byDay.isEmpty() ? BigDecimal.ZERO : round(BigDecimal.valueOf(trades.size()).divide(BigDecimal.valueOf(byDay.size()), 4, RoundingMode.HALF_UP), 2));

        metrics.setLongTradeCount((int) trades.stream().filter(trade -> "LONG".equalsIgnoreCase(nullSafe(trade.getSide()))).count());
        metrics.setShortTradeCount((int) trades.stream().filter(trade -> "SHORT".equalsIgnoreCase(nullSafe(trade.getSide()))).count());
        metrics.setUnknownDirectionCount(trades.size() - metrics.getLongTradeCount() - metrics.getShortTradeCount());
        metrics.setSetupDistribution(distribution(trades.stream().map(trade -> trade.getSetup() == null ? "Unspecified" : trade.getSetup().name()).toList()));
        metrics.setBestPerformingSetup(bestGroup(trades, trade -> trade.getSetup() == null ? "Unspecified" : trade.getSetup().name(), true));
        metrics.setWorstPerformingSetup(bestGroup(trades, trade -> trade.getSetup() == null ? "Unspecified" : trade.getSetup().name(), false));
        metrics.setConfidenceDistribution(distribution(trades.stream().map(trade -> String.valueOf(confidence(trade))).toList()));
        metrics.setAverageHoldMinutes(averageHoldMinutes(trades));
        metrics.setScratchTradeCount((int) trades.stream().filter(trade -> pnl(trade).abs().compareTo(SCRATCH_THRESHOLD_DOLLARS) <= 0).count());

        double averageTradesPerDay = byDay.isEmpty() ? 0 : trades.size() / (double) byDay.size();
        int overtradingThreshold = Math.max(5, (int) Math.ceil(averageTradesPerDay * 1.5));
        metrics.setOvertradingDays((int) byDay.values().stream().filter(dayTrades -> dayTrades.size() > overtradingThreshold).count());
        metrics.setTradesAfterConsecutiveLosses(tradesAfterConsecutiveLosses(trades));
        metrics.setFomoTradeCount((int) trades.stream().filter(trade -> containsEmotion(trade, "FOMO")).count());
        metrics.setRevengeTradeCount((int) trades.stream().filter(trade -> containsEmotion(trade, "REVENGE")).count());

        List<BigDecimal> risks = trades.stream().map(Trade::getRiskAmount).filter(value -> value != null && value.compareTo(BigDecimal.ZERO) > 0).toList();
        metrics.setAverageRiskPerTrade(risks.isEmpty() ? null : average(risks, 2));
        metrics.setRiskConsistency(risks.size() < 2 ? null : coefficientOfVariation(risks));
        BigDecimal accountRisk = account == null ? null : account.getDefaultRiskAmount();
        metrics.setOversizedTradeCount(accountRisk == null || accountRisk.compareTo(BigDecimal.ZERO) <= 0 ? 0 : (int) trades.stream().filter(trade -> trade.getRiskAmount() != null && trade.getRiskAmount().compareTo(accountRisk.multiply(OVERSIZED_RISK_MULTIPLIER)) > 0).count());
        List<BigDecimal> dailyR = byDay.values().stream().map(dayTrades -> dayTrades.stream().map(trade -> rMultiple(trade, account)).filter(Objects::nonNull).reduce(BigDecimal.ZERO, BigDecimal::add)).toList();
        List<BigDecimal> losingDailyR = dailyR.stream().filter(value -> value.compareTo(BigDecimal.ZERO) < 0).toList();
        metrics.setLargestDailyLossR(losingDailyR.stream().min(Comparator.naturalOrder()).map(value -> round(value, 2)).orElse(null));
        metrics.setAverageDailyLossR(losingDailyR.isEmpty() ? null : average(losingDailyR, 2));
        metrics.setRiskRewardRatio(riskRewardRatio(winners, losers));
        metrics.setConfiguredRiskUsagePercent(accountRisk == null || accountRisk.compareTo(BigDecimal.ZERO) <= 0 ? null : percent((int) trades.stream().filter(trade -> effectiveRisk(trade, account) != null).count(), trades.size()));

        metrics.setEmotionDistribution(distribution(trades.stream().map(trade -> nullSafe(trade.getEmotion()).isBlank() ? "Unspecified" : trade.getEmotion().trim().toUpperCase(Locale.ROOT)).toList()));
        metrics.setMostCommonNegativeEmotion(mostCommon(metrics.getEmotionDistribution(), List.of("FEAR", "FOMO", "REVENGE", "FRUSTRATED", "OVERCONFIDENT", "TIRED")));
        metrics.setMostCommonPositiveEmotion(mostCommon(metrics.getEmotionDistribution(), List.of("CALM", "CONFIDENT", "PATIENT")));
        metrics.setEmotionalTradeCount((int) trades.stream().filter(trade -> isNegativeEmotion(trade.getEmotion())).count());
        metrics.setCalmTradeCount((int) trades.stream().filter(trade -> isPositiveEmotion(trade.getEmotion())).count());
        metrics.setNotesCompletionPercent(percent((int) trades.stream().filter(trade -> trade.getNotes() != null && !trade.getNotes().isBlank()).count(), trades.size()));
        metrics.setMetadataCompletionPercent(percent((int) trades.stream().filter(trade -> trade.getSetup() != null && trade.getEmotion() != null && confidence(trade) != null).count(), trades.size()));
        metrics.setHighConfidenceTradePercent(percent((int) trades.stream().filter(trade -> confidence(trade) != null && confidence(trade) >= 4).count(), trades.size()));

        Map<Integer, BigDecimal> weeklyR = trades.stream().collect(Collectors.groupingBy(trade -> trade.getTradeDate().get(WeekFields.ISO.weekOfWeekBasedYear()), Collectors.mapping(trade -> rMultiple(trade, account), Collectors.collectingAndThen(Collectors.toList(), values -> values.stream().filter(Objects::nonNull).reduce(BigDecimal.ZERO, BigDecimal::add)))));
        metrics.setGreenWeekCount((int) weeklyR.values().stream().filter(value -> value.compareTo(BigDecimal.ZERO) > 0).count());
        metrics.setRedWeekCount((int) weeklyR.values().stream().filter(value -> value.compareTo(BigDecimal.ZERO) < 0).count());
        metrics.setWeeklyRVolatility(weeklyR.size() < 2 ? null : standardDeviation(new ArrayList<>(weeklyR.values())));
        metrics.setDailyRVolatility(dailyR.size() < 2 ? null : standardDeviation(dailyR));
        metrics.setLongestProfitableDayStreak(streak(dailyPnl, true));
        metrics.setLongestLosingDayStreak(streak(dailyPnl, false));
        return metrics;
    }

    public BigDecimal rMultiple(Trade trade, TradingAccount account) {
        if (trade.getPnlR() != null) {
            return trade.getPnlR();
        }
        BigDecimal risk = effectiveRisk(trade, account);
        if (risk == null || risk.compareTo(BigDecimal.ZERO) <= 0) {
            return null;
        }
        return pnl(trade).divide(risk, 4, RoundingMode.HALF_UP);
    }

    private BigDecimal effectiveRisk(Trade trade, TradingAccount account) {
        if (trade.getRiskAmount() != null && trade.getRiskAmount().compareTo(BigDecimal.ZERO) > 0) {
            return trade.getRiskAmount();
        }
        if (account != null && account.getDefaultRiskAmount() != null && account.getDefaultRiskAmount().compareTo(BigDecimal.ZERO) > 0) {
            return account.getDefaultRiskAmount();
        }
        return null;
    }

    private BigDecimal pnl(Trade trade) {
        return trade.getPnlAmount() == null ? BigDecimal.ZERO : trade.getPnlAmount();
    }

    private BigDecimal sumPnl(Collection<Trade> trades) {
        return trades.stream().map(this::pnl).reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    private BigDecimal sum(Collection<BigDecimal> values) {
        return values.stream().reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    private BigDecimal average(Collection<BigDecimal> values, int scale) {
        return values.isEmpty() ? null : round(sum(values).divide(BigDecimal.valueOf(values.size()), 6, RoundingMode.HALF_UP), scale);
    }

    private BigDecimal percent(int numerator, int denominator) {
        if (denominator <= 0) return BigDecimal.ZERO;
        return round(BigDecimal.valueOf(numerator).multiply(BigDecimal.valueOf(100)).divide(BigDecimal.valueOf(denominator), 4, RoundingMode.HALF_UP), 2);
    }

    private int count(List<Trade> trades, java.util.function.Predicate<Trade> predicate) {
        return (int) trades.stream().filter(predicate).count();
    }

    private BigDecimal profitFactor(List<Trade> trades) {
        BigDecimal grossWins = trades.stream().map(this::pnl).filter(value -> value.compareTo(BigDecimal.ZERO) > 0).reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal grossLosses = trades.stream().map(this::pnl).filter(value -> value.compareTo(BigDecimal.ZERO) < 0).map(BigDecimal::abs).reduce(BigDecimal.ZERO, BigDecimal::add);
        if (grossLosses.compareTo(BigDecimal.ZERO) == 0) return grossWins.compareTo(BigDecimal.ZERO) > 0 ? new BigDecimal("99.00") : null;
        return round(grossWins.divide(grossLosses, 4, RoundingMode.HALF_UP), 2);
    }

    private BigDecimal riskRewardRatio(List<BigDecimal> winners, List<BigDecimal> losers) {
        if (winners.isEmpty() || losers.isEmpty()) return null;
        BigDecimal avgWinner = average(winners, 4);
        BigDecimal avgLoser = average(losers.stream().map(BigDecimal::abs).toList(), 4);
        if (avgLoser == null || avgLoser.compareTo(BigDecimal.ZERO) == 0) return null;
        return round(avgWinner.divide(avgLoser, 4, RoundingMode.HALF_UP), 2);
    }

    private Map<String, Integer> distribution(List<String> values) {
        Map<String, Integer> result = new LinkedHashMap<>();
        values.stream().filter(value -> value != null && !value.isBlank()).forEach(value -> result.merge(value, 1, Integer::sum));
        return result;
    }

    private String bestGroup(List<Trade> trades, java.util.function.Function<Trade, String> classifier, boolean best) {
        return trades.stream()
                .collect(Collectors.groupingBy(classifier, Collectors.reducing(BigDecimal.ZERO, this::pnl, BigDecimal::add)))
                .entrySet()
                .stream()
                .filter(entry -> entry.getKey() != null)
                .sorted(best ? Map.Entry.<String, BigDecimal>comparingByValue().reversed() : Map.Entry.comparingByValue())
                .map(Map.Entry::getKey)
                .findFirst()
                .orElse(null);
    }

    private BigDecimal averageHoldMinutes(List<Trade> trades) {
        List<BigDecimal> minutes = trades.stream()
                .filter(trade -> trade.getEntryTime() != null && trade.getExitTime() != null)
                .map(trade -> BigDecimal.valueOf(Duration.between(trade.getEntryTime(), trade.getExitTime()).toMinutes()))
                .filter(value -> value.compareTo(BigDecimal.ZERO) >= 0)
                .toList();
        return minutes.isEmpty() ? null : average(minutes, 2);
    }

    private int tradesAfterConsecutiveLosses(List<Trade> trades) {
        int losses = 0;
        int count = 0;
        for (Trade trade : trades.stream().sorted(Comparator.comparing(Trade::getTradeDate).thenComparing(trade -> trade.getEntryTime() == null ? trade.getTradeDate().atStartOfDay() : trade.getEntryTime()).thenComparing(Trade::getId, Comparator.nullsLast(Long::compareTo))).toList()) {
            if (losses >= 2) count++;
            losses = pnl(trade).compareTo(BigDecimal.ZERO) < 0 ? losses + 1 : 0;
        }
        return count;
    }

    private boolean containsEmotion(Trade trade, String token) {
        return trade.getEmotion() != null && trade.getEmotion().toUpperCase(Locale.ROOT).contains(token);
    }

    private Integer confidence(Trade trade) {
        return trade.getConfidence() != null ? trade.getConfidence() : trade.getConfidenceRating();
    }

    private boolean isNegativeEmotion(String emotion) {
        if (emotion == null) return false;
        String normalized = emotion.toUpperCase(Locale.ROOT);
        return normalized.contains("FEAR") || normalized.contains("FOMO") || normalized.contains("REVENGE") || normalized.contains("FRUSTRATED") || normalized.contains("OVERCONFIDENT") || normalized.contains("TIRED");
    }

    private boolean isPositiveEmotion(String emotion) {
        if (emotion == null) return false;
        String normalized = emotion.toUpperCase(Locale.ROOT);
        return normalized.contains("CALM") || normalized.contains("CONFIDENT") || normalized.contains("PATIENT");
    }

    private String mostCommon(Map<String, Integer> distribution, List<String> candidates) {
        Map<String, Integer> candidateCounts = new HashMap<>();
        for (String candidate : candidates) {
            candidateCounts.put(candidate, distribution.getOrDefault(candidate, 0));
        }
        return candidateCounts.entrySet().stream().filter(entry -> entry.getValue() > 0).max(Map.Entry.comparingByValue()).map(Map.Entry::getKey).orElse(null);
    }

    private BigDecimal coefficientOfVariation(List<BigDecimal> values) {
        BigDecimal avg = average(values, 8);
        if (avg == null || avg.compareTo(BigDecimal.ZERO) == 0) return null;
        return round(standardDeviation(values).divide(avg.abs(), 6, RoundingMode.HALF_UP), 2);
    }

    private BigDecimal standardDeviation(List<BigDecimal> values) {
        if (values.size() < 2) return BigDecimal.ZERO;
        double avg = values.stream().mapToDouble(BigDecimal::doubleValue).average().orElse(0);
        double variance = values.stream().mapToDouble(value -> Math.pow(value.doubleValue() - avg, 2)).average().orElse(0);
        return round(BigDecimal.valueOf(Math.sqrt(variance)), 2);
    }

    private int streak(List<BigDecimal> dailyValues, boolean profitable) {
        int best = 0;
        int current = 0;
        for (BigDecimal value : dailyValues) {
            boolean match = profitable ? value.compareTo(BigDecimal.ZERO) > 0 : value.compareTo(BigDecimal.ZERO) < 0;
            current = match ? current + 1 : 0;
            best = Math.max(best, current);
        }
        return best;
    }

    private String nullSafe(String value) {
        return value == null ? "" : value;
    }

    private BigDecimal round(BigDecimal value, int scale) {
        return value == null ? null : value.setScale(scale, RoundingMode.HALF_UP);
    }
}
