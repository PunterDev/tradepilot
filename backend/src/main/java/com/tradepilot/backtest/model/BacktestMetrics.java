package com.tradepilot.backtest.model;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
public record BacktestMetrics(int totalTrades, int wins, int losses, int breakEvens, BigDecimal winRate, BigDecimal winRateExcludingBe,
                              BigDecimal totalRawR, BigDecimal weightedTotalR, BigDecimal averageR, BigDecimal expectancy,
                              BigDecimal profitFactor, BigDecimal maxDrawdownR, int maxConsecutiveLosses, BigDecimal averageWinner,
                              BigDecimal averageLoser, List<Point> equityCurve) {
    public record Point(int tradeIndex, Instant entryTime, LocalDate tradeDate, BigDecimal tradeR, BigDecimal weightedTradeR,
                        BigDecimal cumulativeRawR, BigDecimal cumulativeWeightedR) { }
}
