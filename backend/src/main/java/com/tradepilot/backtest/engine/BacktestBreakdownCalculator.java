package com.tradepilot.backtest.engine;

import com.tradepilot.backtest.model.*;
import java.time.*;
import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

/** Produces the same metrics contract for each deterministic reporting bucket. */
public class BacktestBreakdownCalculator {
    private static final ZoneId NY = ZoneId.of("America/New_York");
    public Map<String, Map<String, BacktestMetrics>> calculate(List<BacktestTradeResult> modelA, List<BacktestTradeResult> modelB) {
        return Map.of("weekday", paired(modelA, modelB, trade -> trade.entryTime().atZone(NY).getDayOfWeek().name()),
                "direction", paired(modelA, modelB, trade -> trade.direction().name()),
                "entryTime", paired(modelA, modelB, trade -> timeBucket(trade.entryTime())),
                "month", paired(modelA, modelB, trade -> trade.tradeDate().getYear() + "-" + String.format("%02d", trade.tradeDate().getMonthValue())));
    }
    private Map<String, BacktestMetrics> paired(List<BacktestTradeResult> a, List<BacktestTradeResult> b, Function<BacktestTradeResult,String> key) {
        Map<String, BacktestMetrics> output = new TreeMap<>();
        grouped(a,key).forEach((bucket, trades) -> output.put(bucket + "|MODEL_A", new BacktestMetricsCalculator().calculate(trades)));
        grouped(b,key).forEach((bucket, trades) -> output.put(bucket + "|MODEL_B", new BacktestMetricsCalculator().calculate(trades)));
        return output;
    }
    private Map<String,List<BacktestTradeResult>> grouped(List<BacktestTradeResult> trades, Function<BacktestTradeResult,String> key) { return trades.stream().collect(Collectors.groupingBy(key, TreeMap::new, Collectors.toList())); }
    private String timeBucket(Instant entry) { LocalTime time=entry.atZone(NY).toLocalTime(); int minute=(time.getHour()*60+time.getMinute()); int start=minute-(minute%15); return String.format("%02d:%02d-%02d:%02d",start/60,start%60,(start+14)/60,(start+14)%60); }
}
