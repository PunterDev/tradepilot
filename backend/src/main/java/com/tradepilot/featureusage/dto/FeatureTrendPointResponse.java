package com.tradepilot.featureusage.dto;

import lombok.Builder;
import lombok.Data;

import java.time.LocalDate;

@Data
@Builder
public class FeatureTrendPointResponse {
    private LocalDate date;
    private long count;
}
