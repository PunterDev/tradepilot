package com.tradepilot.dto.journal;

import lombok.Builder;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Builder
public record JournalResponse(
        Long id,
        Long accountId,
        LocalDate journalDate,
        String mood,
        String setupQuality,
        String mistakes,
        String lessonLearned,
        String notes,
        LocalDateTime createdAt,
        LocalDateTime updatedAt
) {}