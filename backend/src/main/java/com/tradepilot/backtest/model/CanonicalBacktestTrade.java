package com.tradepilot.backtest.model;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.Map;

/** One setup and entry, evaluated through both documented exit models. */
public record CanonicalBacktestTrade(
        LocalDate tradeDate,
        BacktestTradeResult.Direction direction,
        Instant setupTime,
        Instant entryTime,
        BigDecimal entryPrice,
        BigDecimal stopPrice,
        BigDecimal tp1Price,
        BigDecimal runnerTargetPrice,
        long instrumentId,
        boolean ambiguousIntrabar,
        BacktestTradeResult modelA,
        BacktestTradeResult modelB,
        Map<String, Object> metadata) {
}
