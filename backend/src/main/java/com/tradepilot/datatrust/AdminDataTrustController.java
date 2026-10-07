package com.tradepilot.datatrust;
import com.tradepilot.datatrust.dto.DataTrustQualityResponse;
import org.springframework.web.bind.annotation.*;
@RestController @RequestMapping("/api/admin/data-trust") public class AdminDataTrustController { private final AdminDataTrustService service; public AdminDataTrustController(AdminDataTrustService service){this.service=service;} @GetMapping("/quality") public DataTrustQualityResponse quality(){return service.quality();} }
