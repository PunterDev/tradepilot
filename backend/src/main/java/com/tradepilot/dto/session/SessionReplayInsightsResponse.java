package com.tradepilot.dto.session;

import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@Builder
public class SessionReplayInsightsResponse {
    private SessionTimelineTradeResponse firstTrade;
    private SessionTimelineTradeResponse lastTrade;
    private SessionTimelineTradeResponse largestWinner;
    private SessionTimelineTradeResponse largestLoser;
    private SessionTimelineTradeResponse longestHold;
    private SessionTimelineTradeResponse shortestHold;
    private Integer longestHoldMinutes;
    private Integer shortestHoldMinutes;
    private Integer bestTradingHour;
    private BigDecimal bestTradingHourPnl;
    private Integer worstTradingHour;
    private BigDecimal worstTradingHourPnl;
    private LocalDateTime firstTradeTime;
    private LocalDateTime lastTradeTime;
}
