package com.tradepilot.observability;

import com.tradepilot.entity.User;
import com.tradepilot.observability.dto.AlertResolveResponse;
import com.tradepilot.observability.dto.ObservabilitySummaryResponse;
import com.tradepilot.observability.dto.ObservabilityTrendsResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;

@RestController
@RequestMapping("/api/admin/observability")
@RequiredArgsConstructor
@PreAuthorize("hasRole('ADMIN')")
public class AdminObservabilityController {
    private final ObservabilityService observabilityService;
    private final ObservabilityAlertService alertService;

    @GetMapping("/summary")
    public ObservabilitySummaryResponse summary(
            @RequestParam(defaultValue = "TODAY") String range,
            @RequestParam(required = false) LocalDate startDate,
            @RequestParam(required = false) LocalDate endDate
    ) {
        return observabilityService.summary(ObservabilityDateRange.from(range, startDate, endDate));
    }

    @GetMapping("/trends")
    public ObservabilityTrendsResponse trends(
            @RequestParam(defaultValue = "LAST_30_DAYS") String range,
            @RequestParam(required = false) LocalDate startDate,
            @RequestParam(required = false) LocalDate endDate
    ) {
        return observabilityService.trends(ObservabilityDateRange.from(range, startDate, endDate));
    }

    @PatchMapping("/alerts/{id}/resolve")
    public AlertResolveResponse resolveAlert(@PathVariable Long id, @AuthenticationPrincipal User admin) {
        return alertService.resolveAlert(id, admin);
    }
}
