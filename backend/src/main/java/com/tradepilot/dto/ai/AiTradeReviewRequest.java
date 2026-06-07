package com.tradepilot.dto.ai;

import lombok.Data;

@Data
public class AiTradeReviewRequest {
    private Long tradeId;
    private String journalText;
    private TradeSnapshot trade;
    private Boolean forceRefresh = false;
}
