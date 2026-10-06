package com.tradepilot.manualbacktest.service;

import com.tradepilot.backtest.model.BacktestBar;
import java.math.BigDecimal;
import java.time.*;
import java.util.*;

/** Replay aggregation deliberately includes a final, visibly incomplete live bucket. */
public class ReplayAggregationService {
    public record ReplayBar(BacktestBar bar, boolean complete) { }
    public List<ReplayBar> aggregate(List<BacktestBar> bars, Duration size) {
        Map<Instant, List<BacktestBar>> buckets = new LinkedHashMap<>();
        for (BacktestBar bar : bars) buckets.computeIfAbsent(bucket(bar.time(), size), ignored -> new ArrayList<>()).add(bar);
        List<ReplayBar> output = new ArrayList<>();
        for (var entry : buckets.entrySet()) {
            List<BacktestBar> group = entry.getValue(); BacktestBar first = group.get(0), last = group.get(group.size() - 1);
            if (group.stream().anyMatch(bar -> bar.instrumentId() != first.instrumentId())) continue;
            BigDecimal high = group.stream().map(BacktestBar::high).max(BigDecimal::compareTo).orElse(first.high());
            BigDecimal low = group.stream().map(BacktestBar::low).min(BigDecimal::compareTo).orElse(first.low());
            long volume = group.stream().mapToLong(BacktestBar::volume).sum();
            output.add(new ReplayBar(new BacktestBar(entry.getKey(), first.instrumentId(), first.open(), high, low, last.close(), volume), group.size() == size.toMinutes()));
        }
        return output;
    }
    private Instant bucket(Instant instant, Duration size) {
        ZonedDateTime ny = instant.atZone(ZoneId.of("America/New_York")); int minutes = Math.toIntExact(size.toMinutes());
        int minute = (ny.getHour() * 60 + ny.getMinute()) / minutes * minutes;
        return ny.toLocalDate().atStartOfDay(ny.getZone()).plusMinutes(minute).toInstant();
    }
}
