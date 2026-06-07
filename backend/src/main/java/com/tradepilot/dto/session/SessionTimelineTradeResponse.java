package com.tradepilot.dto.session;

import com.tradepilot.enums.OutcomeType;
import com.tradepilot.enums.SetupType;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
public class SessionTimelineTradeResponse {
    private Long tradeId;
    private String symbol;
    private String side;
    private LocalDateTime entryTime;
    private LocalDateTime exitTime;
    private Integer quantity;
    private BigDecimal pnl;
    private SetupType setup;
    private Integer confidence;
    private String emotion;
    private String notes;
    private OutcomeType outcome;
}
