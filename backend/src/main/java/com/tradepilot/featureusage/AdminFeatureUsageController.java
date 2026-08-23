package com.tradepilot.featureusage;

import com.tradepilot.featureusage.dto.FeatureUsageSummaryResponse;
import com.tradepilot.featureusage.dto.FeatureUsageTrendsResponse;
import com.tradepilot.featureusage.dto.FeatureUsageUserResponse;
import org.springframework.web.bind.annotation.*;
import com.tradepilot.billing.PlanType;
import java.time.LocalDate;
import java.util.List;

@RestController
@RequestMapping("/api/admin/feature-usage")
public class AdminFeatureUsageController {
    private final FeatureUsageService featureUsageService;

    @GetMapping("/summary")
    public FeatureUsageSummaryResponse summary(@RequestParam(required = false) String range,
                                               @RequestParam(required = false) LocalDate startDate,
                                               @RequestParam(required = false) LocalDate endDate,
                                               @RequestParam(required = false) PlanType plan,
                                               @RequestParam(required = false) FeatureUsageCategory category,
                                               @RequestParam(required = false) FeatureUsageEventType eventType,
                                               @RequestParam(required = false) Boolean excludeAdmins,
                                               @RequestParam(required = false) String activity) {
        return featureUsageService.summary(range, startDate, endDate, plan, category, eventType, excludeAdmins, activity);
    }

    @GetMapping("/trends")
    public FeatureUsageTrendsResponse trends(@RequestParam(required = false) String range,
                                             @RequestParam(required = false) LocalDate startDate,
                                             @RequestParam(required = false) LocalDate endDate,
                                             @RequestParam(required = false) PlanType plan,
                                             @RequestParam(required = false) FeatureUsageCategory category,
                                             @RequestParam(required = false) FeatureUsageEventType eventType,
                                             @RequestParam(required = false) Boolean excludeAdmins,
                                             @RequestParam(required = false) String activity) {
        return featureUsageService.trends(range, startDate, endDate, plan, category, eventType, excludeAdmins, activity);
    }

    @GetMapping("/users")
    public List<FeatureUsageUserResponse> users(@RequestParam(required = false) String range,
                                                @RequestParam(required = false) LocalDate startDate,
                                                @RequestParam(required = false) LocalDate endDate,
                                                @RequestParam(required = false) PlanType plan,
                                                @RequestParam(required = false) FeatureUsageCategory category,
                                                @RequestParam(required = false) FeatureUsageEventType eventType,
                                                @RequestParam(required = false) Boolean excludeAdmins,
                                                @RequestParam(required = false) String activity) {
        return featureUsageService.users(range, startDate, endDate, plan, category, eventType, excludeAdmins, activity);
    }

    @java.lang.SuppressWarnings("all")
    public AdminFeatureUsageController(final FeatureUsageService featureUsageService) {
        this.featureUsageService = featureUsageService;
    }
}
