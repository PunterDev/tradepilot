package com.tradepilot.retention.dto;

import lombok.Builder;
import lombok.Data;

import java.util.List;

@Data
@Builder
public class RetentionTrendsResponse {
    private List<RetentionTrendPointResponse> dailyActiveUsers;
    private List<RetentionTrendPointResponse> weeklyActiveUsers;
    private List<RetentionTrendPointResponse> monthlyActiveUsers;
}
