package com.tradepilot.leakdetector.dto;

public record BehavioralFindingDto(
        boolean qualified,
        String reasonNotQualified,
        int occurrenceCount,
        int tradeSampleSize,
        int sessionSampleSize,
        EvidenceStrength evidenceStrength,
        BehavioralBaselineDto baseline,
        BehavioralBaselineDto triggered,
        BehavioralCounterfactualDto counterfactual,
        String engineVersion,
        String ruleVersion,
        Integer threshold
) {}
