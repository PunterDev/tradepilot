package com.tradepilot.analytics;

import com.tradepilot.analytics.dto.BehavioralInsightDto;
import com.tradepilot.analytics.dto.CalendarSummaryDto;
import com.tradepilot.analytics.dto.FridayOpexAnalyticsResponse;
import com.tradepilot.analytics.dto.HeatmapCellDto;
import com.tradepilot.analytics.dto.SessionPerformanceDto;
import com.tradepilot.billing.Feature;
import com.tradepilot.billing.FeatureAccessService;
import com.tradepilot.entity.User;
import com.tradepilot.featureusage.FeatureKey;
import com.tradepilot.featureusage.FeatureUsageService;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import java.util.List;

@RestController
@RequestMapping("/api/analytics")
public class TradeAnalyticsController {
    private final TradeAnalyticsService tradeAnalyticsService;
    private final FeatureUsageService featureUsageService;
    private final FeatureAccessService featureAccessService;

    @GetMapping("/friday-opex")
    public FridayOpexAnalyticsResponse fridayOpex(@AuthenticationPrincipal User user, @RequestParam(required = false) Long accountId) {
        featureAccessService.requireFeature(user, Feature.ADVANCED_ANALYTICS);
        FridayOpexAnalyticsResponse response = tradeAnalyticsService.getFridayOpexAnalytics(user, accountId);
        featureUsageService.track(user, FeatureKey.ANALYTICS_VIEW);
        return response;
    }

    @GetMapping("/sessions")
    public List<SessionPerformanceDto> sessions(@AuthenticationPrincipal User user, @RequestParam(required = false) Long accountId) {
        featureAccessService.requireFeature(user, Feature.ADVANCED_ANALYTICS);
        List<SessionPerformanceDto> response = tradeAnalyticsService.getSessionPerformance(user, accountId);
        featureUsageService.track(user, FeatureKey.ANALYTICS_VIEW);
        return response;
    }

    @GetMapping("/heatmap")
    public List<HeatmapCellDto> heatmap(@AuthenticationPrincipal User user, @RequestParam(required = false) Long accountId) {
        featureAccessService.requireFeature(user, Feature.ADVANCED_ANALYTICS);
        List<HeatmapCellDto> response = tradeAnalyticsService.getHeatmap(user, accountId);
        featureUsageService.track(user, FeatureKey.ANALYTICS_VIEW);
        return response;
    }

    @GetMapping("/calendar-summary")
    public List<CalendarSummaryDto> calendarSummary(@AuthenticationPrincipal User user, @RequestParam(required = false) Long accountId) {
        featureAccessService.requireFeature(user, Feature.PNL_CALENDAR);
        List<CalendarSummaryDto> response = tradeAnalyticsService.getCalendarSummary(user, accountId);
        featureUsageService.track(user, FeatureKey.CALENDAR_VIEW);
        return response;
    }

    @GetMapping("/behavioral-insights")
    public List<BehavioralInsightDto> behavioralInsights(@AuthenticationPrincipal User user, @RequestParam(required = false) Long accountId) {
        featureAccessService.requireFeature(user, Feature.ADVANCED_ANALYTICS);
        List<BehavioralInsightDto> response = tradeAnalyticsService.getBehavioralInsights(user, accountId);
        featureUsageService.track(user, FeatureKey.ANALYTICS_VIEW);
        return response;
    }

    @java.lang.SuppressWarnings("all")
    public TradeAnalyticsController(final TradeAnalyticsService tradeAnalyticsService, final FeatureUsageService featureUsageService, final FeatureAccessService featureAccessService) {
        this.tradeAnalyticsService = tradeAnalyticsService;
        this.featureUsageService = featureUsageService;
        this.featureAccessService = featureAccessService;
    }
}
