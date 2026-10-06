package com.tradepilot.leakdetector.service;

import com.tradepilot.entity.Trade;
import com.tradepilot.leakdetector.dto.BehavioralBaselineDto;
import com.tradepilot.leakdetector.dto.BehavioralCounterfactualDto;
import com.tradepilot.leakdetector.dto.BehavioralFindingDto;
import com.tradepilot.leakdetector.dto.EvidenceStrength;
import com.tradepilot.leakdetector.dto.LeakType;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Pure deterministic behavioral analysis. A baseline excludes the triggered cohort so a finding is always
 * compared with the trader's other comparable activity, never with itself.
 */
public final class BehavioralFindingAnalyzer {
    public static final String ENGINE_VERSION = "insight-trust-v1";
    static final int LOSS_STREAK_THRESHOLD = 2;
    static final Duration POST_LOSS_PAUSE = Duration.ofMinutes(20);
    static final BigDecimal MIN_EXPECTANCY_DETERIORATION_R = new BigDecimal("0.25");
    static final BigDecimal MIN_AVERAGE_PNL_DETERIORATION = new BigDecimal("50.00");
    static final int OVERTRADING_MARGIN = 1;
    static final int MIN_OVERTRADING_REFERENCE_SESSIONS = 5;

    private BehavioralFindingAnalyzer() {}

    public static List<Result> analyze(List<Trade> input) {
        List<Trade> trades = input.stream().filter(Objects::nonNull).sorted(order()).toList();
        return List.of(postLoss(trades), sizeEscalation(trades), overtrading(trades));
    }

    public record Result(LeakType type, String title, String recommendation, BehavioralFindingDto finding, List<Trade> relatedTrades) {}

    static Result postLoss(List<Trade> trades) {
        List<Trade> triggered = new ArrayList<>();
        List<LocalDateTime> triggers = new ArrayList<>();
        for (List<Trade> session : sessions(trades).values()) {
            int streak = 0;
            boolean triggeredThisStreak = false;
            for (int index = 0; index < session.size(); index++) {
                Trade trade = session.get(index);
                if (pnl(trade).signum() < 0) {
                    streak++;
                    if (streak == LOSS_STREAK_THRESHOLD && !triggeredThisStreak) {
                        triggeredThisStreak = true;
                        LocalDateTime triggerTime = realizedAt(trade);
                        triggers.add(triggerTime);
                        for (int later = index + 1; later < session.size(); later++) {
                            if (entryAt(session.get(later)).isAfter(triggerTime)) triggered.add(session.get(later));
                        }
                    }
                } else {
                    streak = 0;
                    triggeredThisStreak = false;
                }
            }
        }
        BehavioralCounterfactualDto counterfactual = pauseCounterfactual(trades, triggers);
        return result(LeakType.POST_LOSS_OVERTRADING, "Post-loss overtrading", "After two consecutive losses, pause for 20 minutes before entering another trade.", "post-loss-overtrading-v1", triggers.size(), trades, triggered, counterfactual, null);
    }

    static Result sizeEscalation(List<Trade> trades) {
        List<Trade> triggered = new ArrayList<>();
        for (List<Trade> session : sessions(trades).values()) {
            for (int index = 1; index < session.size(); index++) {
                Trade previous = session.get(index - 1);
                Trade current = session.get(index);
                if (pnl(previous).signum() < 0 && comparable(previous, current) && quantity(current) > quantity(previous)) {
                    triggered.add(current);
                }
            }
        }
        return result(LeakType.SIZE_ESCALATION_AFTER_LOSS, "Size escalation after loss", "Keep size unchanged after a losing trade until your next planned setup.", "size-escalation-after-loss-v1", triggered.size(), trades, triggered, notApplicable(trades), null);
    }

    static Result overtrading(List<Trade> trades) {
        Map<LocalDate, List<Trade>> sessions = sessions(trades);
        List<Trade> triggered = new ArrayList<>();
        int occurrences = 0;
        List<List<Trade>> chronologicalSessions = new ArrayList<>(sessions.values());
        List<Integer> appliedThresholds = new ArrayList<>();
        for (int index = 0; index < chronologicalSessions.size(); index++) {
            List<Trade> session = chronologicalSessions.get(index);
            if (index < MIN_OVERTRADING_REFERENCE_SESSIONS) {
                continue;
            }
            int threshold = overtradingThreshold(chronologicalSessions.subList(0, index));
            if (session.size() > threshold) {
                occurrences++;
                triggered.addAll(session.subList(threshold, session.size()));
                appliedThresholds.add(threshold);
            }
        }
        Integer threshold = appliedThresholds.isEmpty() ? null : appliedThresholds.get(appliedThresholds.size() - 1);
        return result(LeakType.OVERTRADING, "Overtrading", "Stop adding trades once you reach your normal session trade limit.", "overtrading-v1", occurrences, trades, triggered, notApplicable(trades), threshold);
    }

    private static Result result(LeakType type, String title, String recommendation, String ruleVersion, int occurrences, List<Trade> all, List<Trade> triggered, BehavioralCounterfactualDto counterfactual, Integer threshold) {
        Set<Trade> triggeredSet = new HashSet<>(triggered);
        List<Trade> baseline = all.stream().filter(trade -> !triggeredSet.contains(trade)).toList();
        BehavioralBaselineDto baselineMetrics = metrics(baseline);
        BehavioralBaselineDto triggeredMetrics = metrics(triggered);
        EvidenceStrength strength = evidenceStrength(occurrences);
        boolean deteriorated = deteriorated(baselineMetrics, triggeredMetrics);
        boolean qualified = strength != EvidenceStrength.INSUFFICIENT && !triggered.isEmpty() && !baseline.isEmpty() && deteriorated;
        String reason = qualified ? null : reason(strength, triggered, baseline, deteriorated);
        return new Result(type, title, recommendation, new BehavioralFindingDto(qualified, reason, occurrences, all.size(), sessions(all).size(), strength, baselineMetrics, triggeredMetrics, counterfactual, ENGINE_VERSION, ruleVersion, threshold), triggered);
    }

    private static BehavioralCounterfactualDto pauseCounterfactual(List<Trade> trades, List<LocalDateTime> triggers) {
        Set<Trade> skipped = new HashSet<>();
        for (LocalDateTime trigger : triggers) {
            LocalDateTime end = trigger.plus(POST_LOSS_PAUSE);
            trades.stream().filter(trade -> entryAt(trade).isAfter(trigger) && entryAt(trade).isBefore(end)).forEach(skipped::add);
        }
        BigDecimal skippedPnl = skipped.stream().map(BehavioralFindingAnalyzer::pnl).reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal lossesAvoided = skipped.stream().map(BehavioralFindingAnalyzer::pnl).filter(value -> value.signum() < 0).map(BigDecimal::abs).reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal winnersForgone = skipped.stream().map(BehavioralFindingAnalyzer::pnl).filter(value -> value.signum() > 0).reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal actual = trades.stream().map(BehavioralFindingAnalyzer::pnl).reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal impact = skippedPnl.negate();
        return new BehavioralCounterfactualDto(!triggers.isEmpty(), impact.signum() > 0, skipped.size(), (int) skipped.stream().filter(t -> pnl(t).signum() > 0).count(), (int) skipped.stream().filter(t -> pnl(t).signum() < 0).count(), round(lossesAvoided), round(winnersForgone), round(actual), round(actual.subtract(skippedPnl)), round(impact));
    }

    private static BehavioralCounterfactualDto notApplicable(List<Trade> trades) {
        BigDecimal actual = trades.stream().map(BehavioralFindingAnalyzer::pnl).reduce(BigDecimal.ZERO, BigDecimal::add);
        return new BehavioralCounterfactualDto(false, false, 0, 0, 0, BigDecimal.ZERO, BigDecimal.ZERO, round(actual), round(actual), BigDecimal.ZERO);
    }

    static EvidenceStrength evidenceStrength(int occurrences) {
        if (occurrences < 5) return EvidenceStrength.INSUFFICIENT;
        if (occurrences < 10) return EvidenceStrength.WEAK;
        if (occurrences < 20) return EvidenceStrength.MODERATE;
        return EvidenceStrength.STRONG;
    }

    private static boolean deteriorated(BehavioralBaselineDto baseline, BehavioralBaselineDto triggered) {
        if (baseline.sampleSize() == 0 || triggered.sampleSize() == 0) return false;
        if (baseline.expectancyR() != null && triggered.expectancyR() != null) return baseline.expectancyR().subtract(triggered.expectancyR()).compareTo(MIN_EXPECTANCY_DETERIORATION_R) >= 0;
        return baseline.averagePnl().subtract(triggered.averagePnl()).compareTo(MIN_AVERAGE_PNL_DETERIORATION) >= 0;
    }

    private static String reason(EvidenceStrength strength, List<Trade> triggered, List<Trade> baseline, boolean deteriorated) {
        if (strength == EvidenceStrength.INSUFFICIENT) return "INSUFFICIENT_EVIDENCE";
        if (triggered.isEmpty() || baseline.isEmpty()) return "INSUFFICIENT_COMPARABLE_DATA";
        if (!deteriorated) return "NO_MEANINGFUL_DETERIORATION";
        return "NOT_QUALIFIED";
    }

    private static BehavioralBaselineDto metrics(List<Trade> trades) {
        if (trades.isEmpty()) return new BehavioralBaselineDto(0, BigDecimal.ZERO, null, BigDecimal.ZERO);
        BigDecimal totalPnl = trades.stream().map(BehavioralFindingAnalyzer::pnl).reduce(BigDecimal.ZERO, BigDecimal::add);
        List<BigDecimal> r = trades.stream().map(Trade::getPnlR).filter(Objects::nonNull).toList();
        BigDecimal expectancyR = r.size() == trades.size() ? average(r) : null;
        return new BehavioralBaselineDto(trades.size(), percent(trades.stream().filter(t -> pnl(t).signum() > 0).count(), trades.size()), expectancyR, average(trades.stream().map(BehavioralFindingAnalyzer::pnl).toList()));
    }

    private static Map<LocalDate, List<Trade>> sessions(List<Trade> trades) {
        return trades.stream().collect(Collectors.groupingBy(Trade::getTradeDate, LinkedHashMap::new, Collectors.toList()));
    }

    private static Comparator<Trade> order() { return Comparator.comparing(BehavioralFindingAnalyzer::entryAt).thenComparing(Trade::getId, Comparator.nullsLast(Long::compareTo)); }
    private static LocalDateTime entryAt(Trade trade) { return trade.getEntryTime() == null ? trade.getTradeDate().atStartOfDay() : trade.getEntryTime(); }
    private static LocalDateTime realizedAt(Trade trade) { return trade.getExitTime() == null ? entryAt(trade) : trade.getExitTime(); }
    private static BigDecimal pnl(Trade trade) { return trade.getPnlAmount() == null ? BigDecimal.ZERO : trade.getPnlAmount(); }
    private static int quantity(Trade trade) { return trade.getQuantity() == null ? 0 : trade.getQuantity(); }
    private static boolean comparable(Trade left, Trade right) { return left.getSymbol() != null && right.getSymbol() != null && left.getSymbol().trim().toUpperCase(Locale.ROOT).equals(right.getSymbol().trim().toUpperCase(Locale.ROOT)); }
    private static int overtradingThreshold(List<List<Trade>> referenceSessions) {
        if (referenceSessions.size() < MIN_OVERTRADING_REFERENCE_SESSIONS) {
            throw new IllegalArgumentException("At least " + MIN_OVERTRADING_REFERENCE_SESSIONS + " reference sessions are required");
        }
        int[] counts = referenceSessions.stream().mapToInt(List::size).sorted().toArray();
        BigDecimal median = median(counts);
        return median.add(BigDecimal.valueOf(OVERTRADING_MARGIN)).setScale(0, RoundingMode.CEILING).intValueExact();
    }

    private static BigDecimal median(int[] values) {
        int middle = values.length / 2;
        if (values.length % 2 == 1) return BigDecimal.valueOf(values[middle]);
        return BigDecimal.valueOf(values[middle - 1]).add(BigDecimal.valueOf(values[middle])).divide(BigDecimal.valueOf(2));
    }
    private static BigDecimal average(List<BigDecimal> values) { return values.stream().reduce(BigDecimal.ZERO, BigDecimal::add).divide(BigDecimal.valueOf(values.size()), 4, RoundingMode.HALF_UP); }
    private static BigDecimal percent(long numerator, int denominator) { return round(BigDecimal.valueOf(numerator).multiply(BigDecimal.valueOf(100)).divide(BigDecimal.valueOf(denominator), 4, RoundingMode.HALF_UP)); }
    private static BigDecimal round(BigDecimal value) { return value.setScale(2, RoundingMode.HALF_UP); }
}
