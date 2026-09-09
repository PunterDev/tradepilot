package com.tradepilot.execution.controller;

import com.tradepilot.entity.User;
import com.tradepilot.execution.dto.*;
import com.tradepilot.execution.service.ExecutionEngineService;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;
import java.util.List;

@RestController
@RequestMapping({"/api/execution", "/api/v1/execution"})
public class ExecutionEngineController {
    private final ExecutionEngineService executionEngineService;

    public ExecutionEngineController(ExecutionEngineService executionEngineService) {
        this.executionEngineService = executionEngineService;
    }

    @GetMapping("/trades/{tradeId}")
    public ExecutionScoreResponse tradeExecution(@AuthenticationPrincipal User user, @PathVariable Long tradeId) {
        return executionEngineService.tradeExecution(user, tradeId);
    }

    @GetMapping("/accounts/{accountId}/summary")
    public ExecutionAccountSummaryResponse accountSummary(@AuthenticationPrincipal User user, @PathVariable Long accountId) {
        return executionEngineService.accountSummary(user, accountId);
    }

    @GetMapping("/accounts/{accountId}/sessions/{date}")
    public ExecutionSessionSummaryResponse session(@AuthenticationPrincipal User user, @PathVariable Long accountId, @PathVariable @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date) {
        return executionEngineService.session(user, accountId, date);
    }

    @GetMapping("/accounts/{accountId}/loss-response")
    public List<LossResponseResponse> lossResponse(@AuthenticationPrincipal User user, @PathVariable Long accountId) {
        return executionEngineService.lossResponses(user, accountId);
    }

    @GetMapping("/accounts/{accountId}/patterns")
    public List<BehaviorPatternResponse> patterns(@AuthenticationPrincipal User user, @PathVariable Long accountId) {
        return executionEngineService.patterns(user, accountId);
    }

    @GetMapping("/accounts/{accountId}/pressure-points")
    public PressurePointResponse pressurePoints(@AuthenticationPrincipal User user, @PathVariable Long accountId) {
        return executionEngineService.pressurePoint(user, accountId);
    }

    @GetMapping("/accounts/{accountId}/setup-stats")
    public List<SetupExecutionStatsResponse> setupStats(@AuthenticationPrincipal User user, @PathVariable Long accountId) {
        return executionEngineService.setupStats(user, accountId);
    }

    @GetMapping("/accounts/{accountId}/weekly-focus")
    public WeeklyExecutionFocusResponse weeklyFocus(@AuthenticationPrincipal User user, @PathVariable Long accountId) {
        return executionEngineService.weeklyFocus(user, accountId);
    }
}
