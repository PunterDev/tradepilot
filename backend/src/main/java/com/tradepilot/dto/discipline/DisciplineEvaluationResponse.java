package com.tradepilot.dto.discipline;

import lombok.*;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DisciplineEvaluationResponse {
    private Long tradeId;
    private String tradeDate;
    private Integer disciplineScore;
    private Boolean cleanTrade;
    private Integer violationCount;
    private List<RuleViolationDto> violations;
}
