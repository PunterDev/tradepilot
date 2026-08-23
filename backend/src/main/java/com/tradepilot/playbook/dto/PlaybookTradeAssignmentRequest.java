package com.tradepilot.playbook.dto;

import com.tradepilot.playbook.enums.AssignmentSource;
import com.tradepilot.playbook.enums.ExecutionQualityGrade;
import com.tradepilot.playbook.enums.SetupQualityGrade;
import java.util.Map;

public record PlaybookTradeAssignmentRequest(
        SetupQualityGrade setupQualityGrade,
        ExecutionQualityGrade executionQualityGrade,
        Map<Long, String> checklistResults,
        String[] ruleViolations,
        AssignmentSource assignmentSource,
        Integer assignmentConfidence,
        Boolean reviewedByUser,
        String notes
) {
}
