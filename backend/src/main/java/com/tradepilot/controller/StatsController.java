package com.tradepilot.controller;

import com.tradepilot.dto.stats.*;
import com.tradepilot.entity.User;
import com.tradepilot.service.StatsService;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;

@RestController
@RequestMapping("/api/stats")
@RequiredArgsConstructor
public class StatsController {
    private final StatsService statsService;

    @GetMapping("/summary")
    public DashboardSummaryResponse summary(
            @AuthenticationPrincipal User user,
            @RequestParam(required = false) Long accountId,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate
    ) {
        return statsService.getDashboardSummary(user, accountId, startDate, endDate);
    }

    @GetMapping("/by-setup")
    public List<MetricItemResponse> bySetup(
            @AuthenticationPrincipal User user,
            @RequestParam(required = false) Long accountId,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate
    ) {
        return statsService.getStatsBySetup(user, accountId, startDate, endDate);
    }

    @GetMapping("/by-session")
    public List<MetricItemResponse> bySession(
            @AuthenticationPrincipal User user,
            @RequestParam(required = false) Long accountId,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate
    ) {
        return statsService.getStatsBySession(user, accountId, startDate, endDate);
    }

    @GetMapping("/by-symbol")
    public List<MetricItemResponse> bySymbol(
            @AuthenticationPrincipal User user,
            @RequestParam(required = false) Long accountId,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate
    ) {
        return statsService.getStatsBySymbol(user, accountId, startDate, endDate);
    }

    @GetMapping("/by-weekday")
    public List<MetricItemResponse> byWeekday(
            @AuthenticationPrincipal User user,
            @RequestParam(required = false) Long accountId,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate
    ) {
        return statsService.getStatsByWeekday(user, accountId, startDate, endDate);
    }
}
