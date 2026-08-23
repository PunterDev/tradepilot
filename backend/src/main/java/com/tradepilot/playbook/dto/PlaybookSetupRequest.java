package com.tradepilot.playbook.dto;

import com.tradepilot.playbook.enums.PlaybookCategory;
import com.tradepilot.playbook.enums.PlaybookDirection;
import com.tradepilot.playbook.enums.PlaybookStatus;
import java.util.List;

public record PlaybookSetupRequest(
        Long accountId,
        String name,
        String shortDescription,
        String description,
        PlaybookCategory category,
        PlaybookStatus status,
        PlaybookDirection direction,
        List<String> instruments,
        List<String> sessions,
        List<String> timeframes,
        List<String> tags,
        List<String> marketContext,
        List<String> riskRules,
        List<String> managementRules,
        List<String> postTradeRules,
        String iconKey,
        String colorKey,
        String imageUrl,
        List<PlaybookChecklistItemDto> checklistItems
) {
}
