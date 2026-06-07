package com.tradepilot.observability.dto;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class EndpointMetricDto {
    private String endpoint;
    private long requestCount;
    private double avgDurationMs;
    private long maxDurationMs;
}
