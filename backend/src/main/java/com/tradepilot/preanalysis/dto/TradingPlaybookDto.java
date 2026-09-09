package com.tradepilot.preanalysis.dto;

import java.time.LocalDateTime;
import java.util.List;

public record TradingPlaybookDto(
        Long id,
        String name,
        String description,
        List<String> markets,
        List<String> contextTimeframes,
        List<String> setupTimeframes,
        List<String> executionTimeframes,
        List<String> setupSequence,
        List<String> entryRules,
        List<String> invalidationRules,
        List<String> targetRules,
        List<String> avoidRules,
        String customNotes,
        boolean active,
        LocalDateTime createdAt,
        LocalDateTime updatedAt
) {
}
