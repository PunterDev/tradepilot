package com.tradepilot.tradereplay.dto;

public record ContractCandidateResponse(
        String label,
        String rootInstrument,
        String normalizedSymbol,
        String monthCode,
        int month,
        int year,
        boolean mostLikely
) {}
