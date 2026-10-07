package com.tradepilot.datatrust.dto;

import com.tradepilot.datatrust.InsightFeedbackReason;
import java.time.LocalDateTime;

public record InsightFeedbackResponse(Long snapshotId, String findingType, boolean helpful, InsightFeedbackReason reason, LocalDateTime updatedAt) {}
