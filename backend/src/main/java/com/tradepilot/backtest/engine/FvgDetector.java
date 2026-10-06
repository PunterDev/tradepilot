package com.tradepilot.backtest.engine;
import com.tradepilot.backtest.model.*;
import java.math.BigDecimal;
import java.util.Optional;
public class FvgDetector {
    public Optional<PriceZone> detect(BacktestBar first, BacktestBar middle, BacktestBar third, BigDecimal minimumSize) {
        if (first.instrumentId() != middle.instrumentId() || middle.instrumentId() != third.instrumentId()) return Optional.empty();
        if (third.low().subtract(first.high()).compareTo(minimumSize) >= 0)
            return Optional.of(new PriceZone(PriceZone.Direction.BULLISH, first.high(), third.low(), third.time(), third.instrumentId(), false));
        if (first.low().subtract(third.high()).compareTo(minimumSize) >= 0)
            return Optional.of(new PriceZone(PriceZone.Direction.BEARISH, third.high(), first.low(), third.time(), third.instrumentId(), false));
        return Optional.empty();
    }
    public Optional<PriceZone> invert(PriceZone zone, BacktestBar bar, BigDecimal penetration) {
        if (bar.instrumentId() != zone.instrumentId()) return Optional.empty();
        if (zone.direction() == PriceZone.Direction.BULLISH && bar.close().compareTo(zone.low().subtract(penetration)) < 0)
            return Optional.of(new PriceZone(PriceZone.Direction.BEARISH, zone.low(), zone.high(), bar.time(), bar.instrumentId(), true));
        if (zone.direction() == PriceZone.Direction.BEARISH && bar.close().compareTo(zone.high().add(penetration)) > 0)
            return Optional.of(new PriceZone(PriceZone.Direction.BULLISH, zone.low(), zone.high(), bar.time(), bar.instrumentId(), true));
        return Optional.empty();
    }
}
