package com.tradepilot.retention.dto;

import lombok.Builder;
import lombok.Data;

import java.time.LocalDate;

@Data
@Builder
public class RetentionTrendPointResponse {
    private LocalDate date;
    private long count;
}
