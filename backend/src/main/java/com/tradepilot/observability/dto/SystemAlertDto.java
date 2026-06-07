package com.tradepilot.observability.dto;

import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@Builder
public class SystemAlertDto {
    private Long id;
    private String type;
    private String severity;
    private String message;
    private String status;
    private LocalDateTime createdAt;
    private LocalDateTime resolvedAt;
    private Long resolvedBy;
}
