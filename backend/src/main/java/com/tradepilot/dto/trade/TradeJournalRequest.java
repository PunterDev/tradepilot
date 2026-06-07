package com.tradepilot.dto.trade;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Size;

public record TradeJournalRequest(
        @Size(max = 50, message = "Setup must be 50 characters or less")
        String setup,
        @Min(value = 1, message = "Confidence must be between 1 and 5")
        @Max(value = 5, message = "Confidence must be between 1 and 5")
        Integer confidence,
        @Size(max = 50, message = "Emotion must be 50 characters or less")
        String emotion,
        @Size(max = 1000, message = "Notes must be 1000 characters or less")
        String notes
) {}
