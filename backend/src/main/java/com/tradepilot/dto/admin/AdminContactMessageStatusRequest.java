package com.tradepilot.dto.admin;

import com.tradepilot.enums.ContactMessageStatus;
import jakarta.validation.constraints.NotNull;

public record AdminContactMessageStatusRequest(@NotNull ContactMessageStatus status) {
}
