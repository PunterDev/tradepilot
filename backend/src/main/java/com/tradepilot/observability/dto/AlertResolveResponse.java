package com.tradepilot.observability.dto;

import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@Builder
public class AlertResolveResponse {
    private Long id;
    private String status;
    private LocalDateTime resolvedAt;
    private Long resolvedBy;
}
