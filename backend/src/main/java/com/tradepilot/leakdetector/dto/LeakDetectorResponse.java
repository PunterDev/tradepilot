package com.tradepilot.leakdetector.dto;

import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Data
@Builder
public class LeakDetectorResponse {
    private Long id;
    private Long accountId;
    private Boolean cached;
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
}
