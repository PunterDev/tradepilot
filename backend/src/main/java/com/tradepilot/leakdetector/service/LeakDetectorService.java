package com.tradepilot.leakdetector.service;

import com.fasterxml.jackson.databind.JavaType;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.tradepilot.billing.Feature;
import com.tradepilot.billing.FeatureAccessService;
import com.tradepilot.billing.UsageLimitService;
import com.tradepilot.billing.UsageType;
import com.tradepilot.entity.Trade;
import com.tradepilot.entity.User;
import com.tradepilot.enums.OutcomeType;
import com.tradepilot.leakdetector.dto.LeakDetectorResponse;
import com.tradepilot.leakdetector.dto.LeakResultDto;
import com.tradepilot.leakdetector.dto.LeakSeverity;
import com.tradepilot.leakdetector.dto.LeakType;
import com.tradepilot.leakdetector.entity.AiLeakDetectorSnapshot;
import com.tradepilot.leakdetector.repository.AiLeakDetectorSnapshotRepository;
import com.tradepilot.observability.ObservabilityService;
import com.tradepilot.repository.TradeRepository;
import com.tradepilot.service.TradingAccountService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class LeakDetectorService {
    private static final int MIN_STRONG_SAMPLE = 20;
    private static final BigDecimal LARGE_DOLLAR_LOSS_FLOOR = new BigDecimal("-500.00");

    private final FeatureAccessService featureAccessService;
    private final UsageLimitService usageLimitService;
    private final TradingAccountService tradingAccountService;
    private final TradeRepository tradeRepository;
    private final AiLeakDetectorSnapshotRepository snapshotRepository;
    private final ObjectMapper objectMapper;
    private final ObservabilityService observabilityService;

    @Transactional
    public LeakDetectorResponse getLeakDetector(User user, Long accountId) {
        return loadOrGenerate(user, accountId, false);
    }

    @Transactional
    public LeakDetectorResponse regenerate(User user, Long accountId) {
        return loadOrGenerate(user, accountId, true);
    }

    private LeakDetectorResponse loadOrGenerate(User user, Long accountId, boolean forceRefresh) {
        featureAccessService.requireFeature(user, Feature.AI_LEAK_DETECTOR);
        validateOwnedAccount(user, accountId);

        List<Trade> trades = tradeRepository.findByUserAndAccount_IdOrderByTradeDateAscEntryTimeAscIdAsc(user, accountId);
        String dataHash = dataHash(trades);

        if (!forceRefresh) {
            Optional<AiLeakDetectorSnapshot> cached = snapshotRepository
                    .findMatchingSnapshot(user.getId(), accountId, dataHash, PageRequest.of(0, 1))
                    .stream()
                    .findFirst();
            if (cached.isPresent()) {
                observabilityService.recordCacheEvent("AI Leak Detector", true);
                return toResponse(cached.get(), true);
            }
        }

        observabilityService.recordCacheEvent("AI Leak Detector", false);
        usageLimitService.enforceAiUsage(user, UsageType.AI_LEAK_DETECTOR);

        LeakDetectorResponse generated = analyze(accountId, trades, dataHash);

        AiLeakDetectorSnapshot snapshot = AiLeakDetectorSnapshot.builder()
                .userId(user.getId())
                .accountId(accountId)
                .dataHash(dataHash)
                .totalTrades(trades.size())
                .resultJson(writeLeaks(generated.getLeaks()))
                .model("deterministic-v1.1")
                .build();
        AiLeakDetectorSnapshot saved = snapshotRepository.save(snapshot);
        usageLimitService.recordAiUsage(user, UsageType.AI_LEAK_DETECTOR, saved.getId());

        generated.setId(saved.getId());
        generated.setCached(false);
        generated.setGeneratedAt(saved.getCreatedAt());
        return generated;
    }

    LeakDetectorResponse analyze(Long accountId, List<Trade> trades, String dataHash) {
        LocalDateTime generatedAt = LocalDateTime.now();
        if (trades.size() < MIN_STRONG_SAMPLE) {
            return LeakDetectorResponse.builder()
                    .accountId(accountId)
                    .cached(false)
                    .totalTrades(trades.size())
                    .sampleTradeCount(trades.size())
                    .dataHash(dataHash)
                    .leaks(List.of())
                    .message("Not enough trade history for high-confidence leak detection yet.")
                    .generatedAt(generatedAt)
                    .build();
        }

        LeakContext context = new LeakContext(trades);
        List<LeakResultDto> leaks = new ArrayList<>();
        addWorstGroup(leaks, context, LeakType.DAY_OF_WEEK, "Performance Leak", t -> t.getTradeDate().getDayOfWeek().name());
        addWorstGroup(leaks, context, LeakType.SESSION_TIME, "Session Timing Leak", t -> t.getSession() == null ? null : t.getSession().name());
        addWorstGroup(leaks, context, LeakType.SETUP, "Setup Leak", t -> t.getSetup() == null ? null : t.getSetup().name());
        addWorstGroup(leaks, context, LeakType.CONFIDENCE, "Confidence Leak", t -> t.getConfidence() == null ? null : "Confidence " + t.getConfidence());
        addWorstGroup(leaks, context, LeakType.EMOTION, "Emotion Leak", Trade::getEmotion);
        addLongShortBiasLeak(leaks, context);
        addOvertradingLeak(leaks, context);
        addLargeLossLeak(leaks, context);
        addBreakevenLeak(leaks, context);

        List<LeakResultDto> ranked = deduplicateLeaks(leaks);
        ranked.sort(leakPriorityComparator());
        List<LeakResultDto> topLeaks = ranked.stream().limit(8).toList();

        return LeakDetectorResponse.builder()
                .accountId(accountId)
                .cached(false)
                .totalTrades(trades.size())
                .sampleTradeCount(trades.size())
                .dataHash(dataHash)
                .leaks(topLeaks)
                .message(topLeaks.isEmpty()
                        ? "No statistically meaningful negative leak is visible yet. Keep journaling to strengthen the signal."
                        : "Leak detector found " + topLeaks.size() + " behavior pattern(s) worth reviewing.")
                .biggestLeakTitle(topLeaks.isEmpty() ? null : topLeaks.get(0).getTitle())
                .biggestLeakImpact(topLeaks.isEmpty() ? null : topLeaks.get(0).getEstimatedMonthlyImpact())
                .biggestLeakFix(topLeaks.isEmpty() ? null : topLeaks.get(0).getRecommendedFix())
                .totalEstimatedMonthlyImpact(round(topLeaks.stream()
                        .map(LeakResultDto::getEstimatedMonthlyImpact)
                        .filter(Objects::nonNull)
                        .reduce(BigDecimal.ZERO, BigDecimal::add)))
                .highestConfidence(topLeaks.stream()
                        .map(LeakResultDto::getConfidenceScore)
                        .filter(Objects::nonNull)
                        .max(Integer::compareTo)
                        .orElse(null))
                .generatedAt(generatedAt)
                .build();
    }

    private void addWorstGroup(List<LeakResultDto> leaks, LeakContext context, LeakType type, String baseTitle, Function<Trade, String> classifier) {
        Map<String, List<Trade>> groups = context.trades().stream()
                .map(trade -> new ClassifiedTrade(trade, classifier.apply(trade)))
                .filter(classified -> classified.label() != null && !classified.label().isBlank())
                .collect(Collectors.groupingBy(
                        classified -> classified.label().trim(),
                        Collectors.mapping(ClassifiedTrade::trade, Collectors.toList())
                ));
        groups.entrySet().stream()
                .filter(e -> e.getValue().size() >= 3)
                .map(e -> metric(type, titleFor(type, e.getKey(), baseTitle), e.getKey(), e.getValue(), context))
                .filter(this::isMeaningfulLeak)
                .min(Comparator.comparing(LeakResultDto::getAvgPnl).thenComparing(LeakResultDto::getTotalPnl))
                .ifPresent(leaks::add);
    }

    private void addLongShortBiasLeak(List<LeakResultDto> leaks, LeakContext context) {
        Map<String, List<Trade>> bySide = context.trades().stream()
                .filter(trade -> trade.getSide() != null && !trade.getSide().isBlank())
                .collect(Collectors.groupingBy(trade -> trade.getSide().trim().toUpperCase(Locale.ROOT)));
        if (bySide.size() < 2) {
            return;
        }
        List<Trade> longTrades = bySide.getOrDefault("LONG", List.of());
        List<Trade> shortTrades = bySide.getOrDefault("SHORT", List.of());
        if (longTrades.size() < 3 || shortTrades.size() < 3) {
            return;
        }

        BigDecimal longAvg = averagePnl(longTrades);
        BigDecimal shortAvg = averagePnl(shortTrades);
        List<Trade> weakerTrades = longAvg.compareTo(shortAvg) <= 0 ? longTrades : shortTrades;
        String weakerSide = longAvg.compareTo(shortAvg) <= 0 ? "LONG" : "SHORT";
        BigDecimal strongerAvg = longAvg.compareTo(shortAvg) <= 0 ? shortAvg : longAvg;
        BigDecimal weakerAvg = longAvg.compareTo(shortAvg) <= 0 ? longAvg : shortAvg;

        if (weakerAvg.compareTo(BigDecimal.ZERO) < 0
                && strongerAvg.subtract(weakerAvg).compareTo(new BigDecimal("50.00")) >= 0) {
            LeakResultDto leak = metric(
                    LeakType.LONG_SHORT_BIAS,
                    humanize(weakerSide) + " Side Bias Leak",
                    weakerSide,
                    weakerTrades,
                    context
            );
            if (isMeaningfulLeak(leak)) {
                leaks.add(leak);
            }
        }
    }

    private void addOvertradingLeak(List<LeakResultDto> leaks, LeakContext context) {
        Map<LocalDate, List<Trade>> byDay = context.trades().stream().collect(Collectors.groupingBy(Trade::getTradeDate));
        double averageTradesPerDay = context.trades().size() / Math.max(1.0, byDay.size());
        int threshold = Math.max(4, (int) Math.ceil(averageTradesPerDay * 1.5));
        List<Trade> overtradingTrades = byDay.values().stream()
                .filter(dayTrades -> dayTrades.size() > threshold)
                .flatMap(Collection::stream)
                .toList();
        if (overtradingTrades.size() >= 3) {
            LeakResultDto leak = metric(LeakType.OVERTRADING, "Overtrading Leak", "High trade-count days", overtradingTrades, context);
            if (isMeaningfulLeak(leak)) {
                leaks.add(leak);
            }
        }
    }

    private void addLargeLossLeak(List<LeakResultDto> leaks, LeakContext context) {
        BigDecimal dollarThreshold = largeDollarLossThreshold(context.trades());
        List<Trade> largeLosses = context.trades().stream()
                .filter(trade -> isLargeLoss(trade, dollarThreshold))
                .toList();
        if (largeLosses.size() >= 2) {
            leaks.add(metric(LeakType.LARGE_LOSS, "Large Loss Leak", "Losses beyond your normal loss threshold", largeLosses, context));
        }
    }

    private void addBreakevenLeak(List<LeakResultDto> leaks, LeakContext context) {
        List<Trade> breakevens = context.trades().stream()
                .filter(t -> t.getOutcome() == OutcomeType.BE
                        || Optional.ofNullable(calculateRMultiple(t)).map(r -> r.abs().compareTo(new BigDecimal("0.10")) <= 0).orElse(false))
                .toList();
        if (breakevens.size() >= 5 && breakevens.size() > context.trades().size() * 0.25) {
            LeakResultDto leak = metric(LeakType.BREAKEVEN_OR_EARLY_EXIT, "Breakeven / Early Exit Leak", "Frequent scratches and near-zero exits", breakevens, context);
            leaks.add(leak);
        }
    }

    private LeakResultDto metric(LeakType type, String title, String label, List<Trade> leakTrades, LeakContext context) {
        List<BigDecimal> rMultiples = leakTrades.stream()
                .map(this::calculateRMultiple)
                .filter(Objects::nonNull)
                .toList();
        boolean hasValidRMultiple = rMultiples.size() == leakTrades.size();
        BigDecimal totalPnl = round(leakTrades.stream().map(this::pnl).reduce(BigDecimal.ZERO, BigDecimal::add));
        BigDecimal avgPnl = round(totalPnl.divide(BigDecimal.valueOf(leakTrades.size()), 4, RoundingMode.HALF_UP));
        BigDecimal totalR = hasValidRMultiple ? round(rMultiples.stream().reduce(BigDecimal.ZERO, BigDecimal::add)) : null;
        BigDecimal avgR = hasValidRMultiple ? round(totalR.divide(BigDecimal.valueOf(leakTrades.size()), 4, RoundingMode.HALF_UP)) : null;
        BigDecimal winRate = round(BigDecimal.valueOf(leakTrades.stream().filter(t -> pnl(t).compareTo(BigDecimal.ZERO) > 0).count())
                .multiply(BigDecimal.valueOf(100))
                .divide(BigDecimal.valueOf(leakTrades.size()), 2, RoundingMode.HALF_UP));
        BigDecimal monthlyImpact = calculateMonthlyImpact(leakTrades, context.observedDays());
        BigDecimal monthlyImpactR = hasValidRMultiple ? calculateMonthlyImpactR(leakTrades, context.observedDays()) : null;
        int confidence = confidenceScore(leakTrades.size(), confidenceExpectancy(avgPnl, avgR), lossRate(leakTrades), confidenceTotal(totalPnl, totalR));
        LeakSeverity severity = severity(confidence, leakTrades.size(), monthlyImpact, monthlyImpactR, avgPnl, avgR);

        return LeakResultDto.builder()
                .leakType(type)
                .title(title)
                .summary(label + " is dragging performance by " + formatDollarImpact(totalPnl) + " across " + leakTrades.size() + " trades.")
                .description(descriptionFor(type, label, leakTrades.size(), totalPnl, avgPnl, totalR, avgR))
                .tradeCount(leakTrades.size())
                .winRate(winRate)
                .totalPnl(totalPnl)
                .avgPnl(avgPnl)
                .estimatedMonthlyImpact(monthlyImpact)
                .totalR(totalR)
                .avgR(avgR)
                .estimatedMonthlyImpactR(monthlyImpactR)
                .hasValidRMultiple(hasValidRMultiple)
                .confidenceScore(confidence)
                .severity(severity)
                .recommendedFix(recommendationFor(type, label))
                .relatedTradeCount(leakTrades.size())
                .relatedTradeIds(leakTrades.stream().map(Trade::getId).filter(Objects::nonNull).limit(12).toList())
                .generatedAt(LocalDateTime.now())
                .build();
    }

    /**
     * Monthly impact is deterministic: observed leak frequency per calendar day multiplied by 30,
     * then multiplied by the leak's average dollar PnL. This estimates what the pattern costs if
     * the current trading cadence continues for a typical month.
     */
    private BigDecimal calculateMonthlyImpact(List<Trade> leakTrades, int observedDays) {
        BigDecimal monthlyTradeEstimate = BigDecimal.valueOf(leakTrades.size())
                .multiply(BigDecimal.valueOf(30))
                .divide(BigDecimal.valueOf(Math.max(1, observedDays)), 4, RoundingMode.HALF_UP);
        BigDecimal avgPnl = leakTrades.stream().map(this::pnl).reduce(BigDecimal.ZERO, BigDecimal::add)
                .divide(BigDecimal.valueOf(leakTrades.size()), 4, RoundingMode.HALF_UP);
        return round(monthlyTradeEstimate.multiply(avgPnl));
    }

    private BigDecimal calculateMonthlyImpactR(List<Trade> leakTrades, int observedDays) {
        BigDecimal monthlyTradeEstimate = BigDecimal.valueOf(leakTrades.size())
                .multiply(BigDecimal.valueOf(30))
                .divide(BigDecimal.valueOf(Math.max(1, observedDays)), 4, RoundingMode.HALF_UP);
        List<BigDecimal> rMultiples = leakTrades.stream().map(this::calculateRMultiple).filter(Objects::nonNull).toList();
        if (rMultiples.size() != leakTrades.size()) {
            return null;
        }
        BigDecimal avgR = rMultiples.stream().reduce(BigDecimal.ZERO, BigDecimal::add)
                .divide(BigDecimal.valueOf(leakTrades.size()), 4, RoundingMode.HALF_UP);
        return round(monthlyTradeEstimate.multiply(avgR));
    }

    public int confidenceScore(int sampleSize, BigDecimal avgR, double lossRate, BigDecimal totalR) {
        int sample = (int) Math.min(40, Math.round(sampleSize * 2.2));
        int expectancy = (int) Math.min(30, Math.max(0, avgR.abs().doubleValue() * 35));
        int consistency = (int) Math.min(20, Math.max(0, (lossRate - 0.45) * 45));
        int severity = (int) Math.min(10, Math.max(0, totalR.abs().doubleValue() * 1.8));
        int score = Math.min(100, sample + expectancy + consistency + severity);
        if (sampleSize < 5) return Math.min(score, 45);
        if (sampleSize < 10) return Math.min(score, 65);
        if (sampleSize < 20) return Math.min(score, 78);
        return score;
    }

    private boolean isMeaningfulLeak(LeakResultDto leak) {
        return leak.getAvgPnl().compareTo(BigDecimal.ZERO) < 0
                || leak.getTotalPnl().compareTo(new BigDecimal("-250.00")) <= 0;
    }

    private LeakSeverity severity(
            int confidence,
            int tradeCount,
            BigDecimal monthlyImpact,
            BigDecimal monthlyImpactR,
            BigDecimal avgPnl,
            BigDecimal avgR
    ) {
        boolean largeMonthlyDollarImpact = monthlyImpact.compareTo(new BigDecimal("-1000.00")) <= 0;
        boolean largeMonthlyRImpact = monthlyImpactR != null && monthlyImpactR.compareTo(new BigDecimal("-8.00")) <= 0;
        if (confidence >= 85 && tradeCount >= 20 && (largeMonthlyDollarImpact || largeMonthlyRImpact)) {
            return LeakSeverity.CRITICAL;
        }
        if (confidence >= 70 && (monthlyImpact.compareTo(new BigDecimal("-300.00")) <= 0
                || (avgR != null && avgR.compareTo(new BigDecimal("-0.35")) <= 0))) {
            return LeakSeverity.HIGH;
        }
        if (confidence >= 50 && avgPnl.compareTo(BigDecimal.ZERO) < 0) {
            return LeakSeverity.MEDIUM;
        }
        return LeakSeverity.LOW;
    }

    private double lossRate(List<Trade> trades) {
        return trades.stream().filter(t -> pnl(t).compareTo(BigDecimal.ZERO) < 0).count() / Math.max(1.0, trades.size());
    }

    private BigDecimal pnl(Trade trade) {
        return trade.getPnlAmount() == null ? BigDecimal.ZERO : trade.getPnlAmount();
    }

    private BigDecimal calculateRMultiple(Trade trade) {
        if (trade.getPnlR() != null) {
            return trade.getPnlR();
        }
        if (trade.getPnlAmount() != null
                && trade.getRiskAmount() != null
                && trade.getRiskAmount().compareTo(BigDecimal.ZERO) > 0) {
            return trade.getPnlAmount().divide(trade.getRiskAmount(), 4, RoundingMode.HALF_UP);
        }
        return null;
    }

    private BigDecimal confidenceExpectancy(BigDecimal avgPnl, BigDecimal avgR) {
        if (avgR != null) {
            return avgR;
        }
        return avgPnl.divide(new BigDecimal("500.00"), 4, RoundingMode.HALF_UP);
    }

    private BigDecimal confidenceTotal(BigDecimal totalPnl, BigDecimal totalR) {
        if (totalR != null) {
            return totalR;
        }
        return totalPnl.divide(new BigDecimal("500.00"), 4, RoundingMode.HALF_UP);
    }

    private BigDecimal round(BigDecimal value) {
        return value.setScale(2, RoundingMode.HALF_UP);
    }

    private BigDecimal averagePnl(List<Trade> trades) {
        if (trades.isEmpty()) {
            return BigDecimal.ZERO;
        }
        return trades.stream().map(this::pnl).reduce(BigDecimal.ZERO, BigDecimal::add)
                .divide(BigDecimal.valueOf(trades.size()), 4, RoundingMode.HALF_UP);
    }

    private BigDecimal largeDollarLossThreshold(List<Trade> trades) {
        List<BigDecimal> losses = trades.stream()
                .map(this::pnl)
                .filter(value -> value.compareTo(BigDecimal.ZERO) < 0)
                .map(BigDecimal::abs)
                .toList();
        if (losses.isEmpty()) {
            return LARGE_DOLLAR_LOSS_FLOOR;
        }
        BigDecimal averageLoss = losses.stream().reduce(BigDecimal.ZERO, BigDecimal::add)
                .divide(BigDecimal.valueOf(losses.size()), 4, RoundingMode.HALF_UP);
        BigDecimal threshold = averageLoss.multiply(new BigDecimal("-1.50"));
        return threshold.min(LARGE_DOLLAR_LOSS_FLOOR);
    }

    private boolean isLargeLoss(Trade trade, BigDecimal dollarThreshold) {
        BigDecimal rMultiple = calculateRMultiple(trade);
        if (rMultiple != null) {
            return rMultiple.compareTo(new BigDecimal("-1.00")) <= 0;
        }
        return pnl(trade).compareTo(dollarThreshold) <= 0;
    }

    private List<LeakResultDto> deduplicateLeaks(List<LeakResultDto> leaks) {
        List<LeakResultDto> deduped = new ArrayList<>();
        for (LeakResultDto candidate : leaks) {
            boolean merged = false;
            for (int i = 0; i < deduped.size(); i++) {
                LeakResultDto existing = deduped.get(i);
                if (overlapRatio(candidate, existing) >= 0.85) {
                    if (shouldReplaceDuplicate(existing, candidate)) {
                        deduped.set(i, candidate);
                    }
                    merged = true;
                    break;
                }
            }
            if (!merged) {
                deduped.add(candidate);
            }
        }
        return deduped;
    }

    private boolean shouldReplaceDuplicate(LeakResultDto existing, LeakResultDto candidate) {
        int specificityDelta = specificityRank(candidate.getLeakType()) - specificityRank(existing.getLeakType());
        BigDecimal candidateImpact = candidate.getEstimatedMonthlyImpact().abs();
        BigDecimal existingImpact = existing.getEstimatedMonthlyImpact().abs();
        return specificityDelta > 0 && candidateImpact.compareTo(existingImpact.multiply(new BigDecimal("0.80"))) >= 0;
    }

    private double overlapRatio(LeakResultDto first, LeakResultDto second) {
        Set<Long> firstIds = new HashSet<>(first.getRelatedTradeIds() == null ? List.of() : first.getRelatedTradeIds());
        Set<Long> secondIds = new HashSet<>(second.getRelatedTradeIds() == null ? List.of() : second.getRelatedTradeIds());
        if (firstIds.isEmpty() || secondIds.isEmpty()) {
            return 0;
        }
        Set<Long> intersection = new HashSet<>(firstIds);
        intersection.retainAll(secondIds);
        return intersection.size() / Math.max(1.0, Math.min(firstIds.size(), secondIds.size()));
    }

    private Comparator<LeakResultDto> leakPriorityComparator() {
        return Comparator
                .comparingInt((LeakResultDto leak) -> severityRank(leak.getSeverity()))
                .reversed()
                .thenComparing(Comparator.comparing(
                        (LeakResultDto leak) -> leak.getEstimatedMonthlyImpact() == null
                                ? BigDecimal.ZERO
                                : leak.getEstimatedMonthlyImpact().abs()
                ).reversed())
                .thenComparing(Comparator.comparing(
                        LeakResultDto::getConfidenceScore,
                        Comparator.nullsLast(Integer::compareTo)
                ).reversed())
                .thenComparing(Comparator.comparing(
                        LeakResultDto::getTradeCount,
                        Comparator.nullsLast(Integer::compareTo)
                ).reversed());
    }

    private int severityRank(LeakSeverity severity) {
        if (severity == LeakSeverity.CRITICAL) {
            return 4;
        }
        if (severity == LeakSeverity.HIGH) {
            return 3;
        }
        if (severity == LeakSeverity.MEDIUM) {
            return 2;
        }
        return 1;
    }

    private int specificityRank(LeakType type) {
        if (type == LeakType.LARGE_LOSS) {
            return 9;
        }
        if (type == LeakType.SETUP || type == LeakType.CONFIDENCE || type == LeakType.EMOTION) {
            return 8;
        }
        if (type == LeakType.LONG_SHORT_BIAS) {
            return 7;
        }
        if (type == LeakType.OVERTRADING || type == LeakType.DAY_OF_WEEK || type == LeakType.SESSION_TIME) {
            return 6;
        }
        return 4;
    }

    private String titleFor(LeakType type, String label, String baseTitle) {
        String humanized = humanize(label);
        if (type == LeakType.DAY_OF_WEEK) {
            return humanized + " Performance Leak";
        }
        if (type == LeakType.LONG_SHORT_BIAS) {
            return humanized + " Side Bias Leak";
        }
        return humanized + " " + baseTitle;
    }

    private String descriptionFor(
            LeakType type,
            String label,
            int tradeCount,
            BigDecimal totalPnl,
            BigDecimal avgPnl,
            BigDecimal totalR,
            BigDecimal avgR
    ) {
        String stats = " Sample: " + tradeCount + " trades, " + formatDollarImpact(totalPnl)
                + " total, " + formatDollarImpact(avgPnl) + " average.";
        if (totalR != null && avgR != null) {
            stats += " True R: " + formatRImpact(totalR) + " total, " + formatRImpact(avgR) + " average.";
        }
        String description;
        if (type == LeakType.DAY_OF_WEEK) {
            description = label + " has the weakest day-of-week expectancy in your sample.";
        } else if (type == LeakType.SESSION_TIME) {
            description = label + " is the session window most associated with negative PnL.";
        } else if (type == LeakType.SETUP) {
            description = label + " is not paying you enough for the risk taken.";
        } else if (type == LeakType.CONFIDENCE) {
            description = label + " trades are underperforming compared with your broader sample.";
        } else if (type == LeakType.EMOTION) {
            description = label + " emotion-tagged trades are costing measurable money.";
        } else if (type == LeakType.OVERTRADING) {
            description = "High trade-count days are producing weaker outcomes than normal trading days.";
        } else if (type == LeakType.LONG_SHORT_BIAS) {
            description = label + " trades show a directional bias leak.";
        } else if (type == LeakType.LARGE_LOSS) {
            description = "Large losses are responsible for a disproportionate drawdown.";
        } else {
            description = "Frequent scratches may be reducing expectancy or revealing early-exit behavior.";
        }
        return description + stats;
    }

    private String recommendationFor(LeakType type, String label) {
        if (type == LeakType.DAY_OF_WEEK) {
            return "Reduce risk on " + humanize(label) + " until the setup quality is clearly A+.";
        }
        if (type == LeakType.SESSION_TIME) {
            return "Limit " + humanize(label) + " trading to one planned setup and stop after the first rule break.";
        }
        if (type == LeakType.SETUP) {
            return "Pause " + humanize(label) + " setups or require one extra confirmation before entry.";
        }
        if (type == LeakType.CONFIDENCE) {
            return "Only size up when confidence is backed by checklist quality, not recent P&L.";
        }
        if (type == LeakType.EMOTION) {
            return "Use a two-minute reset before taking another trade when this emotion is present.";
        }
        if (type == LeakType.OVERTRADING) {
            return "Set a hard daily trade cap and stop after two losses or one rule break.";
        }
        if (type == LeakType.LONG_SHORT_BIAS) {
            return "Review directional thesis quality and reduce size on the weaker side.";
        }
        if (type == LeakType.LARGE_LOSS) {
            return "Cap maximum loss at -1R and journal every exception before the next session.";
        }
        return "Define management rules before entry and avoid moving to breakeven without structure.";
    }

    private String formatDollarImpact(BigDecimal value) {
        BigDecimal rounded = round(value == null ? BigDecimal.ZERO : value).abs();
        String prefix = value != null && value.compareTo(BigDecimal.ZERO) < 0 ? "-$" : "$";
        return prefix + rounded;
    }

    private String formatRImpact(BigDecimal value) {
        BigDecimal rounded = round(value == null ? BigDecimal.ZERO : value);
        return (rounded.compareTo(BigDecimal.ZERO) > 0 ? "+" : "") + rounded + "R";
    }

    private String humanize(String value) {
        if (value == null) return "Unspecified";
        try {
            return DayOfWeek.valueOf(value).name().charAt(0) + DayOfWeek.valueOf(value).name().substring(1).toLowerCase();
        } catch (Exception ignored) {
            String lower = value.replace('_', ' ').toLowerCase(Locale.ROOT).trim();
            if (lower.isBlank()) return "Unspecified";
            return Arrays.stream(lower.split("\\s+"))
                    .map(part -> part.substring(0, 1).toUpperCase(Locale.ROOT) + part.substring(1))
                    .collect(Collectors.joining(" "))
                    .trim();
        }
    }

    private String dataHash(List<Trade> trades) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            for (Trade trade : trades) {
                String row = "%s|%s|%s|%s|%s|%s|%s|%s|%s|%s%n".formatted(
                        trade.getId(),
                        trade.getUpdatedAt(),
                        trade.getTradeDate(),
                        trade.getEntryTime(),
                        trade.getSide(),
                        trade.getSetup(),
                        trade.getConfidence(),
                        trade.getEmotion(),
                        pnl(trade),
                        calculateRMultiple(trade)
                );
                digest.update(row.getBytes(StandardCharsets.UTF_8));
            }
            byte[] hash = digest.digest();
            StringBuilder out = new StringBuilder();
            for (byte b : hash) out.append(String.format("%02x", b));
            return out.toString();
        } catch (Exception e) {
            throw new IllegalStateException("Unable to hash leak detector data", e);
        }
    }

    private void validateOwnedAccount(User user, Long accountId) {
        if (accountId == null || accountId <= 0) {
            throw new IllegalArgumentException("accountId is required");
        }
        tradingAccountService.getAccountForImport(user, accountId);
    }

    private String writeLeaks(List<LeakResultDto> leaks) {
        try {
            return objectMapper.writeValueAsString(leaks);
        } catch (Exception e) {
            throw new IllegalStateException("Unable to serialize leak detector result", e);
        }
    }

    private LeakDetectorResponse toResponse(AiLeakDetectorSnapshot snapshot, boolean cached) {
        try {
            JavaType leakListType = objectMapper.getTypeFactory()
                    .constructCollectionType(List.class, LeakResultDto.class);
            List<LeakResultDto> leaks = objectMapper.readValue(snapshot.getResultJson(), leakListType);
            normalizeRelatedTradeCounts(leaks);
            return LeakDetectorResponse.builder()
                    .id(snapshot.getId())
                    .accountId(snapshot.getAccountId())
                    .cached(cached)
                    .totalTrades(snapshot.getTotalTrades())
                    .sampleTradeCount(snapshot.getTotalTrades())
                    .dataHash(snapshot.getDataHash())
                    .leaks(leaks)
                    .message(leaks.isEmpty()
                            ? "Not enough trade history yet. Import more trades or journal at least 20 trades for stronger leak detection."
                            : "Leak detector found " + leaks.size() + " behavior pattern(s) worth reviewing.")
                    .biggestLeakTitle(leaks.isEmpty() ? null : leaks.get(0).getTitle())
                    .biggestLeakImpact(leaks.isEmpty() ? null : leaks.get(0).getEstimatedMonthlyImpact())
                    .biggestLeakFix(leaks.isEmpty() ? null : leaks.get(0).getRecommendedFix())
                    .totalEstimatedMonthlyImpact(round(leaks.stream()
                            .map(LeakResultDto::getEstimatedMonthlyImpact)
                            .filter(Objects::nonNull)
                            .reduce(BigDecimal.ZERO, BigDecimal::add)))
                    .highestConfidence(leaks.stream()
                            .map(LeakResultDto::getConfidenceScore)
                            .filter(Objects::nonNull)
                            .max(Integer::compareTo)
                            .orElse(null))
                    .generatedAt(snapshot.getUpdatedAt())
                    .build();
        } catch (Exception e) {
            throw new IllegalStateException("Unable to read leak detector cache", e);
        }
    }

    private void normalizeRelatedTradeCounts(List<LeakResultDto> leaks) {
        for (LeakResultDto leak : leaks) {
            Integer tradeCount = leak.getTradeCount();
            Integer relatedTradeCount = leak.getRelatedTradeCount();
            if (tradeCount != null && tradeCount > 0
                    && (relatedTradeCount == null || relatedTradeCount < tradeCount)) {
                leak.setRelatedTradeCount(tradeCount);
            } else if (relatedTradeCount == null && leak.getRelatedTradeIds() != null) {
                leak.setRelatedTradeCount(leak.getRelatedTradeIds().size());
            }
        }
    }

    private record LeakContext(List<Trade> trades, int observedDays) {
        private LeakContext(List<Trade> trades) {
            this(trades, observedDays(trades));
        }

        private static int observedDays(List<Trade> trades) {
            if (trades.isEmpty()) return 1;
            LocalDate min = trades.stream().map(Trade::getTradeDate).min(LocalDate::compareTo).orElse(LocalDate.now());
            LocalDate max = trades.stream().map(Trade::getTradeDate).max(LocalDate::compareTo).orElse(min);
            return Math.max(1, (int) (max.toEpochDay() - min.toEpochDay() + 1));
        }
    }

    private record ClassifiedTrade(Trade trade, String label) {
    }
}
