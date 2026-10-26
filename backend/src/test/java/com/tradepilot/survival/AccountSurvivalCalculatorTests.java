package com.tradepilot.survival;

import static org.junit.jupiter.api.Assertions.*;
import java.math.BigDecimal;
import java.util.List;
import org.junit.jupiter.api.Test;

class AccountSurvivalCalculatorTests {
    @Test void calculatesMedianLossAndCapacityWithoutUsingFloatingPoint() {
        BigDecimal median = AccountSurvivalCalculator.medianLoss(List.of(new BigDecimal("100"), new BigDecimal("200"), new BigDecimal("300"), new BigDecimal("400"), new BigDecimal("500")));
        assertEquals(0, median.compareTo(new BigDecimal("300")));
        assertEquals(0, AccountSurvivalCalculator.lossCapacity(new BigDecimal("1500"), median).compareTo(new BigDecimal("5.00")));
    }
    @Test void requiresEnoughObservedLossesRatherThanSubstitutingPlannedRisk() {
        assertNull(AccountSurvivalCalculator.medianLoss(List.of(BigDecimal.ONE, BigDecimal.TEN)));
    }
    @Test void appliesMostSevereSurvivalBoundary() {
        assertEquals(AccountSurvivalState.STABLE, AccountSurvivalCalculator.state(new BigDecimal("1000"), new BigDecimal("10"), new BigDecimal("8")));
        assertEquals(AccountSurvivalState.WATCH, AccountSurvivalCalculator.state(new BigDecimal("1000"), new BigDecimal("11"), new BigDecimal("9")));
        assertEquals(AccountSurvivalState.FRAGILE, AccountSurvivalCalculator.state(new BigDecimal("1000"), new BigDecimal("16"), new BigDecimal("6")));
        assertEquals(AccountSurvivalState.CRITICAL, AccountSurvivalCalculator.state(new BigDecimal("1000"), new BigDecimal("10"), new BigDecimal("2")));
        assertEquals(AccountSurvivalState.CRITICAL, AccountSurvivalCalculator.state(BigDecimal.ZERO, new BigDecimal("1"), new BigDecimal("10")));
    }

    @Test void preservesExactStateBoundaries() {
        assertEquals(AccountSurvivalState.STABLE, AccountSurvivalCalculator.state(new BigDecimal("1"), new BigDecimal("10"), new BigDecimal("8")));
        assertEquals(AccountSurvivalState.WATCH, AccountSurvivalCalculator.state(new BigDecimal("1"), new BigDecimal("15"), new BigDecimal("5")));
        assertEquals(AccountSurvivalState.FRAGILE, AccountSurvivalCalculator.state(new BigDecimal("1"), new BigDecimal("25"), new BigDecimal("3")));
        assertEquals(AccountSurvivalState.CRITICAL, AccountSurvivalCalculator.state(new BigDecimal("1"), new BigDecimal("25.0001"), new BigDecimal("3")));
    }
}
