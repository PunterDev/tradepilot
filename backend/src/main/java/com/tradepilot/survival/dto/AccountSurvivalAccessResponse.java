package com.tradepilot.survival.dto;

public record AccountSurvivalAccessResponse(
        String rolloutMode,
        boolean rolloutAvailable,
        boolean premiumAccess,
        boolean readOnly,
        boolean upgradeRequired,
        String message) {
}
