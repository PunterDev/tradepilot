package com.tradepilot.controller;

import com.tradepilot.dto.ai.AiTradeAnalysisJobResponse;
import com.tradepilot.entity.User;
import com.tradepilot.service.AiTradeAnalysisService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/api/ai/trade-analysis")
@RequiredArgsConstructor
public class AiTradeAnalysisController {

    private final AiTradeAnalysisService service;

    @PostMapping(value = "/async", consumes = "multipart/form-data")
    public AiTradeAnalysisJobResponse startAsync(
            @AuthenticationPrincipal User user,
            @RequestPart("request") String requestJson,
            @RequestPart(value = "chartImage", required = false) MultipartFile chartImage
    ) {
        return service.startAsync(user, requestJson, chartImage);
    }

    @GetMapping("/status/{analysisId}")
    public AiTradeAnalysisJobResponse getStatus(
            @AuthenticationPrincipal User user,
            @PathVariable Long analysisId
    ) {
        return service.getStatus(user, analysisId);
    }
}
