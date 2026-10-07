package com.tradepilot.datatrust.dto;

import com.tradepilot.datatrust.ImportMismatchReason;
import com.tradepilot.datatrust.ImportReconciliationStatus;
import jakarta.validation.constraints.NotNull;
import java.util.Set;

public record ReconciliationUpdateRequest(@NotNull ImportReconciliationStatus status, Set<ImportMismatchReason> mismatchReasons, String comment) {}
