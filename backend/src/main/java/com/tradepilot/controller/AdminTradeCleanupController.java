package com.tradepilot.controller;

import com.tradepilot.dto.admin.AdminTradeCleanupResponse;
import com.tradepilot.entity.User;
import com.tradepilot.service.AdminTradeCleanupService;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import java.time.LocalDate;

@RestController
@RequestMapping("/api/admin/trades")
public class AdminTradeCleanupController {
    private final AdminTradeCleanupService cleanupService;

    @DeleteMapping("/cleanup-range")
    public AdminTradeCleanupResponse cleanupRange(@AuthenticationPrincipal User admin, @RequestParam Long userId, @RequestParam Long accountId, @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate, @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate, @RequestParam(defaultValue = "true") boolean dryRun) {
        return cleanupService.cleanupRange(admin, userId, accountId, startDate, endDate, dryRun);
    }

    @java.lang.SuppressWarnings("all")
    public AdminTradeCleanupController(final AdminTradeCleanupService cleanupService) {
        this.cleanupService = cleanupService;
    }
}
