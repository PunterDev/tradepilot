package com.tradepilot.playbook.dto;

import java.util.List;

public record PlaybookDraftResponse(
        PlaybookSetupRequest draft,
        List<String> assumptions,
        int creditsConsumed,
        String message
) {
}
