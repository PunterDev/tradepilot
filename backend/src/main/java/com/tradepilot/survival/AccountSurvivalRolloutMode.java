package com.tradepilot.survival;

import java.util.Locale;

public enum AccountSurvivalRolloutMode {
    DISABLED,
    BETA,
    GENERAL_AVAILABILITY;

    public static AccountSurvivalRolloutMode fromConfiguration(String configured) {
        if (configured == null || configured.isBlank()) {
            return DISABLED;
        }
        try {
            return valueOf(configured.trim().toUpperCase(Locale.ROOT).replace('-', '_'));
        } catch (IllegalArgumentException ignored) {
            return DISABLED;
        }
    }
}
