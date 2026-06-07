package com.tradepilot.observability.dto;

import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@Builder
public class SlowRequestDto {
    private String endpoint;
    private long durationMs;
    private int status;
    private Long userId;
    private String severity;
    private LocalDateTime createdAt;
}
