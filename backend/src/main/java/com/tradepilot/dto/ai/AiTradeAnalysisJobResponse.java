package com.tradepilot.dto.ai;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class AiTradeAnalysisJobResponse {
    private Long analysisId;
    private Long tradeId;
    private String status;
    private AiTradeAnalysisResponse analysis;
    private String message;
}
