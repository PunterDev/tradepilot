package com.tradepilot.dto.trade;

import java.util.List;

public record TradeImportPreviewResponse(
        TradeImportFormat detectedFormat,
        List<String> sourceAccountNames,
        int totalRows,
        int filledRows,
        int cancelledRowsIgnored,
        int invalidRows,
        int completedTrades,
        int duplicateTrades,
        int incompletePositions,
        List<String> warnings,
        List<TradeImportPreviewTradeResponse> trades
) {
}
