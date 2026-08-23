package com.tradepilot.playbook.dto;

import java.util.List;

public record PlaybookOverviewResponse(
        long totalActiveSetups,
        long draftSetups,
        long testingSetups,
        long validatedSetups,
        long archivedSetups,
        PlaybookSetupSummaryResponse bestPerformingSetup,
        PlaybookSetupSummaryResponse highestComplianceSetup,
        PlaybookSetupSummaryResponse highestHealthSetup,
        PlaybookSetupSummaryResponse setupNeedingAttention,
        List<PlaybookSetupSummaryResponse> recentSetups,
        List<PlaybookMilestoneResponse> recentMilestones
) {
}
