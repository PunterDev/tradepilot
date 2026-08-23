package com.tradepilot.config;

import com.tradepilot.billing.Feature;

public class FeatureDisabledException extends RuntimeException {
    private final Feature feature;

    public FeatureDisabledException(Feature feature) {
        super(label(feature) + " is not available in this release.");
        this.feature = feature;
    }

    public Feature getFeature() {
        return feature;
    }

    private static String label(Feature feature) {
        return switch (feature) {
            case PLAYBOOK -> "AI Trading Playbook";
            case TRADING_DNA -> "Trading DNA";
            case PERFORMANCE_TIMELINE -> "AI Performance Timeline";
            default -> feature.name();
        };
    }
}
