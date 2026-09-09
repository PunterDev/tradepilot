package com.tradepilot.preanalysis.dto;

public record PreAnalysisAccessResponse(boolean allowed, String entitlementKey, String label, String badge) {
    public static PreAnalysisAccessResponse beta(boolean allowed) {
        return new PreAnalysisAccessResponse(allowed, "AI_PRE_ANALYSIS_BETA", "AI Pre-Analysis", "Private Beta");
    }
}
