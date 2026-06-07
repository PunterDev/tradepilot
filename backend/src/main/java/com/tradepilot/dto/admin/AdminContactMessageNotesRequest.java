package com.tradepilot.dto.admin;

import jakarta.validation.constraints.Size;

public record AdminContactMessageNotesRequest(
        @Size(max = 5000, message = "Admin notes must be 5000 characters or less")
        String adminNotes
) {
}
