package com.tradepilot.playbook.service;

import com.tradepilot.playbook.enums.PlaybookConfidence;

public record PlaybookComplianceResult(
        int score,
        int requiredScore,
        int optionalScore,
        int missingRequiredCount,
        int unknownItemCount,
        PlaybookConfidence confidence
) {
}
