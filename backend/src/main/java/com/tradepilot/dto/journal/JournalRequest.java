package com.tradepilot.dto.journal;

import java.time.LocalDate;

public record JournalRequest(
        Long accountId,
        LocalDate journalDate,
        String mood,
        String setupQuality,
        String mistakes,
        String lessonLearned,
        String notes
) {}