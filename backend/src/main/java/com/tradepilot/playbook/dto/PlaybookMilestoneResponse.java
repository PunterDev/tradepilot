package com.tradepilot.playbook.dto;

import java.time.LocalDate;
import java.util.List;

public record PlaybookMilestoneResponse(
        Long id,
        Long setupId,
        String milestoneType,
        LocalDate earnedDate,
        List<String> evidence
) {
}
