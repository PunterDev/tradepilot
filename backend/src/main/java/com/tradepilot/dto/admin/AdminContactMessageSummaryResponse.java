package com.tradepilot.dto.admin;

public record AdminContactMessageSummaryResponse(
        long total,
        long newCount,
        long openCount,
        long repliedCount,
        long archivedCount,
        long failedEmailCount
) {
}
