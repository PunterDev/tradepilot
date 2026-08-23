package com.tradepilot.performancetimeline.controller;

import com.tradepilot.billing.Feature;
import com.tradepilot.config.FeatureFlagService;
import com.tradepilot.entity.User;
import com.tradepilot.featureusage.FeatureKey;
import com.tradepilot.featureusage.FeatureUsageService;
import com.tradepilot.performancetimeline.dto.PerformanceTimelineResponse;
import com.tradepilot.performancetimeline.service.PerformanceTimelineService;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping({"/api/ai/performance-timeline", "/api/v1/ai/performance-timeline"})
public class PerformanceTimelineController {
    private final PerformanceTimelineService performanceTimelineService;
    private final FeatureUsageService featureUsageService;
    private final FeatureFlagService featureFlagService;

    public PerformanceTimelineController(PerformanceTimelineService performanceTimelineService, FeatureUsageService featureUsageService, FeatureFlagService featureFlagService) {
        this.performanceTimelineService = performanceTimelineService;
        this.featureUsageService = featureUsageService;
        this.featureFlagService = featureFlagService;
    }

    @GetMapping
    public PerformanceTimelineResponse getTimeline(@AuthenticationPrincipal User user, @RequestParam Long accountId) {
        featureFlagService.requireEnabled(Feature.PERFORMANCE_TIMELINE);
        PerformanceTimelineResponse response = performanceTimelineService.getTimeline(user, accountId);
        featureUsageService.track(user, FeatureKey.PERFORMANCE_TIMELINE);
        return response;
    }

    @PostMapping("/regenerate")
    public PerformanceTimelineResponse regenerate(@AuthenticationPrincipal User user, @RequestParam Long accountId) {
        featureFlagService.requireEnabled(Feature.PERFORMANCE_TIMELINE);
        PerformanceTimelineResponse response = performanceTimelineService.regenerate(user, accountId);
        featureUsageService.track(user, FeatureKey.PERFORMANCE_TIMELINE);
        return response;
    }
}
