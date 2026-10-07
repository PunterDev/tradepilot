package com.tradepilot.datatrust.dto;

import com.tradepilot.datatrust.InsightFeedbackReason;
import jakarta.validation.constraints.NotNull;

public record InsightFeedbackRequest(@NotNull Boolean helpful, InsightFeedbackReason reason, String comment) {}
