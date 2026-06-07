package com.tradepilot.observability.dto;

import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@Builder
public class StripeEventDto {
    private String eventType;
    private boolean success;
    private long durationMs;
    private String errorMessage;
    private LocalDateTime createdAt;
}
