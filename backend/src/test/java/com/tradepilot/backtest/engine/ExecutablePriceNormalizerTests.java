package com.tradepilot.backtest.engine;

import com.tradepilot.backtest.model.BacktestTradeResult;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;

class ExecutablePriceNormalizerTests {
    private static final BigDecimal TICK = new BigDecimal("0.25");

    @Test
    void roundsLongEntriesUpToTheNextExecutableTick() {
        assertThat(ExecutablePriceNormalizer.roundEntryToExecutableTick(BacktestTradeResult.Direction.LONG, new BigDecimal("100.125"), TICK))
                .isEqualByComparingTo("100.25");
        assertThat(ExecutablePriceNormalizer.roundEntryToExecutableTick(BacktestTradeResult.Direction.LONG, new BigDecimal("100.375"), TICK))
                .isEqualByComparingTo("100.50");
    }

    @Test
    void roundsShortEntriesDownToTheNextExecutableTick() {
        assertThat(ExecutablePriceNormalizer.roundEntryToExecutableTick(BacktestTradeResult.Direction.SHORT, new BigDecimal("100.125"), TICK))
                .isEqualByComparingTo("100.00");
        assertThat(ExecutablePriceNormalizer.roundEntryToExecutableTick(BacktestTradeResult.Direction.SHORT, new BigDecimal("100.375"), TICK))
                .isEqualByComparingTo("100.25");
    }

    @Test
    void keepsAnAlreadyExecutableEntryUnchanged() {
        assertThat(ExecutablePriceNormalizer.roundEntryToExecutableTick(BacktestTradeResult.Direction.LONG, new BigDecimal("100.25"), TICK))
                .isEqualByComparingTo("100.25");
        assertThat(ExecutablePriceNormalizer.isTickAligned(new BigDecimal("100.25"), TICK)).isTrue();
        assertThat(ExecutablePriceNormalizer.isTickAligned(new BigDecimal("100.125"), TICK)).isFalse();
    }

    @Test
    void roundsSyntheticBlendedExitsAgainstTheTradeDirection() {
        assertThat(ExecutablePriceNormalizer.roundBlendedExitToExecutableTick(BacktestTradeResult.Direction.LONG, new BigDecimal("100.125"), TICK))
                .isEqualByComparingTo("100.00");
        assertThat(ExecutablePriceNormalizer.roundBlendedExitToExecutableTick(BacktestTradeResult.Direction.SHORT, new BigDecimal("100.125"), TICK))
                .isEqualByComparingTo("100.25");
    }
}
