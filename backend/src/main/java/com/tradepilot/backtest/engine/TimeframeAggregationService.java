package com.tradepilot.backtest.engine;

import com.tradepilot.backtest.model.BacktestBar;
import java.math.BigDecimal;
import java.time.*;
import java.time.temporal.ChronoUnit;
import java.util.*;

/** Aggregates only completed bars, aligned to America/New_York session clock. */
public class TimeframeAggregationService {
    public List<BacktestBar> aggregate(List<BacktestBar> bars, Duration size) {
        Map<Instant, List<BacktestBar>> buckets = new LinkedHashMap<>();
        for (BacktestBar bar : bars) {
            Instant bucket = bucket(bar.time(), size);
            buckets.computeIfAbsent(bucket, ignored -> new ArrayList<>()).add(bar);
        }
        List<BacktestBar> result = new ArrayList<>();
        for (var entry : buckets.entrySet()) {
            List<BacktestBar> group = entry.getValue();
            if (group.size() < size.toMinutes()) continue;
            BacktestBar first = group.get(0), last = group.get(group.size() - 1);
            if (group.stream().anyMatch(bar -> bar.instrumentId() != first.instrumentId())) continue;
            BigDecimal high = group.stream().map(BacktestBar::high).max(BigDecimal::compareTo).orElse(first.high());
            BigDecimal low = group.stream().map(BacktestBar::low).min(BigDecimal::compareTo).orElse(first.low());
            long volume = group.stream().mapToLong(BacktestBar::volume).sum();
            result.add(new BacktestBar(entry.getKey(), first.instrumentId(), first.open(), high, low, last.close(), volume));
        }
        return result;
    }
    private Instant bucket(Instant instant, Duration size) {
        ZonedDateTime ny = instant.atZone(ZoneId.of("America/New_York"));
        long minutes = size.toMinutes();
        int minute = (ny.getHour() * 60 + ny.getMinute()) / (int) minutes * (int) minutes;
        return ny.toLocalDate().atStartOfDay(ny.getZone()).plusMinutes(minute).toInstant();
    }
}
