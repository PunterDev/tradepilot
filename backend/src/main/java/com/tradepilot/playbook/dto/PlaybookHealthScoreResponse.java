package com.tradepilot.playbook.dto;

public record PlaybookHealthScoreResponse(
        Integer score,
        String confidence,
        Integer definitionCompleteness,
        Integer historicalEvidence,
        Integer ruleAdherence,
        Integer performanceConsistency,
        Integer recentRelevance
) {
}
