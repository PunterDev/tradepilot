package com.tradepilot.controller;

import com.tradepilot.dto.ai.AiChartReviewResponse;
import com.tradepilot.entity.User;
import com.tradepilot.service.AiChartReviewService;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/api/ai/chart-review")
public class AiChartReviewController {
    private final AiChartReviewService aiChartReviewService;

    @PostMapping
    public AiChartReviewResponse reviewChart(@AuthenticationPrincipal User user, @RequestParam("file") MultipartFile file, @RequestParam(value = "tradeId", required = false) Long tradeId, @RequestParam(value = "journalText", required = false) String journalText) {
        return aiChartReviewService.reviewChart(user, file, tradeId, journalText);
    }

    @java.lang.SuppressWarnings("all")
    public AiChartReviewController(final AiChartReviewService aiChartReviewService) {
        this.aiChartReviewService = aiChartReviewService;
    }
}
