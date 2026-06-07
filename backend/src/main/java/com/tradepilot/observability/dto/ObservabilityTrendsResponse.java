package com.tradepilot.observability.dto;

import lombok.Data;

import java.util.List;

@Data
public class ObservabilityTrendsResponse {
    private List<ApiTrendPointDto> apiPerformance;
    private List<AiTrendPointDto> aiCosts;
}
