package com.tradepilot.dto.ai;

import lombok.Builder;
import lombok.Data;

import java.util.List;

@Data
@Builder
public class AiTradeReviewResponse {
    private String summary;
    private List<String> mistakes;
    private List<String> strengths;
    private List<String> improvementPlan;
    private List<String> tags;
    private String grade;
}