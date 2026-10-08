package com.tradepilot.backtest.model;
import java.math.BigDecimal;
import java.time.Instant;
public record BacktestBar(Instant time, long instrumentId, BigDecimal open, BigDecimal high, BigDecimal low, BigDecimal close, long volume) { }
