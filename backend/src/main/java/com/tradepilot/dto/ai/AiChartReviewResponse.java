package com.tradepilot.dto.ai;

import lombok.Data;

import java.util.List;

@Data
public class AiChartReviewResponse {
    private String summary;
    private String bias;
    private String entryQuality;
    private String stopLossQuality;
    private String takeProfitQuality;
    private String riskManagement;
    private String grade;

    private List<String> mistakes;
    private List<String> strengths;
    private List<String> improvementPlan;
    private List<String> tags;
}