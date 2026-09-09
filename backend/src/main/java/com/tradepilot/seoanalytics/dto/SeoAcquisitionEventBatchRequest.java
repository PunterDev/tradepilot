package com.tradepilot.seoanalytics.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Size;

import java.util.List;

public record SeoAcquisitionEventBatchRequest(
        @Valid @NotEmpty @Size(max = 25) List<SeoAcquisitionEventRequest> events
) {
}
