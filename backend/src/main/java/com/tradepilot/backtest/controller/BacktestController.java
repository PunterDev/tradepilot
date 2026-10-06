package com.tradepilot.backtest.controller;

import com.tradepilot.backtest.dto.BacktestAccessResponse;
import com.tradepilot.backtest.model.BacktestConfig;
import com.tradepilot.backtest.service.BacktestService;
import com.tradepilot.entity.User;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping({"/api/backtests", "/api/v1/backtests"})
public class BacktestController {
    private final BacktestService service;

    public BacktestController(BacktestService service) {
        this.service = service;
    }

    @GetMapping("/access")
    public BacktestAccessResponse access(@AuthenticationPrincipal User user) {
        return service.access(user);
    }

    @PostMapping
    public Object start(@AuthenticationPrincipal User user, @RequestBody BacktestConfig config) {
        return service.start(user, config);
    }

    @GetMapping
    public Object list(@AuthenticationPrincipal User user) {
        return service.list(user);
    }

    @GetMapping("/{id}")
    public Object get(@AuthenticationPrincipal User user, @PathVariable Long id) {
        return service.get(user, id);
    }

    @GetMapping("/{id}/trades")
    public Object trades(@AuthenticationPrincipal User user, @PathVariable Long id) {
        return service.trades(user, id);
    }

    @GetMapping("/{id}/trades/{tradeId}/chart")
    public Object chart(@AuthenticationPrincipal User user, @PathVariable Long id, @PathVariable Long tradeId,
                        @RequestParam(defaultValue = "1m") String timeframe) {
        return service.chart(user, id, tradeId, timeframe);
    }
}
