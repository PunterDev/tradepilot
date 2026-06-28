package com.tradepilot.controller;

import com.tradepilot.dto.session.GenerateSessionReviewRequest;
import com.tradepilot.dto.session.SessionReviewResponse;
import com.tradepilot.entity.User;
import com.tradepilot.featureusage.FeatureKey;
import com.tradepilot.featureusage.FeatureUsageService;
import com.tradepilot.service.SessionReviewService;
import jakarta.validation.Valid;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import java.time.LocalDate;

@RestController
@RequestMapping("/api/sessions/review")
public class SessionReviewController {
    private final SessionReviewService sessionReviewService;
    private final FeatureUsageService featureUsageService;

    @GetMapping
    public SessionReviewResponse getReview(@AuthenticationPrincipal User user, @RequestParam Long accountId, @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date) {
        SessionReviewResponse response = sessionReviewService.getSessionReview(user, accountId, date);
        featureUsageService.track(user, FeatureKey.SESSION_REVIEW);
        return response;
    }

    @PostMapping("/generate")
    public SessionReviewResponse generateReview(@AuthenticationPrincipal User user, @Valid @RequestBody GenerateSessionReviewRequest request) {
        SessionReviewResponse response = sessionReviewService.generateSessionReview(user, request.getAccountId(), request.getDate(), Boolean.TRUE.equals(request.getForceRefresh()));
        featureUsageService.track(user, FeatureKey.SESSION_REVIEW);
        return response;
    }

    @java.lang.SuppressWarnings("all")
    public SessionReviewController(final SessionReviewService sessionReviewService, final FeatureUsageService featureUsageService) {
        this.sessionReviewService = sessionReviewService;
        this.featureUsageService = featureUsageService;
    }
}
