package com.tradepilot.leakdetector.controller;

import com.tradepilot.entity.User;
import com.tradepilot.featureusage.FeatureKey;
import com.tradepilot.featureusage.FeatureUsageService;
import com.tradepilot.leakdetector.dto.LeakDetectorRegenerateRequest;
import com.tradepilot.leakdetector.dto.LeakDetectorResponse;
import com.tradepilot.leakdetector.service.LeakDetectorService;
import jakarta.validation.Valid;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/ai/leak-detector")
public class LeakDetectorController {
    private final LeakDetectorService leakDetectorService;
    private final FeatureUsageService featureUsageService;

    @GetMapping
    public LeakDetectorResponse getLeakDetector(@AuthenticationPrincipal User user, @RequestParam Long accountId) {
        LeakDetectorResponse response = leakDetectorService.getLeakDetector(user, accountId);
        featureUsageService.track(user, FeatureKey.AI_LEAK_DETECTOR);
        return response;
    }

    @PostMapping("/regenerate")
    public LeakDetectorResponse regenerate(@AuthenticationPrincipal User user, @Valid @RequestBody LeakDetectorRegenerateRequest request) {
        LeakDetectorResponse response = leakDetectorService.regenerate(user, request.getAccountId());
        featureUsageService.track(user, FeatureKey.AI_LEAK_DETECTOR);
        return response;
    }

    @java.lang.SuppressWarnings("all")
    public LeakDetectorController(final LeakDetectorService leakDetectorService, final FeatureUsageService featureUsageService) {
        this.leakDetectorService = leakDetectorService;
        this.featureUsageService = featureUsageService;
    }
}
