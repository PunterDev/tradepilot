package com.tradepilot.leakdetector.dto;

import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Data
@NoArgsConstructor
public class LeakDetectorResponse {
    private Long id;
    private Long accountId;
    private Boolean cached;
    private Boolean aiGenerated;
    private Boolean noTrades;
    private Boolean limitedData;
    private Integer creditsConsumed;
    private Integer totalTrades;
    private Integer sampleTradeCount;
    private String dataHash;
    private List<LeakResultDto> leaks;
    private String message;
    private String biggestLeakTitle;
    private BigDecimal biggestLeakImpact;
    private String biggestLeakFix;
    private BigDecimal totalEstimatedMonthlyImpact;
    private Integer highestConfidence;
    private LocalDateTime generatedAt;

    public static LeakDetectorResponse of(
            Long id,
            Long accountId,
            Boolean cached,
            Boolean aiGenerated,
            Boolean noTrades,
            Boolean limitedData,
            Integer creditsConsumed,
            Integer totalTrades,
            Integer sampleTradeCount,
            String dataHash,
            List<LeakResultDto> leaks,
            String message,
            String biggestLeakTitle,
            BigDecimal biggestLeakImpact,
            String biggestLeakFix,
            BigDecimal totalEstimatedMonthlyImpact,
            Integer highestConfidence,
            LocalDateTime generatedAt
    ) {
        LeakDetectorResponse response = new LeakDetectorResponse();
        response.setId(id);
        response.setAccountId(accountId);
        response.setCached(cached);
        response.setAiGenerated(aiGenerated);
        response.setNoTrades(noTrades);
        response.setLimitedData(limitedData);
        response.setCreditsConsumed(creditsConsumed);
        response.setTotalTrades(totalTrades);
        response.setSampleTradeCount(sampleTradeCount);
        response.setDataHash(dataHash);
        response.setLeaks(leaks);
        response.setMessage(message);
        response.setBiggestLeakTitle(biggestLeakTitle);
        response.setBiggestLeakImpact(biggestLeakImpact);
        response.setBiggestLeakFix(biggestLeakFix);
        response.setTotalEstimatedMonthlyImpact(totalEstimatedMonthlyImpact);
        response.setHighestConfidence(highestConfidence);
        response.setGeneratedAt(generatedAt);
        return response;
    }
}
