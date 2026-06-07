package com.tradepilot.featureusage;

import com.tradepilot.entity.User;
import com.tradepilot.featureusage.dto.FeatureUsageTrackRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/feature-usage")
@RequiredArgsConstructor
public class FeatureUsageController {
    private final FeatureUsageService featureUsageService;

    @PostMapping("/track")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void track(@AuthenticationPrincipal User user, @Valid @RequestBody FeatureUsageTrackRequest request) {
        featureUsageService.track(user, request.feature());
    }
}
