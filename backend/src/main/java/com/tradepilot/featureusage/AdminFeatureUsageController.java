package com.tradepilot.featureusage;

import com.tradepilot.featureusage.dto.FeatureUsageSummaryResponse;
import com.tradepilot.featureusage.dto.FeatureUsageTrendsResponse;
import com.tradepilot.featureusage.dto.FeatureUsageUserResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/admin/feature-usage")
@RequiredArgsConstructor
public class AdminFeatureUsageController {
    private final FeatureUsageService featureUsageService;

    @GetMapping("/summary")
    public FeatureUsageSummaryResponse summary() {
        return featureUsageService.summary();
    }

    @GetMapping("/trends")
    public FeatureUsageTrendsResponse trends() {
        return featureUsageService.trends();
    }

    @GetMapping("/users")
    public List<FeatureUsageUserResponse> users() {
        return featureUsageService.users();
    }
}
