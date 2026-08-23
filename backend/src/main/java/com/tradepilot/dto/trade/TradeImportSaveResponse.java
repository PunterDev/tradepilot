package com.tradepilot.dto.trade;

import java.util.List;

public record TradeImportSaveResponse(
        TradeImportPreviewResponse preview,
        List<TradeResponse> savedTrades,
        int savedCount
) {
}
