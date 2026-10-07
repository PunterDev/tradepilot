package com.tradepilot.datatrust.dto;

import java.util.List;
import java.util.Map;

public record DataTrustQualityResponse(ImportTrust importTrust, InsightTrust insightTrust) {
    public record ImportTrust(long total, long reviewed, long matched, long mismatched, long notReviewed, Double userConfirmedMatchRate, List<RateRow> bySource, Map<String, Long> mismatchReasons) {}
    public record InsightTrust(long rated, long positive, long negative, Double positiveRate, List<RateRow> byFindingType, List<RateRow> byEvidenceStrength, List<RateRow> byVersion, Map<String, Long> negativeReasons) {}
    public record RateRow(String key, long rated, long positive, long negative, Double rate) {}
}
