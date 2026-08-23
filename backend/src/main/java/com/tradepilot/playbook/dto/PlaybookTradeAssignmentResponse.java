package com.tradepilot.playbook.dto;

import com.tradepilot.playbook.enums.AssignmentSource;
import com.tradepilot.playbook.enums.ExecutionQualityGrade;
import com.tradepilot.playbook.enums.PlaybookConfidence;
import com.tradepilot.playbook.enums.SetupQualityGrade;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Map;

public record PlaybookTradeAssignmentResponse(
        Long id,
        Long tradeId,
        Long setupId,
        String setupName,
        LocalDate tradeDate,
        String symbol,
        String direction,
        BigDecimal pnl,
        BigDecimal r,
        SetupQualityGrade setupQualityGrade,
        ExecutionQualityGrade executionQualityGrade,
        Integer complianceScore,
        PlaybookConfidence complianceConfidence,
        Map<Long, String> checklistResults,
        String[] ruleViolations,
        AssignmentSource assignmentSource,
        Boolean reviewedByUser,
        String notes
) {
}
