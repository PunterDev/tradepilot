package com.tradepilot.marketdata.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.time.LocalDate;

public record MarketDataDownloadRequest(
        @NotBlank String instrument,
        @NotNull LocalDate startDate,
        @NotNull LocalDate endDate
) {
}
