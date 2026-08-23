package com.tradepilot.tradereplay.service;

public record ContractCandidate(
        String label,
        String rootInstrument,
        String normalizedSymbol,
        String monthCode,
        int month,
        int year,
        boolean mostLikely
) {}
