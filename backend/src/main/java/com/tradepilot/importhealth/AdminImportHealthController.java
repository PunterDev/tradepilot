package com.tradepilot.importhealth;

import com.tradepilot.importhealth.dto.AdminImportHealthSummaryResponse;
import com.tradepilot.importhealth.dto.AdminRecentImportOperationResponse;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/admin/import-health")
public class AdminImportHealthController {
    private final AdminImportHealthService service;
    public AdminImportHealthController(AdminImportHealthService service) { this.service = service; }
    @GetMapping("/summary") public AdminImportHealthSummaryResponse summary(@RequestParam(defaultValue = "LAST_7_DAYS") AdminImportHealthService.Range range) { return service.summary(range); }
    @GetMapping("/recent") public List<AdminRecentImportOperationResponse> recent(@RequestParam(defaultValue = "LAST_7_DAYS") AdminImportHealthService.Range range) { return service.recent(range); }
}
