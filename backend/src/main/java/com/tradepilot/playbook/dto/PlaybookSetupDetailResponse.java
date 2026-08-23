package com.tradepilot.playbook.dto;

import com.tradepilot.playbook.enums.PlaybookCategory;
import com.tradepilot.playbook.enums.PlaybookDirection;
import com.tradepilot.playbook.enums.PlaybookStatus;
import java.time.LocalDateTime;
import java.util.List;

public record PlaybookSetupDetailResponse(
        Long id,
        Long accountId,
        String accountName,
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
        Integer versionNumber,
        LocalDateTime createdAt,
        LocalDateTime updatedAt,
        LocalDateTime archivedAt,
        List<PlaybookChecklistItemDto> checklistItems,
        PlaybookAnalysisResponse analysis
) {
}
