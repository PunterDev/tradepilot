package com.tradepilot.observability;

import java.time.LocalDate;
import java.time.LocalDateTime;

public record ObservabilityDateRange(LocalDateTime start, LocalDateTime end, String cacheKey) {
    public static ObservabilityDateRange from(String range, LocalDate startDate, LocalDate endDate) {
        LocalDate today = LocalDate.now();
        String normalized = range == null || range.isBlank() ? "TODAY" : range.trim().toUpperCase();

        LocalDate start;
        LocalDate endInclusive;
        switch (normalized) {
            case "LAST_7_DAYS" -> {
                start = today.minusDays(6);
                endInclusive = today;
            }
            case "LAST_30_DAYS" -> {
                start = today.minusDays(29);
                endInclusive = today;
            }
            case "CUSTOM" -> {
                if (startDate == null || endDate == null) {
                    throw new IllegalArgumentException("Custom date range requires startDate and endDate.");
                }
                if (endDate.isBefore(startDate)) {
                    throw new IllegalArgumentException("endDate must be on or after startDate.");
                }
                start = startDate;
                endInclusive = endDate;
            }
            case "TODAY" -> {
                start = today;
                endInclusive = today;
            }
            default -> throw new IllegalArgumentException("Unsupported observability date range: " + range);
        }

        LocalDateTime startAt = start.atStartOfDay();
        LocalDateTime endExclusive = endInclusive.plusDays(1).atStartOfDay();
        return new ObservabilityDateRange(startAt, endExclusive, normalized + ":" + start + ":" + endInclusive);
    }
}
