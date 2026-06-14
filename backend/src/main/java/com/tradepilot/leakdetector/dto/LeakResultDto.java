package com.tradepilot.leakdetector.dto;

import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Data
@Builder
public class LeakResultDto {
    private LeakType leakType;
    private String title;
    private String summary;
    private String description;
    private Integer tradeCount;
    private BigDecimal winRate;
    private BigDecimal totalPnl;
    private BigDecimal avgPnl;
    private BigDecimal estimatedMonthlyImpact;
    private BigDecimal totalR;
    private BigDecimal avgR;
    private BigDecimal estimatedMonthlyImpactR;
    private Boolean hasValidRMultiple;
    private Integer confidenceScore;
    private LeakSeverity severity;
    private String recommendedFix;
    private Integer relatedTradeCount;
    private List<Long> relatedTradeIds;
    private LocalDateTime generatedAt;
}
