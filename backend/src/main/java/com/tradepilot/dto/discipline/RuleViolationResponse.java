package com.tradepilot.dto.discipline;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class RuleViolationResponse {

    private String ruleCode;

    private String ruleTitle;

    private String severity;

    private String reason;

    private String source;
}