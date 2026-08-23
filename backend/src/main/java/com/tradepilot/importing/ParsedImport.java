package com.tradepilot.importing;

import com.tradepilot.dto.trade.TradeImportFormat;
import com.tradepilot.dto.trade.TradeImportRequest;

import java.util.List;

public record ParsedImport(
        TradeImportFormat format,
        List<String> sourceAccountNames,
        int totalRows,
        int filledRows,
        int cancelledRowsIgnored,
        int invalidRows,
        int incompletePositions,
        List<String> warnings,
        List<TradeImportRequest> trades,
        List<List<String>> sourceExecutionIds
) {
}
