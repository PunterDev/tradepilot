package com.tradepilot.seoanalytics;

import com.tradepilot.seoanalytics.dto.SeoAnalyticsResponse;
import com.tradepilot.seoanalytics.dto.SeoAcquisitionEventBatchRequest;
import com.tradepilot.seoanalytics.gsc.GoogleSearchConsoleAnalyticsService;
import com.tradepilot.seoanalytics.gsc.dto.SearchConsoleAnalyticsResponse;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;

@RestController
public class SeoAnalyticsController {
    private final SeoAnalyticsService seoAnalyticsService;
    private final GoogleSearchConsoleAnalyticsService googleSearchConsoleAnalyticsService;

    public SeoAnalyticsController(SeoAnalyticsService seoAnalyticsService, GoogleSearchConsoleAnalyticsService googleSearchConsoleAnalyticsService) {
        this.seoAnalyticsService = seoAnalyticsService;
        this.googleSearchConsoleAnalyticsService = googleSearchConsoleAnalyticsService;
    }

    @PostMapping({"/api/seo/events", "/api/v1/seo/events"})
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void recordEvents(@Valid @RequestBody SeoAcquisitionEventBatchRequest request) {
        seoAnalyticsService.recordEvents(request.events());
    }

    @GetMapping({"/api/admin/analytics/seo", "/api/v1/admin/analytics/seo"})
    public SeoAnalyticsResponse dashboard(@RequestParam(required = false) LocalDate startDate,
                                          @RequestParam(required = false) LocalDate endDate) {
        return seoAnalyticsService.dashboard(startDate, endDate);
    }

    @GetMapping({"/api/admin/analytics/seo/search-console", "/api/v1/admin/analytics/seo/search-console"})
    public SearchConsoleAnalyticsResponse searchConsoleDashboard(@RequestParam(required = false) LocalDate startDate,
                                                                 @RequestParam(required = false) LocalDate endDate) {
        return googleSearchConsoleAnalyticsService.dashboard(startDate, endDate);
    }
}
