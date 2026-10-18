package com.tradepilot.importhealth.dto;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

public record AdminImportHealthSummaryResponse(
        LocalDate startDate,
        LocalDate endDate,
        long importAttempts,
        long completedImports,
        long failedImports,
        Double successRate,
        long previewFailures,
        long tradesSaved,
        long duplicates,
        long firstImports,
        List<CountRow> parserBreakdown,
        List<FailureRow> failureBreakdown,
        LocalDateTime generatedAt
) {
    public record CountRow(String key, long count) { }
    public record FailureRow(String stage, String category, long count) { }
}
