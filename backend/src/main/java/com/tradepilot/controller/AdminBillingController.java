package com.tradepilot.controller;

import com.tradepilot.dto.admin.AdminBillingSummaryResponse;
import com.tradepilot.service.AdminBillingService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/admin/billing")
public class AdminBillingController {
    private final AdminBillingService adminBillingService;

    @GetMapping("/summary")
    public AdminBillingSummaryResponse summary() {
        return adminBillingService.summary();
    }

    @java.lang.SuppressWarnings("all")
    public AdminBillingController(final AdminBillingService adminBillingService) {
        this.adminBillingService = adminBillingService;
    }
}
