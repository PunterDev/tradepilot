package com.tradepilot.controller;

import com.tradepilot.dto.admin.AdminContactMessageResponse;
import com.tradepilot.dto.admin.AdminDashboardSummaryResponse;
import com.tradepilot.dto.admin.AdminUsageSummaryResponse;
import com.tradepilot.dto.admin.AdminUserResponse;
import com.tradepilot.service.AdminDashboardService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import java.util.List;

@RestController
@RequestMapping("/api/admin/dashboard")
public class AdminDashboardController {
    private final AdminDashboardService adminDashboardService;

    @GetMapping("/summary")
    public AdminDashboardSummaryResponse summary() {
        return adminDashboardService.summary();
    }

    @GetMapping("/users/recent")
    public List<AdminUserResponse> recentUsers() {
        return adminDashboardService.recentUsers();
    }

    @GetMapping("/contact-messages/recent")
    public List<AdminContactMessageResponse> recentContactMessages() {
        return adminDashboardService.recentContactMessages();
    }

    @GetMapping("/usage")
    public AdminUsageSummaryResponse usage() {
        return adminDashboardService.usage();
    }

    @java.lang.SuppressWarnings("all")
    public AdminDashboardController(final AdminDashboardService adminDashboardService) {
        this.adminDashboardService = adminDashboardService;
    }
}
