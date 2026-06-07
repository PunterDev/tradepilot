package com.tradepilot.tradingdna.dto;

import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;

@Data
@Builder
public class TradingDnaContext {
    private Long userId;
    private Long accountId;
    private boolean includeAllAccounts;
    private int days;
    private LocalDate startDate;
    private LocalDate endDate;
    private int totalTrades;
    private BigDecimal netPnl;
    private BigDecimal winRate;
    private String bestSetup;
    private String worstSetup;
    private String bestSession;
    private String worstSession;
    private Map<String, Long> mistakeCounts;
    private Map<String, BigDecimal> setupPnl;
    private Map<String, BigDecimal> emotionPnl;
    private Map<String, BigDecimal> confidencePnl;
    private Map<String, Long> violationCounts;
    private List<String> journalMetadataExamples;
    private List<String> behavioralInsights;
    private List<String> tradeReviewSummaries;
    private List<String> aiTradeStrengths;
    private List<String> aiTradeMistakes;
    private List<String> coachNotes;
    private List<String> deterministicSignals;
    private List<TradingDnaTimelinePoint> timeline;
    private Integer deterministicDisciplineScore;
    private Integer deterministicExecutionScore;
    private Integer deterministicPatienceScore;
    private Integer deterministicConsistencyScore;
}
