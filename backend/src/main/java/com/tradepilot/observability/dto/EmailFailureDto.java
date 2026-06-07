package com.tradepilot.observability.dto;

import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@Builder
public class EmailFailureDto {
    private String recipient;
    private String template;
    private String errorMessage;
    private LocalDateTime createdAt;
}
