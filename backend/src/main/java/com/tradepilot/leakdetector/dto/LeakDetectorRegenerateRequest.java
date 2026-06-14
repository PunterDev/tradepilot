package com.tradepilot.leakdetector.dto;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class LeakDetectorRegenerateRequest {
    @NotNull
    private Long accountId;
}
