package com.tradepilot.dto.session;

import com.tradepilot.dto.account.TradingAccountResponse;
import com.tradepilot.dto.trade.TradeResponse;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;

@Data
public class SessionReviewResponse {
    private Long accountId;
    private LocalDate date;
    private LocalDate sessionDate;
    private TradingAccountResponse account;
    private List<TradeResponse> trades;
    private List<SessionTimelineTradeResponse> timelineTrades;
    private Integer totalTrades;
    private Integer winners;
    private Integer losers;
    private Integer breakevenTrades;
    private BigDecimal netPnl;
    private BigDecimal grossProfit;
    private BigDecimal grossLoss;
    private BigDecimal largestWinner;
    private BigDecimal largestLoser;
    private BigDecimal averageWinner;
    private BigDecimal averageLoser;
    private BigDecimal totalPnl;
    private BigDecimal winRate;
    private BigDecimal profitFactor;
    private String sessionGrade;
    private String sessionStory;
    private String bestTradeWhy;
    private String worstTradeWhy;
    private TradeResponse bestTrade;
    private TradeResponse worstTrade;
    private Map<String, Long> mistakeSummary;
    private List<SessionMistakeResponse> detectedMistakes;
    private SessionReplayInsightsResponse replayInsights;
    private Integer sessionDisciplineScore;
    private AiSessionReviewResponse aiSessionReview;
    private Boolean aiReviewCached;
    private LocalDate previousTradingDay;
    private LocalDate nextTradingDay;
}
