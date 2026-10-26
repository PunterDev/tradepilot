package com.tradepilot.survival.dto;

import java.math.BigDecimal;
import java.util.List;

/** Public, account-display-safe evidence for an observed cross-account sequence. */
public record CrossAccountContinuationEvidence(
        List<String> sourceAccounts,
        List<String> laterAccounts,
        List<String> sourceSymbols,
        List<String> laterSymbols,
        String instrumentFamily,
        String direction,
        Long elapsedSeconds,
        BigDecimal previousRealizedLoss,
        BigDecimal previousNormalizedExposure,
        BigDecimal newNormalizedExposure,
        BigDecimal exposureDifference,
        BigDecimal exposureMultiplier,
        String timestampPrecision,
        String confidence,
        List<String> limitations,
        String engineVersion) { }
