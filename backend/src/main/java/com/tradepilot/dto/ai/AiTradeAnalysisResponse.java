package com.tradepilot.dto.ai;

import lombok.Data;

import com.tradepilot.dto.discipline.RuleViolationDto;
import java.util.List;

@Data
public class AiTradeAnalysisResponse {
    private Long id;
    private Long tradeId;
    private String status;
    private String reviewVersion;
    private String reviewHash;

    private String summary;
    private String grade;
    private Integer executionScore;
    private Integer disciplineScore;
    private Integer riskScore;

    private String marketContext;
    private String entryAnalysis;
    private String exitAnalysis;
    private String riskAnalysis;
    private String psychologyAnalysis;
    private String chartAnalysis;

    private List<String> mistakes;
    private List<String> strengths;
    private List<String> improvementPlan;
    private List<String> tags;
    private List<RuleViolationDto> violations;
    private Integer currentDisciplineStreak;
    private Boolean cleanTrade;

    private String errorMessage;
}
