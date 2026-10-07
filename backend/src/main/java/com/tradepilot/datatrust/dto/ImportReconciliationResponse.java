package com.tradepilot.datatrust.dto;

import com.tradepilot.datatrust.ImportMismatchReason;
import com.tradepilot.datatrust.ImportReconciliationStatus;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Set;

public record ImportReconciliationResponse(Long importId, String source, int tradeCount, BigDecimal netPnl, ImportReconciliationStatus status, LocalDateTime reviewedAt, Set<ImportMismatchReason> mismatchReasons, String comment) {}
