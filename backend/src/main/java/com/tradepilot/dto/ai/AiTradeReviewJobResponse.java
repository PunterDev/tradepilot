package com.tradepilot.dto.ai;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class AiTradeReviewJobResponse {
    private Long reviewId;
    private Long tradeId;
    private String status;
    private AiTradeReviewResponse review;
    private String message;
}