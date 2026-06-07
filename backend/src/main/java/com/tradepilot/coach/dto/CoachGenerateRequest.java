package com.tradepilot.coach.dto;

import lombok.Getter;
import lombok.Setter;

import java.time.LocalDate;

@Getter
@Setter
public class CoachGenerateRequest {

    private Long accountId;

    private LocalDate periodStart;

    private LocalDate periodEnd;

    private Boolean forceRefresh = false;
}