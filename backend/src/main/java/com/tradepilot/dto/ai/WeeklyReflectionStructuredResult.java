package com.tradepilot.dto.ai;

import lombok.*;

import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class WeeklyReflectionStructuredResult {
    private String summary;
    private List<String> strengths;
    private List<String> mistakes;
    private List<String> psychologyPatterns;
    private List<String> actionItems;
    private Integer coachingScore;
}
