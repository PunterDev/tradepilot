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

        long wins = trades.stream()
                .filter(t -> getPnl(t).compareTo(BigDecimal.ZERO) > 0)
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

        return CoachDeterministicSummary.builder()
                .totalTrades(totalTrades)
                .netPnl(netPnl)
                .winRate(winRate)
                .profitFactor(profitFactor)
                .bestSetup(bestSetup)
                .worstSetup(worstSetup)
                .bestSession(bestSession)
                .worstSession(worstSession)
                .biggestMistake(biggestMistake)
                .setupAnalysis(analysisByPnl("Setup Analysis", trades, this::getSetup))
                .emotionAnalysis(analysisByPnl("Emotion Analysis", trades, Trade::getEmotion))
                .confidenceAnalysis(analysisByPnl("Confidence Analysis", trades, t -> t.getConfidence() == null ? null : String.valueOf(t.getConfidence())))
                .journalMetadataExamples(journalMetadataExamples(trades))
                .executionScoreAverage(executionAvg)
                .riskScoreAverage(riskAvg)
                .disciplineScoreAverage(disciplineAvg)
                .repeatedMistakes(repeatedMistakes)
                .strengths(strengths)
                .warnings(warnings)
                .build();
    }

    private CoachDeterministicSummary emptySummary(CoachSnapshotType type) {
        String warning = type == CoachSnapshotType.DAILY
                ? "Import trades first to generate a meaningful daily coach."
                : "Import more trades to generate a meaningful weekly coach.";

        return CoachDeterministicSummary.builder()
                .totalTrades(0)
                .netPnl(BigDecimal.ZERO)
                .winRate(BigDecimal.ZERO)
                .profitFactor(BigDecimal.ZERO)
                .bestSetup("Not enough data")
                .worstSetup("Not enough data")
                .bestSession("Not enough data")
                .worstSession("Not enough data")
                .biggestMistake("Not enough data")
                .setupAnalysis("No setup metadata available yet.")
                .emotionAnalysis("No emotion metadata available yet.")
                .confidenceAnalysis("No confidence metadata available yet.")
                .journalMetadataExamples(List.of())
                .executionScoreAverage(BigDecimal.ZERO)
                .riskScoreAverage(BigDecimal.ZERO)
                .disciplineScoreAverage(BigDecimal.ZERO)
                .repeatedMistakes(List.of())
                .strengths(List.of())
                .warnings(List.of(warning))
                .build();
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
