package com.tradepilot.tradereplay.repository;

import com.tradepilot.tradereplay.entity.MarketCandle;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.Instant;
import java.util.List;

public interface MarketCandleRepository extends JpaRepository<MarketCandle, Long> {
    List<MarketCandle> findByProviderAndContractSymbolAndIntervalAndTimestampGreaterThanEqualAndTimestampLessThanOrderByTimestampAsc(
            String provider,
            String contractSymbol,
            String interval,
            Instant start,
            Instant end
    );

    long countByProviderAndContractSymbolAndIntervalAndTimestampGreaterThanEqualAndTimestampLessThan(
            String provider,
            String contractSymbol,
            String interval,
            Instant start,
            Instant end
    );
}
