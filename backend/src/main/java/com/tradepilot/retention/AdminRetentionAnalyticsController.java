package com.tradepilot.retention;

import com.tradepilot.retention.dto.*;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import java.util.List;

@RestController
@RequestMapping("/api/admin/retention")
public class AdminRetentionAnalyticsController {
    private final AdminRetentionAnalyticsService retentionAnalyticsService;

    @GetMapping("/summary")
    public RetentionSummaryResponse summary() {
        return retentionAnalyticsService.summary();
    }

    @GetMapping("/trends")
    public RetentionTrendsResponse trends() {
        return retentionAnalyticsService.trends();
    }

    @GetMapping("/cohorts")
    public List<RetentionCohortResponse> cohorts() {
        return retentionAnalyticsService.cohorts();
    }

    @GetMapping("/users")
    public List<RetentionUserResponse> users() {
        return retentionAnalyticsService.users();
    }

    @java.lang.SuppressWarnings("all")
    public AdminRetentionAnalyticsController(final AdminRetentionAnalyticsService retentionAnalyticsService) {
        this.retentionAnalyticsService = retentionAnalyticsService;
    }
}
