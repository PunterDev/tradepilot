package com.tradepilot.controller;

import com.tradepilot.dto.admin.AdminBillingSummaryResponse;
import com.tradepilot.service.AdminBillingService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/admin/billing")
@RequiredArgsConstructor
public class AdminBillingController {
    private final AdminBillingService adminBillingService;

    @GetMapping("/summary")
    public AdminBillingSummaryResponse summary() {
        return adminBillingService.summary();
    }
}
