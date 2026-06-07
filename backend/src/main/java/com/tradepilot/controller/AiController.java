package com.tradepilot.controller;

import com.tradepilot.dto.ai.AiTradeReviewJobResponse;
import com.tradepilot.dto.ai.AiTradeReviewRequest;
import com.tradepilot.dto.ai.AiTradeReviewResponse;
import com.tradepilot.entity.User;
import com.tradepilot.featureusage.FeatureKey;
import com.tradepilot.featureusage.FeatureUsageService;
import com.tradepilot.service.AiTradeReviewService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/ai")
@RequiredArgsConstructor
public class AiController {

    private final AiTradeReviewService aiTradeReviewService;
    private final FeatureUsageService featureUsageService;

    @PostMapping("/trade-review")
    public AiTradeReviewResponse reviewTrade(
            @AuthenticationPrincipal User user,
            @RequestBody AiTradeReviewRequest request
    ) {
        AiTradeReviewResponse response = aiTradeReviewService.reviewTrade(user, request);
        featureUsageService.track(user, FeatureKey.AI_REVIEW);
        return response;
    }

    @PostMapping("/trade-review/async")
    public AiTradeReviewJobResponse startAsyncTradeReview(
            @AuthenticationPrincipal User user,
            @RequestBody AiTradeReviewRequest request
    ) {
        AiTradeReviewJobResponse response = aiTradeReviewService.startAsyncReview(user, request);
        featureUsageService.track(user, FeatureKey.AI_REVIEW);
        return response;
    }

    @GetMapping("/trade-review/status/{reviewId}")
    public AiTradeReviewJobResponse getTradeReviewStatus(
            @AuthenticationPrincipal User user,
            @PathVariable Long reviewId
    ) {
        return aiTradeReviewService.getReviewStatus(user, reviewId);
    }


}
