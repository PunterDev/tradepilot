package com.tradepilot.survival.dto;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalTime;

public record ProtectionCommitmentRequest(LocalDate effectiveSessionDate, BigDecimal maxNormalizedExposure, Integer maxAttempts,
        BigDecimal maxSessionLoss, Integer postLossPauseMinutes, LocalTime allowedWindowStart, LocalTime allowedWindowEnd,
        Boolean requireFreshSetupConfirmation) {}
