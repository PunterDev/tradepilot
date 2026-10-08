package com.tradepilot.backtest.engine;

import com.tradepilot.backtest.model.BacktestTradeResult;
import java.math.BigDecimal;
import java.math.RoundingMode;

/** Converts a causally-triggered reference price into a conservative exchange-executable fill. */
final class ExecutablePriceNormalizer {
    private ExecutablePriceNormalizer() {
    }

    static BigDecimal roundEntryToExecutableTick(BacktestTradeResult.Direction direction, BigDecimal referencePrice,
                                                  BigDecimal tickSize) {
        if (tickSize == null || tickSize.signum() <= 0) {
            throw new IllegalArgumentException("Tick size must be positive.");
        }
        RoundingMode mode = direction == BacktestTradeResult.Direction.LONG ? RoundingMode.CEILING : RoundingMode.FLOOR;
        return referencePrice.divide(tickSize, 0, mode).multiply(tickSize);
    }

    static boolean isTickAligned(BigDecimal price, BigDecimal tickSize) {
        return price.remainder(tickSize).stripTrailingZeros().signum() == 0;
    }

    static BigDecimal roundBlendedExitToExecutableTick(BacktestTradeResult.Direction direction, BigDecimal blendedPrice,
                                                        BigDecimal tickSize) {
        if (tickSize == null || tickSize.signum() <= 0) {
            throw new IllegalArgumentException("Tick size must be positive.");
        }
        RoundingMode mode = direction == BacktestTradeResult.Direction.LONG ? RoundingMode.FLOOR : RoundingMode.CEILING;
        return blendedPrice.divide(tickSize, 0, mode).multiply(tickSize);
    }
}
