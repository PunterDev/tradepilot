package com.tradepilot.dto.trade;

import lombok.Data;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Data
public class TradeViolationResponse {
    private Long id;
    private String ruleCode;
    private String ruleTitle;
    private String category;
    private String severity;
    private String reason;
    private String source;
    private LocalDate tradeDate;
    private LocalDateTime createdAt;

    public static TradeViolationResponse of(
            Long id,
            String ruleCode,
            String ruleTitle,
            String category,
            String severity,
            String reason,
            String source,
            LocalDate tradeDate,
            LocalDateTime createdAt
    ) {
        TradeViolationResponse response = new TradeViolationResponse();
        response.setId(id);
        response.setRuleCode(ruleCode);
        response.setRuleTitle(ruleTitle);
        response.setCategory(category);
        response.setSeverity(severity);
        response.setReason(reason);
        response.setSource(source);
        response.setTradeDate(tradeDate);
        response.setCreatedAt(createdAt);
        return response;
    }
}
