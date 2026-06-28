package com.tradepilot;

import com.tradepilot.coach.dto.CoachAiResponse;
import com.tradepilot.coach.dto.CoachDeterministicSummary;
import com.tradepilot.coach.entity.CoachSnapshotType;
import com.tradepilot.coach.service.CoachOutputConsistencyService;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class CoachOutputConsistencyServiceTests {

    private final CoachOutputConsistencyService service = new CoachOutputConsistencyService();

    @Test
    void patchesLongFailureLanguageWhenLongDayWasProfitable() {
        CoachAiResponse response = response(
                "The LONG idea failed. Avoid taking this long direction again.",
                "The LONG idea failed because confirmation was weak."
        );

        CoachAiResponse patched = service.enforce(
                CoachSnapshotType.DAILY,
                summary("LONG", new BigDecimal("162.50"), 1, 0),
                response,
                1L,
                6L,
                LocalDate.of(2026, 6, 23)
        );

        assertThat(patched.getNextSessionFocus())
                .contains("The LONG trade was profitable")
                .doesNotContain("LONG idea failed");
        assertThat(patched.getAvoidNextSession())
                .contains("not an overtrading day")
                .doesNotContain("LONG idea failed");
    }

    @Test
    void patchesShortFailureLanguageWhenShortDayWasProfitable() {
        CoachAiResponse response = response(
                "The SHORT idea failed. Avoid short setups.",
                "The short failed because the entry was late."
        );

        CoachAiResponse patched = service.enforce(
                CoachSnapshotType.DAILY,
                summary("SHORT", new BigDecimal("162.50"), 0, 1),
                response,
                1L,
                6L,
                LocalDate.of(2026, 6, 24)
        );

        assertThat(patched.getNextSessionFocus())
                .contains("The SHORT trade was profitable")
                .doesNotContain("SHORT idea failed");
        assertThat(patched.getAvoidNextSession())
                .contains("not an overtrading day")
                .doesNotContain("short failed");
    }

    @Test
    void doesNotPatchFailedLongLanguageWhenLongDayWasNegative() {
        CoachAiResponse response = response(
                "The LONG idea failed. Review confirmation.",
                "The LONG idea failed because the entry was late."
        );

        CoachAiResponse patched = service.enforce(
                CoachSnapshotType.DAILY,
                summary("LONG", new BigDecimal("-76.25"), 1, 0),
                response,
                1L,
                6L,
                LocalDate.of(2026, 6, 22)
        );

        assertThat(patched.getNextSessionFocus()).isEqualTo("The LONG idea failed because the entry was late.");
        assertThat(patched.getAvoidNextSession()).isEqualTo("The LONG idea failed. Review confirmation.");
    }

    @Test
    void doesNotOvercorrectMixedDirectionProfitableDays() {
        CoachAiResponse response = response(
                "Review the mixed direction quality.",
                "Direction review is mixed because both long and short trades were taken."
        );

        CoachAiResponse patched = service.enforce(
                CoachSnapshotType.DAILY,
                summary("MIXED", new BigDecimal("162.50"), 1, 1),
                response,
                1L,
                6L,
                LocalDate.of(2026, 6, 25)
        );

        assertThat(patched.getNextSessionFocus()).isEqualTo("Direction review is mixed because both long and short trades were taken.");
        assertThat(patched.getAvoidNextSession()).isEqualTo("Review the mixed direction quality.");
    }

    private CoachAiResponse response(String avoidNextSession, String nextSessionFocus) {
        CoachAiResponse response = new CoachAiResponse();
        response.setAvoidNextSession(avoidNextSession);
        response.setNextSessionFocus(nextSessionFocus);
        return response;
    }

    private CoachDeterministicSummary summary(String side, BigDecimal netPnl, int longTrades, int shortTrades) {
        BigDecimal longPnl = longTrades > 0 ? netPnl : BigDecimal.ZERO;
        BigDecimal shortPnl = shortTrades > 0 ? netPnl : BigDecimal.ZERO;
        if ("MIXED".equals(side)) {
            longPnl = new BigDecimal("100.00");
            shortPnl = new BigDecimal("62.50");
        }
        return CoachDeterministicSummary.of(
                LocalDate.of(2026, 6, 23),
                longTrades + shortTrades,
                netPnl,
                BigDecimal.valueOf(100),
                BigDecimal.ZERO,
                longTrades,
                shortTrades,
                0,
                netPnl.compareTo(BigDecimal.ZERO) > 0 ? 1 : 0,
                netPnl.compareTo(BigDecimal.ZERO) < 0 ? 1 : 0,
                side,
                List.of("MESU6"),
                longPnl,
                shortPnl,
                side,
                "One trade, $" + netPnl + " net.",
                "Review process quality.",
                "The " + side + " trade was profitable.",
                "Limited Data",
                "Limited confidence: only one trade was taken.",
                "Define thesis, invalidation, and target.",
                "Not enough data",
                "Not enough data",
                "Not enough data",
                "Not enough data",
                "No repeated mistake detected",
                "Setup Analysis: no metadata available.",
                "Emotion Analysis: no metadata available.",
                "Confidence Analysis: no metadata available.",
                List.of(),
                BigDecimal.ZERO,
                BigDecimal.ZERO,
                BigDecimal.ZERO,
                List.of(),
                List.of(),
                List.of()
        );
    }
}
