package com.tradepilot.featurefunnel;

import com.tradepilot.billing.PlanType;
import com.tradepilot.featurefunnel.dto.*;
import com.tradepilot.featureusage.FeatureUsageCategory;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;

@RestController
@RequestMapping({"/api/admin/feature-funnels", "/api/v1/admin/feature-funnels"})
public class AdminFeatureFunnelController {
    private final FeatureFunnelService featureFunnelService;

    @GetMapping
    public FeatureFunnelDashboardResponse dashboard(@RequestParam(required = false) String funnel,
                                                    @RequestParam(required = false) String range,
                                                    @RequestParam(required = false) LocalDate startDate,
                                                    @RequestParam(required = false) LocalDate endDate,
                                                    @RequestParam(required = false) PlanType plan,
                                                    @RequestParam(required = false) FeatureUsageCategory category,
                                                    @RequestParam(required = false) Boolean excludeAdmins,
                                                    @RequestParam(required = false) Boolean includeTestUsers,
                                                    @RequestParam(required = false) FeatureFunnelCalculationMode calculationMode,
                                                    @RequestParam(required = false) Integer conversionWindow) {
        return featureFunnelService.dashboard(funnel, range, startDate, endDate, plan, category, excludeAdmins, includeTestUsers, calculationMode, conversionWindow);
    }

    @GetMapping("/{funnelKey}")
    public FeatureFunnelSummaryResponse detail(@PathVariable FeatureFunnelKey funnelKey,
                                               @RequestParam(required = false) String range,
                                               @RequestParam(required = false) LocalDate startDate,
                                               @RequestParam(required = false) LocalDate endDate,
                                               @RequestParam(required = false) PlanType plan,
                                               @RequestParam(required = false) FeatureUsageCategory category,
                                               @RequestParam(required = false) Boolean excludeAdmins,
                                               @RequestParam(required = false) Boolean includeTestUsers,
                                               @RequestParam(required = false) FeatureFunnelCalculationMode calculationMode,
                                               @RequestParam(required = false) Integer conversionWindow) {
        return featureFunnelService.detail(funnelKey, range, startDate, endDate, plan, category, excludeAdmins, includeTestUsers, calculationMode, conversionWindow);
    }

    @GetMapping("/{funnelKey}/users")
    public List<FeatureFunnelUserDrilldownResponse> users(@PathVariable FeatureFunnelKey funnelKey,
                                                          @RequestParam(required = false) String range,
                                                          @RequestParam(required = false) LocalDate startDate,
                                                          @RequestParam(required = false) LocalDate endDate,
                                                          @RequestParam(required = false) PlanType plan,
                                                          @RequestParam(required = false) Boolean excludeAdmins,
                                                          @RequestParam(required = false) Boolean includeTestUsers,
                                                          @RequestParam(defaultValue = "0") int page,
                                                          @RequestParam(defaultValue = "25") int size) {
        return featureFunnelService.users(funnelKey, range, startDate, endDate, plan, excludeAdmins, includeTestUsers, page, size);
    }

    public AdminFeatureFunnelController(FeatureFunnelService featureFunnelService) {
        this.featureFunnelService = featureFunnelService;
    }
}
