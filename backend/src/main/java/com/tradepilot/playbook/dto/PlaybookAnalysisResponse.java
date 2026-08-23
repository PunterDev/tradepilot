package com.tradepilot.playbook.dto;

import java.time.LocalDateTime;
import java.util.List;

public record PlaybookAnalysisResponse(
        Long setupId,
        PlaybookMetricsResponse metrics,
        PlaybookHealthScoreResponse healthScore,
        List<PlaybookBehaviorComparisonResponse> behaviorComparisons,
        PlaybookAiReviewResponse aiReview,
        boolean cached,
        boolean aiReviewStale,
        int creditsConsumed,
        String dataHash,
        LocalDateTime generatedAt,
        PlaybookEligibilityResponse eligibility
) {
}
