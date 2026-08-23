package com.tradepilot.tradereplay.service;

import com.tradepilot.entity.Trade;
import com.tradepilot.tradereplay.entity.TradeReplayInsightType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Duration;
import java.time.Instant;
import java.util.*;

@Component
public class TradeReplayAnalysisEngine {
    private static final Logger log = LoggerFactory.getLogger(TradeReplayAnalysisEngine.class);
    private static final BigDecimal HUNDRED = new BigDecimal("100");
    private static final BigDecimal STRONG_CAPTURE_THRESHOLD = new BigDecimal("80");
    private static final BigDecimal GOOD_CAPTURE_THRESHOLD = new BigDecimal("60");
    private static final BigDecimal MODERATE_CAPTURE_THRESHOLD = new BigDecimal("30");
    private static final BigDecimal LARGE_GIVEBACK_PCT = new BigDecimal("50");
    private static final BigDecimal MEANINGFUL_FAVORABLE_POINTS = new BigDecimal("2");

    public TradeReplayAnalysis analyze(Trade trade, List<HistoricalCandle> candles, String canonicalInstrument) {
        if (trade == null || candles == null || candles.isEmpty()) {
            return new TradeReplayAnalysis(Map.of(), List.of(), null, null);
        }
        boolean isShort = "SHORT".equalsIgnoreCase(trade.getSide());
        boolean isLong = "LONG".equalsIgnoreCase(trade.getSide());
        BigDecimal entry = trade.getEntryPrice();
        BigDecimal exit = trade.getExitPrice();
        BigDecimal pointValue = pointValue(canonicalInstrument);
        BigDecimal quantity = validQuantity(trade);
        Instant entryInstant = trade.getEntryTime() == null ? null : TradeReplayTime.toInstant(trade.getEntryTime());
        Instant exitInstant = trade.getExitTime() == null ? null : TradeReplayTime.toInstant(trade.getExitTime());
        boolean validTradeInputs = (isLong || isShort)
                && positive(entry)
                && positive(exit)
                && quantity != null
                && entryInstant != null
                && exitInstant != null
                && exitInstant.isAfter(entryInstant);
        BigDecimal realizedPoints = validTradeInputs ? (isShort ? entry.subtract(exit) : exit.subtract(entry)) : null;
        BigDecimal realizedDollars = trade.getPnlAmount() != null ? trade.getPnlAmount() : dollars(realizedPoints, pointValue, quantity);

        List<HistoricalCandle> sortedCandles = candles.stream()
                .filter(Objects::nonNull)
                .filter(candle -> candle.timestamp() != null)
                .sorted(Comparator.comparing(HistoricalCandle::timestamp))
                .toList();
        List<HistoricalCandle> tradeCandles = validTradeInputs
                ? sortedCandles.stream()
                        .filter(c -> !c.timestamp().isBefore(entryInstant) && !c.timestamp().isAfter(exitInstant))
                        .toList()
                : List.of();

        HistoricalCandle mfe = null;
        HistoricalCandle mae = null;
        BigDecimal mfePoints = null;
        BigDecimal maePoints = null;
        for (HistoricalCandle candle : tradeCandles) {
            if (!validCandle(candle)) continue;
            BigDecimal favorable = maxZero(isShort ? entry.subtract(candle.low()) : candle.high().subtract(entry));
            BigDecimal adverse = maxZero(isShort ? candle.high().subtract(entry) : entry.subtract(candle.low()));
            if (mfePoints == null || favorable.compareTo(mfePoints) > 0) {
                mfePoints = favorable;
                mfe = candle;
            }
            if (maePoints == null || adverse.compareTo(maePoints) > 0) {
                maePoints = adverse;
                mae = candle;
            }
        }
        HistoricalCandle maxHighCandle = tradeCandles.stream()
                .filter(this::validCandle)
                .max(Comparator.comparing(HistoricalCandle::high))
                .orElse(null);
        HistoricalCandle minLowCandle = tradeCandles.stream()
                .filter(this::validCandle)
                .min(Comparator.comparing(HistoricalCandle::low))
                .orElse(null);
        log.debug(
                "Trade Replay extrema. tradeId={}, contract={}, side={}, entryPrice={}, maxHigh={}, maxHighTimestamp={}, maxHighOpen={}, maxHighLow={}, maxHighClose={}, minLow={}, minLowTimestamp={}, minLowOpen={}, minLowHigh={}, minLowClose={}",
                trade.getId(), canonicalInstrument, trade.getSide(), entry,
                maxHighCandle == null ? null : maxHighCandle.high(),
                maxHighCandle == null ? null : maxHighCandle.timestamp(),
                maxHighCandle == null ? null : maxHighCandle.open(),
                maxHighCandle == null ? null : maxHighCandle.low(),
                maxHighCandle == null ? null : maxHighCandle.close(),
                minLowCandle == null ? null : minLowCandle.low(),
                minLowCandle == null ? null : minLowCandle.timestamp(),
                minLowCandle == null ? null : minLowCandle.open(),
                minLowCandle == null ? null : minLowCandle.high(),
                minLowCandle == null ? null : minLowCandle.close()
        );
        log.debug(
                "Trade Replay excursion inputs. tradeId={}, contract={}, side={}, quantity={}, entryPrice={}, exitPrice={}, tradeEntryTime={}, tradeExitTime={}, candleCount={}, tradeCandleCount={}, firstCandleTimestamp={}, firstCandleOpen={}, firstCandleHigh={}, firstCandleLow={}, firstCandleClose={}, candleHighMax={}, candleLowMin={}, computedMfePoints={}, computedMaePoints={}, computedRealizedPoints={}",
                trade.getId(), canonicalInstrument, trade.getSide(), quantity, entry, exit, trade.getEntryTime(), trade.getExitTime(),
                sortedCandles.size(), tradeCandles.size(),
                first(sortedCandles) == null ? null : first(sortedCandles).timestamp(),
                first(sortedCandles) == null ? null : first(sortedCandles).open(),
                first(sortedCandles) == null ? null : first(sortedCandles).high(),
                first(sortedCandles) == null ? null : first(sortedCandles).low(),
                first(sortedCandles) == null ? null : first(sortedCandles).close(),
                tradeCandles.stream().filter(this::validCandle).map(HistoricalCandle::high).max(BigDecimal::compareTo).orElse(null),
                tradeCandles.stream().filter(this::validCandle).map(HistoricalCandle::low).min(BigDecimal::compareTo).orElse(null),
                mfePoints, maePoints, realizedPoints
        );
        BigDecimal mfeDollars = dollars(mfePoints, pointValue, quantity);
        BigDecimal maeDollars = dollars(maePoints, pointValue, quantity);
        BigDecimal riskPerContract = riskPerContract(trade, entry, isShort);
        BigDecimal riskDollars = riskPerContract == null || quantity == null ? null : riskPerContract.multiply(pointValue).multiply(quantity);
        BigDecimal mfeR = divide(mfeDollars, riskDollars);
        BigDecimal maeR = divide(maeDollars, riskDollars);
        BigDecimal realizedR = trade.getPnlR() != null ? trade.getPnlR() : divide(realizedDollars, riskDollars);

        List<HistoricalCandle> postExit = validTradeInputs
                ? sortedCandles.stream()
                        .filter(this::validCandle)
                        .filter(c -> c.timestamp().isAfter(exitInstant))
                        .toList()
                : List.of();
        HistoricalCandle postBest = bestPostExit(postExit, entry, exit, isShort);
        BigDecimal additionalFavorable = postBest == null || exit == null ? null : maxZero(isShort ? exit.subtract(postBest.low()) : postBest.high().subtract(exit));
        BigDecimal capturePct = mfePoints == null || mfePoints.compareTo(BigDecimal.ZERO) <= 0 || realizedPoints == null
                ? null
                : maxZero(realizedPoints).multiply(HUNDRED).divide(mfePoints, 2, RoundingMode.HALF_UP);
        BigDecimal givebackPoints = profitable(realizedPoints) && mfePoints != null ? maxZero(mfePoints.subtract(realizedPoints)) : null;
        BigDecimal givebackPct = givebackPoints == null || mfePoints == null || mfePoints.compareTo(BigDecimal.ZERO) <= 0
                ? null
                : givebackPoints.multiply(HUNDRED).divide(mfePoints, 2, RoundingMode.HALF_UP);
        BigDecimal riskPoints = riskPerContract;
        BigDecimal additionalFavorableR = additionalFavorable == null || riskPoints == null ? null : divide(additionalFavorable, riskPoints);

        Map<String, Object> metrics = new LinkedHashMap<>();
        metrics.put("direction", isShort ? "SHORT" : isLong ? "LONG" : "UNKNOWN");
        metrics.put("mfePoints", round(mfePoints));
        metrics.put("maePoints", round(maePoints));
        metrics.put("mfeDollars", round(mfeDollars));
        metrics.put("maeDollars", round(maeDollars));
        metrics.put("mfeR", round(mfeR));
        metrics.put("maeR", round(maeR));
        metrics.put("realizedPoints", round(realizedPoints));
        metrics.put("realizedDollars", round(realizedDollars));
        metrics.put("realizedR", round(realizedR));
        metrics.put("riskSource", riskDollars == null ? null : (trade.getStopPrice() != null ? "STOP_PRICE" : "RISK_AMOUNT"));
        metrics.put("excursionCapturePct", round(capturePct));
        metrics.put("givebackPoints", round(givebackPoints));
        metrics.put("givebackPct", round(givebackPct));
        metrics.put("additionalFavorableAfterExitPoints", round(additionalFavorable));
        metrics.put("additionalFavorableAfterExitR", round(additionalFavorableR));
        metrics.put("holdingMinutes", trade.getEntryTime() == null || trade.getExitTime() == null ? null : Duration.between(trade.getEntryTime(), trade.getExitTime()).toMinutes());

        List<ScoredInsight> candidates = new ArrayList<>();
        addExecutionInsights(candidates, trade, exitInstant, exit, realizedPoints, mfePoints, maePoints, mfeDollars, maeDollars, mfeR, maeR, capturePct, givebackPoints, givebackPct);
        if (mfe != null) {
            candidates.add(scored(TradeReplayInsightType.MFE, mfe.timestamp(), isShort ? mfe.low() : mfe.high(), "INFO", "Maximum favorable excursion", "Best available price reached while the position was open.", metrics("FACT", "mfePoints", round(mfePoints), "mfeDollars", round(mfeDollars), "mfeR", round(mfeR)), 45));
        }
        if (mae != null) {
            candidates.add(scored(TradeReplayInsightType.MAE, mae.timestamp(), isShort ? mae.high() : mae.low(), "INFO", "Maximum adverse excursion", "Worst available price reached against the position while it was open.", metrics("RISK", "maePoints", round(maePoints), "maeDollars", round(maeDollars), "maeR", round(maeR)), 50));
        }
        addRiskEfficiencyInsight(candidates, mae, trade, mfePoints, maePoints);
        addPostExitInsight(candidates, postBest, isShort, exit, additionalFavorable, additionalFavorableR, mfePoints, realizedPoints);
        addTargetAndStopInsights(candidates, trade, sortedCandles, tradeCandles, postExit, entryInstant, exitInstant);

        List<TradeReplayAnalysis.InsightDraft> insights = ranked(candidates);

        String summary = summary(candles.size(), realizedPoints, mfePoints, maePoints, capturePct);
        String question = "What influenced this exit: market conditions, risk management, your plan, or emotion?";
        return new TradeReplayAnalysis(metrics, insights, summary, question);
    }

    private void addExecutionInsights(List<ScoredInsight> candidates, Trade trade, Instant exitInstant, BigDecimal exit, BigDecimal realizedPoints, BigDecimal mfePoints, BigDecimal maePoints, BigDecimal mfeDollars, BigDecimal maeDollars, BigDecimal mfeR, BigDecimal maeR, BigDecimal capturePct, BigDecimal givebackPoints, BigDecimal givebackPct) {
        if (mfePoints == null || mfePoints.compareTo(BigDecimal.ZERO) <= 0 || realizedPoints == null) return;
        Map<String, Object> captureMetrics = metrics("EXECUTION",
                "realizedPoints", round(realizedPoints),
                "mfePoints", round(mfePoints),
                "capturePct", round(capturePct),
                "givebackPoints", round(givebackPoints),
                "givebackPct", round(givebackPct),
                "mfeDollars", round(mfeDollars),
                "maeDollars", round(maeDollars),
                "mfeR", round(mfeR),
                "maeR", round(maeR));
        if (realizedPoints.compareTo(BigDecimal.ZERO) < 0 && mfePoints.compareTo(MEANINGFUL_FAVORABLE_POINTS) >= 0) {
            candidates.add(scored(
                    TradeReplayInsightType.WINNER_TURNED_LOSER,
                    exitInstant,
                    exit,
                    "WATCH",
                    "Favorable move was not converted",
                    "Price moved " + format(mfePoints) + " points in your favor before the trade finished " + format(realizedPoints.abs()) + " points negative.",
                    captureMetrics,
                    100
            ));
            return;
        }
        if (profitable(realizedPoints) && givebackPct != null && givebackPct.compareTo(LARGE_GIVEBACK_PCT) >= 0 && givebackPoints.compareTo(MEANINGFUL_FAVORABLE_POINTS) >= 0) {
            candidates.add(scored(
                    TradeReplayInsightType.LARGE_PROFIT_GIVEBACK,
                    exitInstant,
                    exit,
                    "WATCH",
                    "Significant open-profit giveback",
                    "The trade gave back " + format(givebackPoints) + " points, or " + format(givebackPct) + "% of peak favorable movement, before the final exit.",
                    captureMetrics,
                    95
            ));
            return;
        }
        if (capturePct == null || realizedPoints.compareTo(BigDecimal.ZERO) <= 0) return;
        if (capturePct.compareTo(STRONG_CAPTURE_THRESHOLD) >= 0) {
            candidates.add(scored(
                    TradeReplayInsightType.STRONG_EXCURSION_CAPTURE,
                    exitInstant,
                    exit,
                    "POSITIVE",
                    "Strong excursion capture",
                    "You captured " + format(realizedPoints) + " of " + format(mfePoints) + " favorable points, preserving " + format(capturePct) + "% of the available move.",
                    captureMetrics,
                    90
            ));
        } else if (capturePct.compareTo(GOOD_CAPTURE_THRESHOLD) >= 0) {
            candidates.add(scored(
                    TradeReplayInsightType.GOOD_EXCURSION_CAPTURE,
                    exitInstant,
                    exit,
                    "POSITIVE",
                    "Good excursion capture",
                    "You captured " + format(realizedPoints) + " of " + format(mfePoints) + " favorable points during the trade.",
                    captureMetrics,
                    82
            ));
        } else if (capturePct.compareTo(MODERATE_CAPTURE_THRESHOLD) >= 0) {
            candidates.add(scored(
                    TradeReplayInsightType.MODERATE_EXCURSION_CAPTURE,
                    exitInstant,
                    exit,
                    "INFO",
                    "Moderate excursion capture",
                    "The exit captured " + format(capturePct) + "% of the maximum favorable movement while the trade was open.",
                    captureMetrics,
                    72
            ));
        } else {
            candidates.add(scored(
                    TradeReplayInsightType.LOW_EXCURSION_CAPTURE,
                    exitInstant,
                    exit,
                    "WATCH",
                    "Low excursion capture",
                    "The exit captured " + format(capturePct) + "% of the maximum favorable movement. Compare that with the exit rule in your plan.",
                    captureMetrics,
                    84
            ));
        }
    }

    private void addRiskEfficiencyInsight(List<ScoredInsight> candidates, HistoricalCandle mae, Trade trade, BigDecimal mfePoints, BigDecimal maePoints) {
        if (mfePoints == null || maePoints == null || maePoints.compareTo(BigDecimal.ZERO) <= 0) return;
        BigDecimal ratio = mfePoints.divide(maePoints, 2, RoundingMode.HALF_UP);
        if (ratio.compareTo(new BigDecimal("2.00")) < 0 || mfePoints.compareTo(MEANINGFUL_FAVORABLE_POINTS) < 0) return;
        candidates.add(scored(
                TradeReplayInsightType.RISK_EFFICIENCY,
                mae == null ? null : mae.timestamp(),
                null,
                "POSITIVE",
                "Favorable excursion outweighed adverse movement",
                "Favorable excursion was " + format(ratio) + "x the maximum adverse excursion during the position.",
                Map.of("category", "RISK", "mfeMaeRatio", round(ratio), "mfePoints", round(mfePoints), "maePoints", round(maePoints)),
                65
        ));
    }

    private void addPostExitInsight(List<ScoredInsight> candidates, HistoricalCandle postBest, boolean isShort, BigDecimal exit, BigDecimal additionalFavorable, BigDecimal additionalFavorableR, BigDecimal mfePoints, BigDecimal realizedPoints) {
        if (postBest == null || additionalFavorable == null || additionalFavorable.compareTo(BigDecimal.ZERO) <= 0 || exit == null) return;
        BigDecimal postVsMfePct = mfePoints == null || mfePoints.compareTo(BigDecimal.ZERO) <= 0 ? null : additionalFavorable.multiply(HUNDRED).divide(mfePoints, 2, RoundingMode.HALF_UP);
        Map<String, Object> supporting = metrics("POST_EXIT", "additionalPoints", round(additionalFavorable), "additionalR", round(additionalFavorableR), "postVsMfePct", round(postVsMfePct));
        BigDecimal price = isShort ? postBest.low() : postBest.high();
        if (postVsMfePct != null && postVsMfePct.compareTo(new BigDecimal("25")) < 0) {
            candidates.add(scored(
                    TradeReplayInsightType.EXIT_NEAR_END_OF_MOVE,
                    postBest.timestamp(),
                    price,
                    "POSITIVE",
                    "Exit retained most of the move",
                    "After the final exit, price only continued favorably by " + format(additionalFavorable) + " points in the replay window.",
                    supporting,
                    profitable(realizedPoints) ? 70 : 55
            ));
        } else {
            candidates.add(scored(
                    TradeReplayInsightType.POST_EXIT_CONTINUATION,
                    postBest.timestamp(),
                    price,
                    postVsMfePct != null && postVsMfePct.compareTo(new BigDecimal("75")) >= 0 ? "IMPORTANT" : "INFO",
                    postVsMfePct != null && postVsMfePct.compareTo(new BigDecimal("75")) >= 0 ? "Large move continued after exit" : "Some opportunity remained after exit",
                    "Price continued " + format(additionalFavorable) + " favorable points after exit. Review whether the exit matched your plan; continuation alone does not make the exit wrong.",
                    supporting,
                    postVsMfePct != null && postVsMfePct.compareTo(new BigDecimal("75")) >= 0 ? 86 : 68
            ));
        }
    }

    private void addTargetAndStopInsights(List<ScoredInsight> candidates, Trade trade, List<HistoricalCandle> sortedCandles, List<HistoricalCandle> tradeCandles, List<HistoricalCandle> postExit, Instant entryInstant, Instant exitInstant) {
        if (trade.getTargetPrice() != null && postExit.stream().anyMatch(c -> touched(c, trade.getTargetPrice()))) {
            candidates.add(scored(TradeReplayInsightType.TARGET_REACHED_AFTER_EXIT, null, trade.getTargetPrice(), "INFO", "Recorded target reached after exit", "The recorded target price traded after the final exit. Use this as a process review point, not hindsight proof.", Map.of("category", "POST_EXIT", "targetPrice", trade.getTargetPrice()), 58));
        }
        if (trade.getStopPrice() == null) return;
        Optional<HistoricalCandle> during = tradeCandles.stream().filter(this::validCandle).filter(c -> touched(c, trade.getStopPrice())).findFirst();
        if (during.isPresent()) {
            candidates.add(scored(TradeReplayInsightType.STOP_LEVEL_TRADED_DURING_POSITION, during.get().timestamp(), trade.getStopPrice(), "IMPORTANT", "Stop level traded during the position", "Price touched the recorded stop level while the position was open. This is factual price contact, not an assumption about order handling.", Map.of("category", "RISK", "stopPrice", trade.getStopPrice(), "timing", "DURING_TRADE"), 62));
            return;
        }
        Optional<HistoricalCandle> after = sortedCandles.stream().filter(this::validCandle).filter(c -> exitInstant != null && c.timestamp().isAfter(exitInstant)).filter(c -> touched(c, trade.getStopPrice())).findFirst();
        if (after.isPresent()) {
            candidates.add(scored(TradeReplayInsightType.STOP_LEVEL_TRADED_AFTER_EXIT, after.get().timestamp(), trade.getStopPrice(), "INFO", "Stop level traded after exit", "Price reached the recorded stop level after you were already out, so it was not an active stop threat during the position.", Map.of("category", "FACT", "stopPrice", trade.getStopPrice(), "timing", "AFTER_EXIT"), 35));
            return;
        }
        Optional<HistoricalCandle> before = sortedCandles.stream().filter(this::validCandle).filter(c -> entryInstant != null && c.timestamp().isBefore(entryInstant)).filter(c -> touched(c, trade.getStopPrice())).findFirst();
        before.ifPresent(candle -> candidates.add(scored(TradeReplayInsightType.STOP_LEVEL_TRADED_BEFORE_ENTRY, candle.timestamp(), trade.getStopPrice(), "INFO", "Stop level traded before entry", "Price touched the recorded stop level before the position opened, so it is background context rather than execution risk.", Map.of("category", "FACT", "stopPrice", trade.getStopPrice(), "timing", "BEFORE_ENTRY"), 30)));
    }

    private List<TradeReplayAnalysis.InsightDraft> ranked(List<ScoredInsight> candidates) {
        List<ScoredInsight> ranked = candidates.stream()
                .filter(candidate -> candidate.draft() != null)
                .sorted(Comparator.comparingInt(ScoredInsight::score).reversed())
                .toList();
        List<TradeReplayAnalysis.InsightDraft> result = new ArrayList<>();
        int order = 1;
        for (ScoredInsight candidate : ranked) {
            TradeReplayAnalysis.InsightDraft draft = candidate.draft();
            result.add(new TradeReplayAnalysis.InsightDraft(draft.type(), draft.timestamp(), draft.price(), draft.severity(), draft.title(), draft.explanation(), draft.supportingMetrics(), order++));
        }
        return result;
    }

    private ScoredInsight scored(TradeReplayInsightType type, Instant timestamp, BigDecimal price, String severity, String title, String explanation, Map<String, Object> supportingMetrics, int score) {
        return new ScoredInsight(new TradeReplayAnalysis.InsightDraft(type, timestamp, price, severity, title, explanation, supportingMetrics, 0), score);
    }

    private Map<String, Object> metrics(String category, Object... pairs) {
        Map<String, Object> metrics = new LinkedHashMap<>();
        metrics.put("category", category);
        for (int i = 0; i + 1 < pairs.length; i += 2) {
            if (pairs[i] instanceof String key) {
                metrics.put(key, pairs[i + 1]);
            }
        }
        return metrics;
    }

    private String summary(int candleCount, BigDecimal realizedPoints, BigDecimal mfePoints, BigDecimal maePoints, BigDecimal capturePct) {
        if (realizedPoints != null && realizedPoints.compareTo(BigDecimal.ZERO) > 0 && capturePct != null) {
            return "Replay reconstructed " + candleCount + " one-minute candles. You captured " + format(realizedPoints) + " of " + format(mfePoints) + " favorable points (" + format(capturePct) + "%) while the trade moved " + format(maePoints) + " points against you at worst.";
        }
        if (realizedPoints != null && realizedPoints.compareTo(BigDecimal.ZERO) < 0 && mfePoints != null && mfePoints.compareTo(BigDecimal.ZERO) > 0) {
            return "Replay reconstructed " + candleCount + " one-minute candles. Price moved " + format(mfePoints) + " points in your favor before the trade finished " + format(realizedPoints.abs()) + " points negative.";
        }
        return "Replay reconstructed " + candleCount + " one-minute candles. MFE was " + format(mfePoints) + " points and MAE was " + format(maePoints) + " points.";
    }

    private HistoricalCandle bestPostExit(List<HistoricalCandle> candles, BigDecimal entry, BigDecimal exit, boolean isShort) {
        return candles.stream().max(Comparator.comparing(c -> isShort ? exit.subtract(c.low()) : c.high().subtract(exit))).orElse(null);
    }

    private HistoricalCandle first(List<HistoricalCandle> candles) {
        return candles == null || candles.isEmpty() ? null : candles.get(0);
    }

    private boolean touched(HistoricalCandle candle, BigDecimal price) {
        return candle.low().compareTo(price) <= 0 && candle.high().compareTo(price) >= 0;
    }

    private boolean profitable(BigDecimal realizedPoints) {
        return realizedPoints != null && realizedPoints.compareTo(BigDecimal.ZERO) > 0;
    }

    private BigDecimal riskPerContract(Trade trade, BigDecimal entry, boolean isShort) {
        if (entry != null && trade.getStopPrice() != null) {
            BigDecimal risk = isShort ? trade.getStopPrice().subtract(entry) : entry.subtract(trade.getStopPrice());
            return risk.compareTo(BigDecimal.ZERO) > 0 ? risk : null;
        }
        if (trade.getRiskAmount() != null && trade.getRiskAmount().compareTo(BigDecimal.ZERO) > 0) {
            BigDecimal pointValue = pointValue(trade.getSymbol());
            BigDecimal qty = validQuantity(trade);
            if (qty == null) return null;
            return trade.getRiskAmount().divide(pointValue.multiply(qty), 6, RoundingMode.HALF_UP);
        }
        return null;
    }

    private BigDecimal validQuantity(Trade trade) {
        if (trade.getQuantity() == null) return BigDecimal.ONE;
        return trade.getQuantity() > 0 ? new BigDecimal(trade.getQuantity()) : null;
    }

    private boolean positive(BigDecimal value) {
        return value != null && value.compareTo(BigDecimal.ZERO) > 0;
    }

    private boolean validCandle(HistoricalCandle candle) {
        return candle != null
                && candle.timestamp() != null
                && positive(candle.open())
                && positive(candle.high())
                && positive(candle.low())
                && positive(candle.close());
    }

    private BigDecimal pointValue(String symbol) {
        String root = symbol == null ? "" : symbol.toUpperCase(Locale.ROOT);
        if (root.startsWith("MES")) return new BigDecimal("5");
        if (root.startsWith("MNQ")) return new BigDecimal("2");
        if (root.startsWith("NQ")) return new BigDecimal("20");
        return new BigDecimal("50");
    }

    private BigDecimal dollars(BigDecimal points, BigDecimal pointValue, BigDecimal quantity) {
        return points == null ? null : points.multiply(pointValue).multiply(quantity);
    }

    private BigDecimal divide(BigDecimal numerator, BigDecimal denominator) {
        if (numerator == null || denominator == null || denominator.compareTo(BigDecimal.ZERO) <= 0) return null;
        return numerator.divide(denominator, 4, RoundingMode.HALF_UP);
    }

    private BigDecimal maxZero(BigDecimal value) {
        return value == null || value.compareTo(BigDecimal.ZERO) < 0 ? BigDecimal.ZERO : value;
    }

    private BigDecimal round(BigDecimal value) {
        return value == null ? null : value.setScale(2, RoundingMode.HALF_UP);
    }

    private String format(BigDecimal value) {
        return value == null ? "unknown" : round(value).stripTrailingZeros().toPlainString();
    }

    private record ScoredInsight(TradeReplayAnalysis.InsightDraft draft, int score) {}
}
