package com.tradepilot.config;

import com.tradepilot.billing.Feature;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.LinkedHashMap;
import java.util.Map;

@RestController
@RequestMapping({"/api/features", "/api/v1/features"})
public class FeatureAvailabilityController {
    private final FeatureFlagService featureFlagService;

    public FeatureAvailabilityController(FeatureFlagService featureFlagService) {
        this.featureFlagService = featureFlagService;
    }

    @GetMapping
    public FeatureAvailabilityResponse getFeatures() {
        Map<String, Boolean> features = new LinkedHashMap<>();
        features.put("playbook", featureFlagService.isEnabled(Feature.PLAYBOOK));
        features.put("tradingDna", featureFlagService.isEnabled(Feature.TRADING_DNA));
        features.put("journey", featureFlagService.isEnabled(Feature.PERFORMANCE_TIMELINE));
        features.put("tradingMemory", featureFlagService.isEnabled(Feature.TRADING_MEMORY));
        features.put("tradeReplay", featureFlagService.isEnabled(Feature.TRADE_REPLAY));
        features.put("coach", true);
        return new FeatureAvailabilityResponse(featureFlagService.isV2FeaturesEnabled(), featureFlagService.isV3FeaturesEnabled(), features);
    }
}
