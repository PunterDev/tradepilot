package com.tradepilot.playbook.dto;

import java.util.List;

public record BulkLinkTradesRequest(
        List<Long> tradeIds,
        PlaybookTradeAssignmentRequest assignment
) {
}
