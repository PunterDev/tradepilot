package com.tradepilot.featureusage.dto;

import java.time.LocalDate;

public record FeatureTrendPointResponse(LocalDate date, long count) {
}
