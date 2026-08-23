package com.tradepilot.dev;

import com.tradepilot.entity.User;
import com.tradepilot.tradereplay.dto.TradeReplayResetResult;
import com.tradepilot.tradereplay.service.TradeReplayService;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;
import java.util.Map;

@RestController
@RequestMapping("/api/dev")
public class DevToolsController {
    private final DevToolsAvailability availability;
    private final DevDemoDataService demoDataService;
    private final TradeReplayService tradeReplayService;

    @PostMapping("/seed-demo-data")
    public Map<String, Object> seedDemoData(@AuthenticationPrincipal User user) {
        assertEnabled();
        return demoDataService.seed(user);
    }

    @PostMapping("/clear-demo-data")
    public Map<String, Object> clearDemoData(@AuthenticationPrincipal User user) {
        assertEnabled();
        return demoDataService.clear(user);
    }

    @PostMapping("/demo-data/reseed")
    public DemoDataReseedResult reseedDemoData(
            @AuthenticationPrincipal User user,
            @RequestParam(defaultValue = "true") boolean dryRun
    ) {
        assertEnabled();
        return demoDataService.reseedDemoFixtures(user, dryRun);
    }

    @DeleteMapping("/trades/{tradeId}/replay")
    public TradeReplayResetResult resetTradeReplay(@PathVariable Long tradeId, @AuthenticationPrincipal User user) {
        assertEnabled();
        return tradeReplayService.resetReplayForDev(user, tradeId);
    }

    @PostMapping("/mock-upgrade-pro")
    public Map<String, Object> mockUpgradePro(@AuthenticationPrincipal User user) {
        assertEnabled();
        return demoDataService.mockUpgradePro(user);
    }

    @PostMapping("/mock-downgrade-free")
    public Map<String, Object> mockDowngradeFree(@AuthenticationPrincipal User user) {
        assertEnabled();
        return demoDataService.mockDowngradeFree(user);
    }

    @PostMapping("/feature-usage/backfill")
    public Map<String, Object> backfillFeatureUsage(@AuthenticationPrincipal User user) {
        assertEnabled();
        return demoDataService.backfillFeatureUsage(user);
    }

    private void assertEnabled() {
        if (!availability.enabled()) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Dev tools are disabled.");
        }
    }

    @java.lang.SuppressWarnings("all")
    public DevToolsController(final DevToolsAvailability availability, final DevDemoDataService demoDataService, final TradeReplayService tradeReplayService) {
        this.availability = availability;
        this.demoDataService = demoDataService;
        this.tradeReplayService = tradeReplayService;
    }
}
