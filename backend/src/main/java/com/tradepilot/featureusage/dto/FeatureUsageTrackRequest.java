package com.tradepilot.featureusage.dto;

import com.tradepilot.featureusage.FeatureKey;
import jakarta.validation.constraints.NotNull;

public record FeatureUsageTrackRequest(@NotNull FeatureKey feature) {
}
