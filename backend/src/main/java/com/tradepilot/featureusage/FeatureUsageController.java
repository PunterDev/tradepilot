package com.tradepilot.featureusage;

import com.tradepilot.entity.User;
import com.tradepilot.featureusage.FeatureKey;
import com.tradepilot.featureusage.FeatureUsageAction;
import com.tradepilot.featureusage.FeatureUsageEventType;
import com.tradepilot.featureusage.dto.FeatureUsageTrackRequest;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/feature-usage")
public class FeatureUsageController {
    private final FeatureUsageService featureUsageService;

    @PostMapping("/track")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void track(@AuthenticationPrincipal User user, @Valid @RequestBody FeatureUsageTrackRequest request) {
        featureUsageService.track(user, request.feature());
    }

    @PostMapping("/conversion/locked-preview-viewed")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void trackLockedPreviewViewed(@AuthenticationPrincipal User user) {
        featureUsageService.track(user, FeatureKey.TRADING_DNA, FeatureUsageAction.LOCKED_PREVIEW_VIEWED,
                FeatureUsageEventType.PAGE_VIEW, null, "LOCKED_FEATURE_PREVIEW", null, "client", null);
    }

    @PostMapping("/conversion/billing-page-viewed")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void trackBillingPageViewed(@AuthenticationPrincipal User user) {
        featureUsageService.track(user, FeatureKey.PLAYBOOK, FeatureUsageAction.BILLING_PAGE_VIEWED,
                FeatureUsageEventType.PAGE_VIEW, null, "BILLING_PAGE", null, "client", null);
    }

    @java.lang.SuppressWarnings("all")
    public FeatureUsageController(final FeatureUsageService featureUsageService) {
        this.featureUsageService = featureUsageService;
    }
}
