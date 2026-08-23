package com.tradepilot.playbook.dto;

import com.tradepilot.playbook.enums.PlaybookDetailState;
import com.tradepilot.playbook.enums.PlaybookRequirement;
import java.util.List;

public record PlaybookEligibilityResponse(
        PlaybookDetailState detailState,
        boolean definitionComplete,
        int completedDefinitionSections,
        int totalDefinitionSections,
        long linkedTradeCount,
        long reviewedTradeCount,
        boolean behaviorComparisonEligible,
        int behaviorComparisonMinimumTrades,
        boolean aiReviewEligible,
        int aiReviewMinimumTrades,
        List<PlaybookRequirement> missingRequirements,
        String recommendedPrimaryAction
) {
}
