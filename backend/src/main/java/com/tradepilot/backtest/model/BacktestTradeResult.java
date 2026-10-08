package com.tradepilot.backtest.model;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.Map;
public record BacktestTradeResult(LocalDate tradeDate, Direction direction, Instant entryTime, BigDecimal entryPrice, BigDecimal stopPrice,
                                  BigDecimal tp1Price, BigDecimal runnerTargetPrice, Instant exitTime, BigDecimal exitPrice,
                                  BigDecimal rawR, BigDecimal weightedR, Outcome outcome, ExitModel exitModel, long instrumentId,
                                  Map<String, Object> metadata) {
    public enum Direction { LONG, SHORT }
    public enum Outcome { WIN, LOSS, BREAK_EVEN }
    public enum ExitModel { MODEL_A, MODEL_B }
}
