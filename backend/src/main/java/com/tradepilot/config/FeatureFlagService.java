package com.tradepilot.config;

import com.tradepilot.billing.Feature;
import org.springframework.stereotype.Service;

import java.util.EnumSet;
import java.util.Set;

@Service
public class FeatureFlagService {
    private static final Set<Feature> V2_FEATURES = EnumSet.of(
            Feature.PLAYBOOK,
            Feature.TRADING_DNA,
            Feature.PERFORMANCE_TIMELINE,
            Feature.TRADING_MEMORY
    );
    private static final Set<Feature> V3_FEATURES = EnumSet.of(Feature.TRADE_REPLAY);

    private final FeatureProperties properties;

    public FeatureFlagService(FeatureProperties properties) {
        this.properties = properties;
    }

    public boolean isEnabled(Feature feature) {
        if (V2_FEATURES.contains(feature)) {
            return properties.isV2Enabled();
        }
        if (V3_FEATURES.contains(feature)) {
            return properties.isV3Enabled();
        }
        return true;
    }

    public void requireEnabled(Feature feature) {
        if (!isEnabled(feature)) {
            throw new FeatureDisabledException(feature);
        }
    }

    public boolean isV2FeaturesEnabled() {
        return properties.isV2Enabled();
    }

    public boolean isV3FeaturesEnabled() {
        return properties.isV3Enabled();
    }
}
