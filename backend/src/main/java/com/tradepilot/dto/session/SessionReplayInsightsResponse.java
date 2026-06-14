package com.tradepilot.dto.session;

import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
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

    public static SessionReplayInsightsResponse of(
            SessionTimelineTradeResponse firstTrade,
            SessionTimelineTradeResponse lastTrade,
            SessionTimelineTradeResponse largestWinner,
            SessionTimelineTradeResponse largestLoser,
            SessionTimelineTradeResponse longestHold,
            SessionTimelineTradeResponse shortestHold,
            Integer longestHoldMinutes,
            Integer shortestHoldMinutes,
            Integer bestTradingHour,
            BigDecimal bestTradingHourPnl,
            Integer worstTradingHour,
            BigDecimal worstTradingHourPnl,
            LocalDateTime firstTradeTime,
            LocalDateTime lastTradeTime
    ) {
        SessionReplayInsightsResponse response = new SessionReplayInsightsResponse();
        response.setFirstTrade(firstTrade);
        response.setLastTrade(lastTrade);
        response.setLargestWinner(largestWinner);
        response.setLargestLoser(largestLoser);
        response.setLongestHold(longestHold);
        response.setShortestHold(shortestHold);
        response.setLongestHoldMinutes(longestHoldMinutes);
        response.setShortestHoldMinutes(shortestHoldMinutes);
        response.setBestTradingHour(bestTradingHour);
        response.setBestTradingHourPnl(bestTradingHourPnl);
        response.setWorstTradingHour(worstTradingHour);
        response.setWorstTradingHourPnl(worstTradingHourPnl);
        response.setFirstTradeTime(firstTradeTime);
        response.setLastTradeTime(lastTradeTime);
        return response;
    }
}
