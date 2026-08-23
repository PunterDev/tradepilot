package com.tradepilot.importing;

import com.tradepilot.enums.SessionType;

import java.time.LocalTime;
import java.time.ZoneId;
import java.util.List;

public final class TradingSessionPolicy {
    public static final ZoneId MARKET_ZONE = ZoneId.of("America/New_York");

    public static final LocalTime ASIA_START = LocalTime.of(18, 0);
    public static final LocalTime ASIA_END = LocalTime.of(3, 0);
    public static final LocalTime LONDON_START = LocalTime.of(3, 0);
    public static final LocalTime LONDON_END = LocalTime.of(9, 30);
    public static final LocalTime NY_AM_START = LocalTime.of(9, 30);
    public static final LocalTime NY_AM_END = LocalTime.of(12, 30);
    public static final LocalTime NY_PM_START = LocalTime.of(12, 30);
    public static final LocalTime NY_PM_END = LocalTime.of(16, 0);

    static final List<Window> WINDOWS = List.of(
            new Window(SessionType.ASIA, ASIA_START, ASIA_END),
            new Window(SessionType.LONDON, LONDON_START, LONDON_END),
            new Window(SessionType.NY_AM, NY_AM_START, NY_AM_END),
            new Window(SessionType.NY_PM, NY_PM_START, NY_PM_END)
    );

    private TradingSessionPolicy() {
    }

    static final class Window {
        private final SessionType session;
        private final LocalTime startInclusive;
        private final LocalTime endExclusive;

        Window(SessionType session, LocalTime startInclusive, LocalTime endExclusive) {
            this.session = session;
            this.startInclusive = startInclusive;
            this.endExclusive = endExclusive;
        }

        SessionType session() {
            return session;
        }

        boolean contains(LocalTime time) {
            if (startInclusive.equals(endExclusive)) {
                return true;
            }
            if (startInclusive.isBefore(endExclusive)) {
                return !time.isBefore(startInclusive) && time.isBefore(endExclusive);
            }
            return !time.isBefore(startInclusive) || time.isBefore(endExclusive);
        }
    }
}
