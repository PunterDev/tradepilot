package com.tradepilot.tradingdna.dto;

import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;

@Data
public class TradingDnaResponse {
    private Long id;
    private Long accountId;
    private Integer days;
    private Boolean includeAllAccounts;
    private String status;
    private Boolean cached;
    private Boolean limitReached;
    private Boolean aiGenerated;
    private Boolean noTrades;
    private Boolean limitedData;
    private Integer creditsConsumed;
    private String message;
    private Integer totalTrades;
    private LocalDateTime lastGeneratedAt;
    private List<String> strengths;
    private List<String> weaknesses;
    private List<String> emotionalPatterns;
    private List<String> riskPatterns;
    private List<String> bestConditions;
    private List<String> worstConditions;
    private List<String> recurringMistakes;
    private String improvementTrend;
    private Integer consistencyScore;
    private Integer disciplineScore;
    private Integer executionScore;
    private Integer patienceScore;
    private String confidenceSummary;
    private String coachingSummary;
    private List<TradingDnaTimelinePoint> timeline;
}
