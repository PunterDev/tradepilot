package com.tradepilot.coach.dto;

import lombok.*;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CoachAiResponse {

    private String headline;
    private String emotionalPattern;
    private String bestBehavior;
    private String biggestMistake;
    private String bestSetup;
    private String avoidNextSession;
    private String nextSessionFocus;
    private String riskNote;
    private String disciplineNote;
    private String oneRuleForNextSession;
}