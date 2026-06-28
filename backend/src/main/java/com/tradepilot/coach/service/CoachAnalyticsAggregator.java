package com.tradepilot.coach.service;

import com.tradepilot.coach.dto.CoachDeterministicSummary;
import com.tradepilot.coach.entity.CoachSnapshotType;
import com.tradepilot.entity.Trade;
import com.tradepilot.enums.MistakeTag;
import com.tradepilot.repository.TradeRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.DayOfWeek;
import java.time.Duration;
import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class CoachAnalyticsAggregator {

    private final TradeRepository tradeRepository;

    public CoachDeterministicSummary aggregate(
            Long userId,
            Long accountId,
            CoachSnapshotType type,
            java.time.LocalDate periodStart,
            java.time.LocalDate periodEnd
    ) {
        List<Trade> trades =
                tradeRepository.findByUser_IdAndAccount_IdAndTradeDateBetween(
                        userId,
                        accountId,
                        periodStart,
                        periodEnd
                );

        if (trades.isEmpty()) {
            return emptySummary(type);
        }

        int totalTrades = trades.size();

        BigDecimal netPnl = trades.stream()
                .map(this::getPnl)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        long longTradeCount = trades.stream().filter(t -> "LONG".equals(normalizeSide(t.getSide()))).count();
        long shortTradeCount = trades.stream().filter(t -> "SHORT".equals(normalizeSide(t.getSide()))).count();
        long unknownTradeCount = trades.stream().filter(t -> "UNKNOWN".equals(normalizeSide(t.getSide()))).count();
        BigDecimal longPnl = pnlForSide(trades, "LONG");
        BigDecimal shortPnl = pnlForSide(trades, "SHORT");
        String worstTradeSide = trades.stream()
                .min(Comparator.comparing(this::getPnl))
                .map(trade -> normalizeSide(trade.getSide()))
                .orElse("UNKNOWN");

        long wins = trades.stream()
                .filter(t -> getPnl(t).compareTo(BigDecimal.ZERO) > 0)
                .count();
        long losses = trades.stream()
                .filter(t -> getPnl(t).compareTo(BigDecimal.ZERO) < 0)
                .count();

        BigDecimal winRate = BigDecimal.valueOf(wins)
                .multiply(BigDecimal.valueOf(100))
                .divide(BigDecimal.valueOf(totalTrades), 2, RoundingMode.HALF_UP);

        BigDecimal grossProfit = trades.stream()
                .map(this::getPnl)
                .filter(p -> p.compareTo(BigDecimal.ZERO) > 0)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        BigDecimal grossLoss = trades.stream()
                .map(this::getPnl)
                .filter(p -> p.compareTo(BigDecimal.ZERO) < 0)
                .map(BigDecimal::abs)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        BigDecimal profitFactor = grossLoss.compareTo(BigDecimal.ZERO) == 0
                ? grossProfit
                : grossProfit.divide(grossLoss, 2, RoundingMode.HALF_UP);

        String bestSetup = bestByPnl(trades, this::getSetup);
        String worstSetup = worstByPnl(trades, this::getSetup);

        String bestSession = bestByPnl(trades, this::getSession);
        String worstSession = worstByPnl(trades, this::getSession);

        List<String> repeatedMistakes = repeatedMistakes(trades);

        String biggestMistake = repeatedMistakes.isEmpty()
                ? "No repeated mistake detected"
                : repeatedMistakes.get(0);

        BigDecimal executionAvg = averageScore(trades, this::getExecutionScore);
        BigDecimal riskAvg = averageScore(trades, this::getRiskScore);
        BigDecimal disciplineAvg = averageScore(trades, this::getDisciplineScore);

        List<String> strengths = detectStrengths(
                winRate,
                profitFactor,
                bestSetup,
                bestSession,
                executionAvg,
                riskAvg,
                disciplineAvg
        );

        List<String> warnings = detectWarnings(
                type,
                trades,
                netPnl,
                winRate,
                worstSetup,
                worstSession,
                repeatedMistakes,
                executionAvg,
                riskAvg,
                disciplineAvg
        );

        return CoachDeterministicSummary.of(
                type == CoachSnapshotType.DAILY ? periodStart : null,
                totalTrades,
                netPnl,
                winRate,
                profitFactor,
                Math.toIntExact(longTradeCount),
                Math.toIntExact(shortTradeCount),
                Math.toIntExact(unknownTradeCount),
                Math.toIntExact(wins),
                Math.toIntExact(losses),
                dominantDirection(longTradeCount, shortTradeCount, unknownTradeCount, longPnl, shortPnl),
                instruments(trades),
                longPnl,
                shortPnl,
                worstTradeSide,
                selectedDaySummary(type, trades, netPnl, longTradeCount, shortTradeCount, unknownTradeCount),
                primaryIssueToday(type, trades, netPnl),
                setupDirectionReview(type, trades),
                dataQualityLabel(trades),
                dataQualityReason(trades),
                nextSessionRule(type, trades),
                bestSetup,
                worstSetup,
                bestSession,
                worstSession,
                biggestMistake,
                analysisByPnl("Setup Analysis", trades, this::getSetup),
                analysisByPnl("Emotion Analysis", trades, Trade::getEmotion),
                analysisByPnl("Confidence Analysis", trades, t -> t.getConfidence() == null ? null : String.valueOf(t.getConfidence())),
                journalMetadataExamples(trades),
                executionAvg,
                riskAvg,
                disciplineAvg,
                repeatedMistakes,
                strengths,
                warnings
        );
    }

    private CoachDeterministicSummary emptySummary(CoachSnapshotType type) {
        if (type == CoachSnapshotType.DAILY) {
            return CoachDeterministicSummary.emptyDaily("Import trades first to generate a meaningful daily coach.");
        }
        return CoachDeterministicSummary.emptyWeekly("Import trades for this week before generating weekly coaching.");
    }

    private String bestByPnl(List<Trade> trades, Function<Trade, String> classifier) {
        return trades.stream()
                .filter(t -> clean(classifier.apply(t)) != null)
                .collect(Collectors.groupingBy(
                        t -> clean(classifier.apply(t)),
                        Collectors.reducing(BigDecimal.ZERO, this::getPnl, BigDecimal::add)
                ))
                .entrySet()
                .stream()
                .max(Map.Entry.comparingByValue())
                .map(Map.Entry::getKey)
                .orElse("Not enough data");
    }

    private String worstByPnl(List<Trade> trades, Function<Trade, String> classifier) {
        return trades.stream()
                .filter(t -> clean(classifier.apply(t)) != null)
                .collect(Collectors.groupingBy(
                        t -> clean(classifier.apply(t)),
                        Collectors.reducing(BigDecimal.ZERO, this::getPnl, BigDecimal::add)
                ))
                .entrySet()
                .stream()
                .min(Map.Entry.comparingByValue())
                .map(Map.Entry::getKey)
                .orElse("Not enough data");
    }

    private List<String> repeatedMistakes(List<Trade> trades) {
        Map<String, Long> counts = trades.stream()
                .flatMap(t -> getMistakes(t).stream())
                .map(this::clean)
                .filter(Objects::nonNull)
                .collect(Collectors.groupingBy(m -> m, Collectors.counting()));

        return counts.entrySet()
                .stream()
                .sorted(Map.Entry.<String, Long>comparingByValue().reversed())
                .limit(5)
                .map(e -> e.getKey() + " (" + e.getValue() + "x)")
                .toList();
    }

    private String analysisByPnl(String label, List<Trade> trades, Function<Trade, String> classifier) {
        Map<String, BigDecimal> pnlByTag = trades.stream()
                .filter(t -> clean(classifier.apply(t)) != null)
                .collect(Collectors.groupingBy(
                        t -> clean(classifier.apply(t)),
                        Collectors.reducing(BigDecimal.ZERO, this::getPnl, BigDecimal::add)
                ));
        if (pnlByTag.isEmpty()) {
            return label + ": no metadata available.";
        }
        String best = pnlByTag.entrySet().stream()
                .max(Map.Entry.comparingByValue())
                .map(e -> e.getKey() + " generated " + e.getValue())
                .orElse("not enough data");
        String worst = pnlByTag.entrySet().stream()
                .min(Map.Entry.comparingByValue())
                .map(e -> e.getKey() + " generated " + e.getValue())
                .orElse("not enough data");
        return label + ": best " + best + "; weakest " + worst + ".";
    }

    private String selectedDaySummary(
            CoachSnapshotType type,
            List<Trade> trades,
            BigDecimal netPnl,
            long longTradeCount,
            long shortTradeCount,
            long unknownTradeCount
    ) {
        if (type != CoachSnapshotType.DAILY) {
            return trades.size() + " trades, " + money(netPnl) + " net.";
        }
        if (trades.size() == 1) {
            Trade trade = trades.get(0);
            String side = normalizeSide(trade.getSide());
            return "One trade, " + money(netPnl) + " net. The session "
                    + (netPnl.compareTo(BigDecimal.ZERO) < 0 ? "loss" : "result")
                    + " came from a single " + trade.getSymbol() + " " + side + " trade.";
        }
        return trades.size() + " trades, " + money(netPnl) + " net. Long trades: "
                + longTradeCount + ", short trades: " + shortTradeCount + ", unknown: "
                + unknownTradeCount + ".";
    }

    private String primaryIssueToday(CoachSnapshotType type, List<Trade> trades, BigDecimal netPnl) {
        if (type != CoachSnapshotType.DAILY) {
            return netPnl.compareTo(BigDecimal.ZERO) < 0
                    ? "The week was negative. Review the largest loss clusters and reduce risk on weak patterns."
                    : "The week was stable. Protect the behaviors that produced positive expectancy.";
        }
        if (trades.size() == 1) {
            return "This was not an overtrading day. The key review is whether the entry had enough confirmation, a predefined stop, and a realistic target before execution.";
        }
        if (trades.size() >= 5 && netPnl.compareTo(BigDecimal.ZERO) < 0) {
            return "Multiple trades with negative P&L. Check whether trade frequency increased after the first loss.";
        }
        return netPnl.compareTo(BigDecimal.ZERO) < 0
                ? "The session was negative. Review entry quality, invalidation, and whether risk was reduced after the loss."
                : "The session was controlled. Keep the same selection standards next session.";
    }

    private String setupDirectionReview(CoachSnapshotType type, List<Trade> trades) {
        if (trades.stream().anyMatch(trade -> "UNKNOWN".equals(normalizeSide(trade.getSide())))) {
            return "Direction was unknown, so review the original execution data before judging directional performance.";
        }
        if (type == CoachSnapshotType.DAILY && trades.size() == 1) {
            Trade trade = trades.get(0);
            String side = normalizeSide(trade.getSide());
            BigDecimal pnl = getPnl(trade);
            if (pnl.compareTo(BigDecimal.ZERO) > 0) {
                return "The " + side
                        + " trade was profitable. Review whether the entry had a clear thesis, invalidation point, predefined stop, and realistic target before taking a similar setup.";
            }
            if (pnl.compareTo(BigDecimal.ZERO) < 0) {
                return "The " + side
                        + " idea failed. Review whether the entry had valid bias, clean location, predefined stop, and realistic target before taking a similar setup.";
            }
            return "The " + side
                    + " trade was essentially breakeven. Review whether the entry had a clear thesis, invalidation point, predefined stop, and realistic target before taking a similar setup.";
        }
        Map<String, BigDecimal> pnlBySide = trades.stream()
                .collect(Collectors.groupingBy(
                        trade -> normalizeSide(trade.getSide()),
                        Collectors.reducing(BigDecimal.ZERO, this::getPnl, BigDecimal::add)
                ));
        return "Direction review: LONG " + money(pnlBySide.getOrDefault("LONG", BigDecimal.ZERO))
                + ", SHORT " + money(pnlBySide.getOrDefault("SHORT", BigDecimal.ZERO))
                + ", UNKNOWN " + money(pnlBySide.getOrDefault("UNKNOWN", BigDecimal.ZERO)) + ".";
    }

    private String dataQualityLabel(List<Trade> trades) {
        if (trades.size() < 3) {
            return "Limited Data";
        }
        if (trades.stream().anyMatch(trade -> "UNKNOWN".equals(normalizeSide(trade.getSide())))) {
            return "Direction Data Warning";
        }
        if (!hasJournalMetadata(trades)) {
            return "Execution Data Only";
        }
        return "Good Data";
    }

    private String dominantDirection(
            long longTradeCount,
            long shortTradeCount,
            long unknownTradeCount,
            BigDecimal longPnl,
            BigDecimal shortPnl
    ) {
        if (longTradeCount > 0 && shortTradeCount == 0 && unknownTradeCount == 0) {
            return "LONG";
        }
        if (shortTradeCount > 0 && longTradeCount == 0 && unknownTradeCount == 0) {
            return "SHORT";
        }
        if (longTradeCount > shortTradeCount && longPnl.compareTo(shortPnl) >= 0) {
            return "LONG";
        }
        if (shortTradeCount > longTradeCount && shortPnl.compareTo(longPnl) >= 0) {
            return "SHORT";
        }
        if (longTradeCount > 0 && shortTradeCount > 0) {
            return "MIXED";
        }
        return "UNKNOWN";
    }

    private List<String> instruments(List<Trade> trades) {
        return trades.stream()
                .map(Trade::getSymbol)
                .map(this::clean)
                .filter(Objects::nonNull)
                .distinct()
                .toList();
    }

    private String dataQualityReason(List<Trade> trades) {
        long unknownSides = trades.stream().filter(trade -> "UNKNOWN".equals(normalizeSide(trade.getSide()))).count();
        if (trades.size() == 1 && !hasJournalMetadata(trades)) {
            if (unknownSides > 0) {
                return "Limited confidence: only one trade was taken, direction was UNKNOWN, and setup/emotion notes were not available. Coaching is based mainly on execution data.";
            }
            return "Limited confidence: only one trade was taken, and setup/emotion notes were not available. Coaching is based mainly on execution data.";
        }
        List<String> reasons = new ArrayList<>();
        if (trades.size() == 1) {
            reasons.add("Limited confidence: only one trade was taken.");
        } else if (trades.size() < 3) {
            reasons.add("Limited confidence: only " + trades.size() + " trades were in this sample.");
        }
        if (unknownSides > 0) {
            reasons.add(unknownSides + " trade(s) have UNKNOWN direction, so direction-specific coaching is limited.");
        }
        if (!hasJournalMetadata(trades)) {
            reasons.add("Setup/emotion notes were not available. Coaching is based mainly on execution data.");
        }
        if (reasons.isEmpty()) {
            reasons.add("Trade direction and journal metadata are available for this sample.");
        }
        return String.join(" ", reasons);
    }

    private String nextSessionRule(CoachSnapshotType type, List<Trade> trades) {
        if (type == CoachSnapshotType.DAILY && trades.size() == 1) {
            return "Before entering, define direction thesis, invalidation, and target. Skip the trade if any are unclear.";
        }
        if (trades.size() >= 5) {
            return "Set a trade cap before the session starts and pause after two rule breaks.";
        }
        return "Trade only when bias, confirmation, invalidation, and target are all clear before entry.";
    }

    private boolean hasJournalMetadata(List<Trade> trades) {
        return trades.stream().anyMatch(trade ->
                trade.getSetup() != null
                        || trade.getConfidence() != null
                        || clean(trade.getEmotion()) != null
                        || clean(trade.getNotes()) != null
        );
    }

    private BigDecimal pnlForSide(List<Trade> trades, String side) {
        return trades.stream()
                .filter(trade -> side.equals(normalizeSide(trade.getSide())))
                .map(this::getPnl)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    private String normalizeSide(String side) {
        if (side == null || side.isBlank()) {
            return "UNKNOWN";
        }
        String normalized = side.trim().toUpperCase(Locale.ROOT);
        if ("LONG".equals(normalized) || "SHORT".equals(normalized)) {
            return normalized;
        }
        return "UNKNOWN";
    }

    private String money(BigDecimal value) {
        BigDecimal amount = value == null ? BigDecimal.ZERO : value.setScale(2, RoundingMode.HALF_UP);
        return (amount.compareTo(BigDecimal.ZERO) < 0 ? "-$" : "$") + amount.abs();
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
                    if (clean(trade.getEmotion()) != null) {
                        parts.add("emotion=" + trade.getEmotion());
                    }
                    if (clean(trade.getNotes()) != null) {
                        parts.add("notes=" + trade.getNotes());
                    }
                    return parts.isEmpty() ? null : trade.getSymbol() + " " + getPnl(trade) + " (" + String.join(", ", parts) + ")";
                })
                .filter(Objects::nonNull)
                .limit(10)
                .toList();
    }

    private BigDecimal averageScore(
            List<Trade> trades,
            Function<Trade, BigDecimal> scoreGetter
    ) {
        List<BigDecimal> scores = trades.stream()
                .map(scoreGetter)
                .filter(Objects::nonNull)
                .toList();

        if (scores.isEmpty()) {
            return BigDecimal.ZERO;
        }

        BigDecimal total = scores.stream()
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        return total.divide(BigDecimal.valueOf(scores.size()), 2, RoundingMode.HALF_UP);
    }

    private List<String> detectStrengths(
            BigDecimal winRate,
            BigDecimal profitFactor,
            String bestSetup,
            String bestSession,
            BigDecimal executionAvg,
            BigDecimal riskAvg,
            BigDecimal disciplineAvg
    ) {
        List<String> strengths = new ArrayList<>();

        if (winRate.compareTo(BigDecimal.valueOf(50)) >= 0) {
            strengths.add("Win rate was above 50%.");
        }

        if (profitFactor.compareTo(BigDecimal.ONE) > 0) {
            strengths.add("Profit factor was positive.");
        }

        if (!"Not enough data".equals(bestSetup)) {
            strengths.add("Best setup: " + bestSetup);
        }

        if (!"Not enough data".equals(bestSession)) {
            strengths.add("Best session: " + bestSession);
        }

        if (executionAvg.compareTo(BigDecimal.valueOf(70)) >= 0) {
            strengths.add("Execution score was strong.");
        }

        if (riskAvg.compareTo(BigDecimal.valueOf(70)) >= 0) {
            strengths.add("Risk management score was strong.");
        }

        if (disciplineAvg.compareTo(BigDecimal.valueOf(70)) >= 0) {
            strengths.add("Discipline score was strong.");
        }

        return strengths;
    }

    private List<String> detectWarnings(
            CoachSnapshotType type,
            List<Trade> trades,
            BigDecimal netPnl,
            BigDecimal winRate,
            String worstSetup,
            String worstSession,
            List<String> repeatedMistakes,
            BigDecimal executionAvg,
            BigDecimal riskAvg,
            BigDecimal disciplineAvg
    ) {
        List<String> warnings = new ArrayList<>();

        if (netPnl.compareTo(BigDecimal.ZERO) < 0) {
            warnings.add("Net P&L was negative.");
        }

        if (winRate.compareTo(BigDecimal.valueOf(40)) < 0) {
            warnings.add("Win rate was below 40%.");
        }

        if (!"Not enough data".equals(worstSetup)) {
            warnings.add("Weakest setup: " + worstSetup);
        }

        if (!"Not enough data".equals(worstSession)) {
            warnings.add("Weakest session: " + worstSession);
        }

        if (!repeatedMistakes.isEmpty()) {
            warnings.add("Repeated mistake detected: " + repeatedMistakes.get(0));
        }

        if (executionAvg.compareTo(BigDecimal.valueOf(60)) < 0) {
            warnings.add("Execution score was weak.");
        }

        if (riskAvg.compareTo(BigDecimal.valueOf(60)) < 0) {
            warnings.add("Risk score was weak.");
        }

        if (disciplineAvg.compareTo(BigDecimal.valueOf(60)) < 0) {
            warnings.add("Discipline score was weak.");
        }

        if (detectEmotionalTrading(trades)) {
            warnings.add("Possible revenge trading behavior detected.");
        }

        if (type == CoachSnapshotType.WEEKLY && detectFridayWeakness(trades)) {
            warnings.add("Friday trading performance was weak.");
        }

        return warnings;
    }

    private boolean detectEmotionalTrading(List<Trade> trades) {
        if (trades.size() < 3) {
            return false;
        }

        List<Trade> sortedTrades = trades.stream()
                .filter(t -> t.getEntryTime() != null)
                .sorted(Comparator.comparing(Trade::getEntryTime))
                .toList();

        int revengePatterns = 0;

        for (int i = 1; i < sortedTrades.size(); i++) {
            Trade previous = sortedTrades.get(i - 1);
            Trade current = sortedTrades.get(i);

            if (getPnl(previous).compareTo(BigDecimal.ZERO) < 0
                    && previous.getExitTime() != null
                    && current.getEntryTime() != null) {

                long minutes = Duration.between(
                        previous.getExitTime(),
                        current.getEntryTime()
                ).toMinutes();

                if (minutes >= 0 && minutes <= 5) {
                    revengePatterns++;
                }
            }
        }

        return revengePatterns >= 2;
    }

    private boolean detectFridayWeakness(List<Trade> trades) {
        List<Trade> fridayTrades = trades.stream()
                .filter(t -> t.getTradeDate() != null)
                .filter(t -> t.getTradeDate().getDayOfWeek() == DayOfWeek.FRIDAY)
                .toList();

        if (fridayTrades.isEmpty()) {
            return false;
        }

        BigDecimal fridayPnl = fridayTrades.stream()
                .map(this::getPnl)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        return fridayPnl.compareTo(BigDecimal.ZERO) < 0;
    }

    private String clean(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }

        return value.trim().replace("_", " ");
    }

    private BigDecimal getPnl(Trade trade) {
        return trade.getPnlAmount() == null
                ? BigDecimal.ZERO
                : trade.getPnlAmount();
    }

    private String getSetup(Trade trade) {
        return trade.getSetup() == null
                ? null
                : trade.getSetup().name();
    }

    private String getSession(Trade trade) {
        return trade.getSession() == null
                ? null
                : trade.getSession().name();
    }

    private BigDecimal getExecutionScore(Trade trade) {
        return trade.getExecutionRating() == null
                ? null
                : BigDecimal.valueOf(trade.getExecutionRating() * 10L);
    }

    private BigDecimal getRiskScore(Trade trade) {
        if (trade.getFollowedPlan() == null) {
            return null;
        }

        return trade.getFollowedPlan()
                ? BigDecimal.valueOf(85)
                : BigDecimal.valueOf(40);
    }

    private BigDecimal getDisciplineScore(Trade trade) {
        BigDecimal score = BigDecimal.valueOf(70);

        if (Boolean.TRUE.equals(trade.getFollowedPlan())) {
            score = score.add(BigDecimal.valueOf(15));
        }

        if (Boolean.FALSE.equals(trade.getFollowedPlan())) {
            score = score.subtract(BigDecimal.valueOf(20));
        }

        if (trade.getMistakeTag() != null) {
            score = score.subtract(BigDecimal.valueOf(20));
        }

        if (trade.getExecutionRating() != null) {
            score = score.add(BigDecimal.valueOf(trade.getExecutionRating() - 5L));
        }

        if (score.compareTo(BigDecimal.ZERO) < 0) {
            return BigDecimal.ZERO;
        }

        if (score.compareTo(BigDecimal.valueOf(100)) > 0) {
            return BigDecimal.valueOf(100);
        }

        return score;
    }

    private List<String> getMistakes(Trade trade) {
        MistakeTag mistakeTag = trade.getMistakeTag();

        if (mistakeTag == null) {
            return List.of();
        }

        return List.of(mistakeTag.name());
    }
}
