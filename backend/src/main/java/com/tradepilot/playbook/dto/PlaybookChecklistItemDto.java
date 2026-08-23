package com.tradepilot.playbook.dto;

import com.tradepilot.playbook.enums.ChecklistType;

public record PlaybookChecklistItemDto(
        Long id,
        ChecklistType checklistType,
        String label,
        String description,
        Boolean required,
        Integer weight,
        Integer displayOrder,
        Boolean active,
        String expectedValueType,
        Object expectedValue
) {
}
