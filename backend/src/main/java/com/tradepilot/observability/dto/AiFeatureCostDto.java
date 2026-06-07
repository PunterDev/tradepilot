package com.tradepilot.observability.dto;

import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;

@Data
@Builder
public class AiFeatureCostDto {
    private String feature;
    private long calls;
    private long tokens;
    private BigDecimal estimatedCost;
}
