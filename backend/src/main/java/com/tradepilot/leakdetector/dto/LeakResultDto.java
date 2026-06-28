package com.tradepilot.leakdetector.dto;

import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Data
@NoArgsConstructor
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

    public static LeakResultDto of(
            LeakType leakType,
            String title,
            String summary,
            String description,
            Integer tradeCount,
            BigDecimal winRate,
            BigDecimal totalPnl,
            BigDecimal avgPnl,
            BigDecimal estimatedMonthlyImpact,
            BigDecimal totalR,
            BigDecimal avgR,
            BigDecimal estimatedMonthlyImpactR,
            Boolean hasValidRMultiple,
            Integer confidenceScore,
            LeakSeverity severity,
            String recommendedFix,
            Integer relatedTradeCount,
            List<Long> relatedTradeIds,
            LocalDateTime generatedAt
    ) {
        LeakResultDto result = new LeakResultDto();
        result.setLeakType(leakType);
        result.setTitle(title);
        result.setSummary(summary);
        result.setDescription(description);
        result.setTradeCount(tradeCount);
        result.setWinRate(winRate);
        result.setTotalPnl(totalPnl);
        result.setAvgPnl(avgPnl);
        result.setEstimatedMonthlyImpact(estimatedMonthlyImpact);
        result.setTotalR(totalR);
        result.setAvgR(avgR);
        result.setEstimatedMonthlyImpactR(estimatedMonthlyImpactR);
        result.setHasValidRMultiple(hasValidRMultiple);
        result.setConfidenceScore(confidenceScore);
        result.setSeverity(severity);
        result.setRecommendedFix(recommendedFix);
        result.setRelatedTradeCount(relatedTradeCount);
        result.setRelatedTradeIds(relatedTradeIds);
        result.setGeneratedAt(generatedAt);
        return result;
    }
}
