package com.tradepilot.preanalysis.dto;

import java.util.List;

public record TradingPlaybookRequest(
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
        String customNotes
) {
}
