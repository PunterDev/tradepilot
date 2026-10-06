package com.tradepilot.backtest.engine;

import com.tradepilot.backtest.model.BacktestBar;
import java.time.Duration;
import java.time.Instant;

/** Databento OHLCV timestamps mark inclusive interval starts; OHLC is usable only at interval end. */
public final class BarTiming {
    public static final Duration ONE_MINUTE = Duration.ofMinutes(1);

    private BarTiming() { }

    public static Instant availableAt(BacktestBar oneMinuteBar) {
        return availableAt(oneMinuteBar.time(), ONE_MINUTE);
    }

    public static Instant availableAt(Instant barStart, Duration interval) {
        return barStart.plus(interval);
    }

    public static boolean availableBy(Instant availableAt, Instant decisionTime) {
        return !availableAt.isAfter(decisionTime);
    }
}
