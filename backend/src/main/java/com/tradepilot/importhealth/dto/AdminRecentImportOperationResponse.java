package com.tradepilot.importhealth.dto;

import java.time.LocalDateTime;

public record AdminRecentImportOperationResponse(
        LocalDateTime occurredAt,
        String user,
        Long accountId,
        String stage,
        String status,
        String parserSource,
        Integer parsedTrades,
        Integer savedRows,
        Integer duplicateRows,
        String failureCategory,
        boolean firstImport
) { }
