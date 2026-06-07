package com.tradepilot.dto.ai;

import lombok.Data;

@Data
public class AiTradeAnalysisRequest {
    private Long tradeId;
    private String journalText;
    private TradeSnapshot trade;
    private String reviewVersion = "v1";
}
