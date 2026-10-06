package com.tradepilot.marketdata.repository;

import com.tradepilot.marketdata.entity.MarketBar;
import com.tradepilot.marketdata.model.MarketInstrument;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.Instant;
import java.util.List;

public interface MarketBarRepository extends JpaRepository<MarketBar, Long> {
    List<MarketBar> findBySymbolAndTimeframeAndTsEventGreaterThanEqualAndTsEventLessThanOrderByTsEventAsc(
            MarketInstrument symbol, String timeframe, Instant startInclusive, Instant endExclusive);

    List<MarketBar> findBySymbolAndInstrumentIdAndTimeframeAndTsEventGreaterThanEqualAndTsEventLessThanOrderByTsEventAsc(
            MarketInstrument symbol, Long instrumentId, String timeframe, Instant startInclusive, Instant endExclusive);
}
