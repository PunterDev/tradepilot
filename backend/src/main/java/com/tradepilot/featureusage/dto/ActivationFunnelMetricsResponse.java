package com.tradepilot.featureusage.dto;

public record ActivationFunnelMetricsResponse(
        long signedUpUsers,
        long importedUsers,
        long firstInsightUsers,
        long secondAiFeatureUsers,
        long proConversionUsers,
        Double medianSignupToFirstImportMinutes,
        Double medianFirstImportToFirstInsightMinutes,
        Double medianSignupToFirstInsightMinutes,
        double signupToImportRate,
        double importToFirstInsightRate,
        double firstInsightToSecondAiRate,
        double firstInsightToProRate
) {
}
