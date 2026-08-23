package com.tradepilot.playbook.dto;

import java.util.List;

public record PlaybookDraftRequest(
        String description,
        Long accountId,
        List<Long> tradeIds
) {
}
