package com.tradepilot.observability.dto;

import lombok.Builder;
import lombok.Data;

import java.time.LocalDate;

@Data
@Builder
public class ApiTrendPointDto {
    private LocalDate date;
    private long requests;
    private double avgDurationMs;
    private double errorRate;
}
