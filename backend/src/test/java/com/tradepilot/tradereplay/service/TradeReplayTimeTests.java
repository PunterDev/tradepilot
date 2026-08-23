package com.tradepilot.tradereplay.service;

import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;

class TradeReplayTimeTests {
    @Test
    void convertsSummerTradeWallClockTimeToUtcUsingChicagoDst() {
        Instant instant = TradeReplayTime.toInstant(LocalDateTime.of(2026, 6, 8, 5, 25));

        assertThat(instant).isEqualTo(Instant.parse("2026-06-08T10:25:00Z"));
    }

    @Test
    void convertsWinterTradeWallClockTimeToUtcUsingChicagoStandardTime() {
        Instant instant = TradeReplayTime.toInstant(LocalDateTime.of(2026, 1, 8, 5, 25));

        assertThat(instant).isEqualTo(Instant.parse("2026-01-08T11:25:00Z"));
    }
}
