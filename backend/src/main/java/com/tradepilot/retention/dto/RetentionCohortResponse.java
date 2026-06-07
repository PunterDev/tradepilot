package com.tradepilot.retention.dto;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class RetentionCohortResponse {
    private String cohortMonth;
    private long users;
    private double retained7d;
    private double retained30d;
}
