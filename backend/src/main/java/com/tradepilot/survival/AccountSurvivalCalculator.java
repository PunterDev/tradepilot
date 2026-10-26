package com.tradepilot.survival;

import java.math.*;
import java.util.*;

/** Pure, deterministic calculation rules for Account Survival V1. */
public final class AccountSurvivalCalculator {
    public static final int RECENT_LOSS_SAMPLE_SIZE = 20;
    public static final int MIN_LOSS_EVIDENCE = 5;
    private AccountSurvivalCalculator() {}
    public static BigDecimal medianLoss(List<BigDecimal> losses) {
        if (losses == null || losses.size() < MIN_LOSS_EVIDENCE) return null;
        List<BigDecimal> ordered = losses.stream().filter(Objects::nonNull).map(BigDecimal::abs).sorted().limit(RECENT_LOSS_SAMPLE_SIZE).toList();
        if (ordered.size() < MIN_LOSS_EVIDENCE) return null;
        int mid = ordered.size() / 2;
        return (ordered.size() % 2 == 0 ? ordered.get(mid - 1).add(ordered.get(mid)).divide(BigDecimal.valueOf(2), 6, RoundingMode.HALF_UP) : ordered.get(mid));
    }
    public static BigDecimal percent(BigDecimal numerator, BigDecimal denominator) { return valid(denominator) && numerator != null ? numerator.multiply(BigDecimal.valueOf(100)).divide(denominator, 4, RoundingMode.HALF_UP) : null; }
    public static BigDecimal lossCapacity(BigDecimal buffer, BigDecimal typicalLoss) { return valid(buffer) && valid(typicalLoss) ? buffer.divide(typicalLoss, 2, RoundingMode.DOWN) : null; }
    public static AccountSurvivalState state(BigDecimal buffer, BigDecimal typicalLossPercent, BigDecimal capacity) {
        if (buffer == null || buffer.signum() <= 0) return AccountSurvivalState.CRITICAL;
        if (typicalLossPercent == null || capacity == null) return AccountSurvivalState.INSUFFICIENT_DATA;
        if (typicalLossPercent.compareTo(BigDecimal.valueOf(25)) > 0 || capacity.compareTo(BigDecimal.valueOf(3)) < 0) return AccountSurvivalState.CRITICAL;
        if (typicalLossPercent.compareTo(BigDecimal.valueOf(15)) > 0 || capacity.compareTo(BigDecimal.valueOf(5)) < 0) return AccountSurvivalState.FRAGILE;
        if (typicalLossPercent.compareTo(BigDecimal.valueOf(10)) > 0 || capacity.compareTo(BigDecimal.valueOf(8)) < 0) return AccountSurvivalState.WATCH;
        return AccountSurvivalState.STABLE;
    }
    public static boolean valid(BigDecimal value) { return value != null && value.signum() > 0; }
}
