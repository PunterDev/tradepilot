package com.tradepilot.backtest.model;
import java.math.BigDecimal;
import java.time.Instant;
public record PriceZone(Direction direction, BigDecimal low, BigDecimal high, Instant confirmedAt, long instrumentId, boolean inversion) {
    public enum Direction { BULLISH, BEARISH }
    public boolean contains(BigDecimal price) { return price.compareTo(low) >= 0 && price.compareTo(high) <= 0; }
    public BigDecimal midpoint() { return low.add(high).divide(BigDecimal.valueOf(2)); }
}
