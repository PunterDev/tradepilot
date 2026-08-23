package com.tradepilot.tradereplay.service;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;

final class TradeReplayTime {
    static final ZoneId TRADE_TIME_ZONE = ZoneId.of("America/Chicago");

    private TradeReplayTime() {}

    static Instant toInstant(LocalDateTime tradeTime) {
        return tradeTime == null ? null : tradeTime.atZone(TRADE_TIME_ZONE).toInstant();
    }
}
