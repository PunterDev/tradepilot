package com.tradepilot.config;

import java.util.Map;

public record FeatureAvailabilityResponse(
        boolean v2FeaturesEnabled,
        boolean v3FeaturesEnabled,
        Map<String, Boolean> features
) {
}
