package com.tradepilot.analytics;

import com.tradepilot.analytics.dto.BehavioralInsightDto;
import com.tradepilot.analytics.dto.CalendarSummaryDto;
import com.tradepilot.analytics.dto.FridayOpexAnalyticsResponse;
import com.tradepilot.analytics.dto.HeatmapCellDto;
import com.tradepilot.analytics.dto.SessionPerformanceDto;
import com.tradepilot.entity.User;
import com.tradepilot.featureusage.FeatureKey;
import com.tradepilot.featureusage.FeatureUsageService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/analytics")
@RequiredArgsConstructor
public class TradeAnalyticsController {

    private final TradeAnalyticsService tradeAnalyticsService;
    private final FeatureUsageService featureUsageService;

    @GetMapping("/friday-opex")
    public FridayOpexAnalyticsResponse fridayOpex(
            @AuthenticationPrincipal User user,
            @RequestParam(required = false) Long accountId
    ) {
        FridayOpexAnalyticsResponse response = tradeAnalyticsService.getFridayOpexAnalytics(user, accountId);
        featureUsageService.track(user, FeatureKey.ANALYTICS_VIEW);
        return response;
    }

    @GetMapping("/sessions")
    public List<SessionPerformanceDto> sessions(
            @AuthenticationPrincipal User user,
            @RequestParam(required = false) Long accountId
    ) {
        List<SessionPerformanceDto> response = tradeAnalyticsService.getSessionPerformance(user, accountId);
        featureUsageService.track(user, FeatureKey.ANALYTICS_VIEW);
        return response;
    }

    @GetMapping("/heatmap")
    public List<HeatmapCellDto> heatmap(
            @AuthenticationPrincipal User user,
            @RequestParam(required = false) Long accountId
    ) {
        List<HeatmapCellDto> response = tradeAnalyticsService.getHeatmap(user, accountId);
        featureUsageService.track(user, FeatureKey.ANALYTICS_VIEW);
        return response;
    }

    @GetMapping("/calendar-summary")
    public List<CalendarSummaryDto> calendarSummary(
            @AuthenticationPrincipal User user,
            @RequestParam(required = false) Long accountId
    ) {
        List<CalendarSummaryDto> response = tradeAnalyticsService.getCalendarSummary(user, accountId);
        featureUsageService.track(user, FeatureKey.CALENDAR_VIEW);
        return response;
    }

    @GetMapping("/behavioral-insights")
    public List<BehavioralInsightDto> behavioralInsights(
            @AuthenticationPrincipal User user,
            @RequestParam(required = false) Long accountId
    ) {
        List<BehavioralInsightDto> response = tradeAnalyticsService.getBehavioralInsights(user, accountId);
        featureUsageService.track(user, FeatureKey.ANALYTICS_VIEW);
        return response;
    }
}
