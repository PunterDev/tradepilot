package com.tradepilot.dto.auth;

import jakarta.validation.constraints.NotBlank;

public record TokenRequest(@NotBlank String token) {
    @Override
    public String toString() {
        return "TokenRequest[token=[REDACTED]]";
    }
}
