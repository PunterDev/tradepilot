package com.tradepilot.dto.trade;

import com.tradepilot.enums.OutcomeType;
import com.tradepilot.enums.SessionType;
import com.tradepilot.enums.SetupType;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

public record TradeImportRequest(
        Long accountId,
        String fileName,
        String externalTradeKey,
        String symbol,
        String market,
        LocalDate tradeDate,
        LocalDateTime entryTime,
        LocalDateTime exitTime,
        String side,
        SetupType setup,
        SessionType session,
        BigDecimal entryPrice,
        BigDecimal exitPrice,
        Integer quantity,
        BigDecimal pnlAmount,
        BigDecimal pnlR,
        OutcomeType outcome,
        String originalBrokerSymbol,
        String contractSymbol,
        String contractSymbolSource
) {}
