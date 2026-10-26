package com.tradepilot.survival.dto;

import com.tradepilot.survival.*;
import java.math.BigDecimal;
import java.time.*;

public record ProtectionCommitmentResponse(Long id, Long accountId, LocalDate effectiveSessionDate,
        BigDecimal maxNormalizedExposure, Integer maxAttempts, BigDecimal maxSessionLoss, Integer postLossPauseMinutes,
        LocalTime allowedWindowStart, LocalTime allowedWindowEnd, Boolean requireFreshSetupConfirmation,
        ProtectionCommitmentSource source, ProtectionCommitmentStatus status, LocalDateTime acceptedAt) {}
