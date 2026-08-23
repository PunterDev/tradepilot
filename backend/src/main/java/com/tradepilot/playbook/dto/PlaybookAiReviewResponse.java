package com.tradepilot.playbook.dto;

import com.tradepilot.playbook.enums.PlaybookStatusRecommendation;
import java.util.List;

public record PlaybookAiReviewResponse(
        String headline,
        String summary,
        String strongestBehavior,
        String biggestSetupWeakness,
        String biggestExecutionWeakness,
        String mostBrokenRule,
        String complianceInsight,
        String performanceInsight,
        String recentTrend,
        String nextFocus,
        PlaybookStatusRecommendation statusRecommendation,
        String statusRecommendationReason,
        String encouragement,
        String confidenceExplanation,
        List<String> supportingEvidence,
        List<String> limitations
) {
}
