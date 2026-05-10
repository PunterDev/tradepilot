package com.tradepilot.dto.trade;

import com.tradepilot.enums.*;
import lombok.*;

import java.time.LocalDate;

@Data
@Builder
public class TradeFilterRequest {
    private String symbol;
    private SetupType setup;
    private SessionType session;
    private OutcomeType outcome;
    private LocalDate startDate;
    private LocalDate endDate;
    private Long accountId;
}
