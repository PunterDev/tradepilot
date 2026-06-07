package com.tradepilot.dto.session;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.time.LocalDate;

@Data
public class GenerateSessionReviewRequest {
    @NotNull
    private Long accountId;

    @NotNull
    private LocalDate date;

    private Boolean forceRefresh = false;
}
