package com.tradepilot.dto.contact;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record ContactRequest(
        @NotBlank(message = "Name is required")
        @Size(max = 120, message = "Name must be 120 characters or less")
        String name,

        @NotBlank(message = "Email is required")
        @Email(message = "Email must be valid")
        @Size(max = 180, message = "Email must be 180 characters or less")
        String email,

        @NotBlank(message = "Subject is required")
        @Size(max = 180, message = "Subject must be 180 characters or less")
        String subject,

        @NotBlank(message = "Message is required")
        @Size(max = 5000, message = "Message must be 5000 characters or less")
        String message
) {
}
