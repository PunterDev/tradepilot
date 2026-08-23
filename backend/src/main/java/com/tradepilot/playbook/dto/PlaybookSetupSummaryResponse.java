package com.tradepilot.playbook.dto;

import com.tradepilot.playbook.enums.PlaybookCategory;
import com.tradepilot.playbook.enums.PlaybookDirection;
import com.tradepilot.playbook.enums.PlaybookStatus;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

public record PlaybookSetupSummaryResponse(
        Long id,
        Long accountId,
        String accountName,
        String name,
        String shortDescription,
        PlaybookCategory category,
        PlaybookStatus status,
        PlaybookDirection direction,
        List<String> instruments,
        List<String> sessions,
        List<String> tags,
        long linkedTradeCount,
        BigDecimal totalR,
        BigDecimal netPnl,
        BigDecimal winRate,
        BigDecimal profitFactor,
        Integer complianceScore,
        Integer healthScore,
        String confidence,
        LocalDate lastUsedDate,
        LocalDateTime lastAnalyzedAt,
        boolean aiReviewStale
) {
}
