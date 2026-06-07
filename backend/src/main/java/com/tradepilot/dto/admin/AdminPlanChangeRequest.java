package com.tradepilot.dto.admin;

import jakarta.validation.constraints.NotBlank;

import java.time.LocalDateTime;

public record AdminPlanChangeRequest(
        @NotBlank(message = "Reason is required") String reason,
        LocalDateTime proUntil
) {
}
