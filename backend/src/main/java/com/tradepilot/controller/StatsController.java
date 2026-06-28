package com.tradepilot.controller;

import com.tradepilot.billing.Feature;
import com.tradepilot.billing.FeatureAccessService;
import com.tradepilot.dto.stats.*;
import com.tradepilot.entity.User;
import com.tradepilot.service.StatsService;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import java.time.LocalDate;
import java.util.List;

@RestController
@RequestMapping("/api/stats")
public class StatsController {
    private final StatsService statsService;
    private final FeatureAccessService featureAccessService;

    @GetMapping("/summary")
    public DashboardSummaryResponse summary(@AuthenticationPrincipal User user, @RequestParam(required = false) Long accountId, @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate, @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate) {
        featureAccessService.requireFeature(user, Feature.BASIC_ANALYTICS);
        return statsService.getDashboardSummary(user, accountId, startDate, endDate);
    }

    @GetMapping("/by-setup")
    public List<MetricItemResponse> bySetup(@AuthenticationPrincipal User user, @RequestParam(required = false) Long accountId, @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate, @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate) {
        featureAccessService.requireFeature(user, Feature.BASIC_ANALYTICS);
        return statsService.getStatsBySetup(user, accountId, startDate, endDate);
    }

    @GetMapping("/by-session")
    public List<MetricItemResponse> bySession(@AuthenticationPrincipal User user, @RequestParam(required = false) Long accountId, @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate, @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate) {
        featureAccessService.requireFeature(user, Feature.BASIC_ANALYTICS);
        return statsService.getStatsBySession(user, accountId, startDate, endDate);
    }

    @GetMapping("/by-symbol")
    public List<MetricItemResponse> bySymbol(@AuthenticationPrincipal User user, @RequestParam(required = false) Long accountId, @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate, @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate) {
        featureAccessService.requireFeature(user, Feature.BASIC_ANALYTICS);
        return statsService.getStatsBySymbol(user, accountId, startDate, endDate);
    }

    @GetMapping("/by-weekday")
    public List<MetricItemResponse> byWeekday(@AuthenticationPrincipal User user, @RequestParam(required = false) Long accountId, @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate, @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate) {
        featureAccessService.requireFeature(user, Feature.BASIC_ANALYTICS);
        return statsService.getStatsByWeekday(user, accountId, startDate, endDate);
    }

    @java.lang.SuppressWarnings("all")
    public StatsController(final StatsService statsService, final FeatureAccessService featureAccessService) {
        this.statsService = statsService;
        this.featureAccessService = featureAccessService;
    }
}
