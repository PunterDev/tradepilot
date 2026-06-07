package com.tradepilot.dto.admin;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record AdminContactMessageReplyRequest(
        @NotBlank(message = "Subject is required")
        @Size(max = 180, message = "Subject must be 180 characters or less")
        String subject,

        @NotBlank(message = "Message is required")
        @Size(max = 5000, message = "Message must be 5000 characters or less")
        String message
) {
}
