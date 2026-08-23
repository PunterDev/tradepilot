package com.tradepilot.tradereplay.service;

import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;

class FuturesContractCalendarTests {
    private final FuturesContractSymbolNormalizer normalizer = new FuturesContractSymbolNormalizer();
    private final FuturesContractCalendar calendar = new FuturesContractCalendar(normalizer);

    @Test
    void resolvesExpectedQuarterlyContractsAround2026Expirations() {
        assertThat(calendar.expectedActiveContract("MES", LocalDate.of(2026, 3, 10))).contains("MESH6");
        assertThat(calendar.expectedActiveContract("MES", LocalDate.of(2026, 3, 23))).contains("MESM6");
        assertThat(calendar.expectedActiveContract("MES", LocalDate.of(2026, 6, 10))).contains("MESM6");
        assertThat(calendar.expectedActiveContract("MES", LocalDate.of(2026, 6, 22))).contains("MESU6");
        assertThat(calendar.expectedActiveContract("MES", LocalDate.of(2026, 9, 21))).contains("MESZ6");
        assertThat(calendar.expectedActiveContract("MES", LocalDate.of(2026, 12, 21))).contains("MESH7");
    }

    @Test
    void handlesDecemberToMarchYearTransition() {
        assertThat(calendar.expectedActiveContract("MES", LocalDate.of(2026, 12, 21))).contains("MESH7");
        assertThat(calendar.expectedActiveContract("MNQ", LocalDate.of(2026, 12, 21))).contains("MNQH7");
    }

    @Test
    void parsesOneDigitAndTwoDigitContractYearsThroughNormalizer() {
        LocalDateTime executionTime = LocalDateTime.of(2026, 6, 22, 14, 33);

        assertThat(calendar.parse("MNQM6", executionTime)).get()
                .extracting(FuturesContractCalendar.ParsedContract::normalizedSymbol)
                .isEqualTo("MNQM6");
        assertThat(calendar.parse("MNQM26", executionTime)).get()
                .extracting(FuturesContractCalendar.ParsedContract::normalizedSymbol)
                .isEqualTo("MNQM6");
        assertThat(calendar.parse("MNQ 06-26", executionTime)).get()
                .extracting(FuturesContractCalendar.ParsedContract::normalizedSymbol)
                .isEqualTo("MNQM6");
    }

    @Test
    void separatesExpiredValidityFromExpectedActiveContract() {
        LocalDateTime executionTime = LocalDateTime.of(2026, 6, 22, 14, 33);

        assertThat(calendar.contractValidForDate("MESM6", LocalDate.of(2026, 6, 22), executionTime)).isFalse();
        assertThat(calendar.expectedActiveContract("MES", LocalDate.of(2026, 6, 22))).contains("MESU6");
    }
}
