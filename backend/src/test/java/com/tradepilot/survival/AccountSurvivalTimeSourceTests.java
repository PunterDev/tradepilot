package com.tradepilot.survival;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.time.LocalDate;
import java.time.ZoneId;
import org.junit.jupiter.api.Test;
import org.springframework.mock.env.MockEnvironment;

class AccountSurvivalTimeSourceTests {
    @Test
    void e2eClockStartsAtFixtureDateAndOnlyMovesForward() {
        MockEnvironment environment = new MockEnvironment();
        environment.setActiveProfiles("e2e");
        AccountSurvivalTimeSource timeSource = new AccountSurvivalTimeSource(environment);

        timeSource.resetE2eDate(LocalDate.of(2026, 10, 1));
        assertEquals(LocalDate.of(2026, 10, 1), timeSource.today(ZoneId.systemDefault()));

        timeSource.advanceE2eDate(LocalDate.of(2026, 10, 3));
        assertEquals(LocalDate.of(2026, 10, 3), timeSource.today(ZoneId.systemDefault()));
        assertThrows(IllegalArgumentException.class, () -> timeSource.advanceE2eDate(LocalDate.of(2026, 10, 3)));
    }

    @Test
    void testClockCannotBeUsedOutsideE2eProfile() {
        AccountSurvivalTimeSource timeSource = new AccountSurvivalTimeSource(new MockEnvironment());

        assertThrows(IllegalStateException.class, () -> timeSource.resetE2eDate(LocalDate.of(2026, 10, 1)));
        assertThrows(IllegalStateException.class, () -> timeSource.advanceE2eDate(LocalDate.of(2026, 10, 2)));
    }
}
