package com.tradepilot.dto.ai;

import lombok.*;

import java.util.List;

@Data
@Builder
public class WeeklyReflectionResponse {
    private Long reflectionId;
    private String summary;
    private List<String> strengths;
    private List<String> mistakes;
    private List<String> psychologyPatterns;
    private List<String> actionItems;
    private Integer coachingScore;
    private String rawResponseText;
}
