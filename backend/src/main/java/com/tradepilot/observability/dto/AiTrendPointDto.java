package com.tradepilot.observability.dto;

import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;

@Data
@Builder
public class AiTrendPointDto {
    private LocalDate date;
    private long tokens;
    private BigDecimal estimatedCost;
}
