package com.tradepilot.dto.trade;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

public record TradeImportPreviewTradeResponse(
        String symbol,
        String side,
        LocalDate tradeDate,
        LocalDateTime entryTime,
        LocalDateTime exitTime,
        Integer quantity,
        BigDecimal entryPrice,
        BigDecimal exitPrice,
        BigDecimal pnlAmount,
        String originalBrokerSymbol,
        String contractSymbol,
        String externalTradeKey,
        List<String> sourceExecutionIds,
        boolean duplicate
) {
}
