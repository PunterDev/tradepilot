package com.tradepilot.dto.discipline;

import lombok.*;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RuleViolationDto {
    private Long id;
    private Long tradeId;
    private String ruleCode;
    private String ruleTitle;
    private String category;
    private String severity;
    private String reason;
    private String source;
}
