package com.tradepilot.importing;

import com.tradepilot.enums.SessionType;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;

@Component
public class TradingSessionClassifier {
    public SessionType classify(Instant executionInstant) {
        if (executionInstant == null) {
            return SessionType.UNKNOWN;
        }
        ZonedDateTime marketTime = executionInstant.atZone(TradingSessionPolicy.MARKET_ZONE);
        return classifyMarketTime(marketTime.toLocalTime());
    }

    public SessionType classify(OffsetDateTime executionTime) {
        if (executionTime == null) {
            return SessionType.UNKNOWN;
        }
        return classify(executionTime.toInstant());
    }

    public SessionType classifyMarketLocalTimestamp(LocalDateTime marketLocalTime) {
        if (marketLocalTime == null) {
            return SessionType.UNKNOWN;
        }
        return classifyMarketTime(marketLocalTime.toLocalTime());
    }

    public SessionType classifySourceLocalTimestamp(LocalDateTime sourceLocalTime, ZoneId sourceZone) {
        if (sourceLocalTime == null || sourceZone == null) {
            return SessionType.UNKNOWN;
        }
        return classify(sourceLocalTime.atZone(sourceZone).toInstant());
    }

    private SessionType classifyMarketTime(LocalTime time) {
        if (time == null) {
            return SessionType.UNKNOWN;
        }
        for (TradingSessionPolicy.Window window : TradingSessionPolicy.WINDOWS) {
            if (window.contains(time)) {
                return window.session();
            }
        }
        return SessionType.UNKNOWN;
    }
}
