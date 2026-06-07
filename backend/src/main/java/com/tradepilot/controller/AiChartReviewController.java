package com.tradepilot.controller;

import com.tradepilot.dto.ai.AiChartReviewResponse;
import com.tradepilot.service.AiChartReviewService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/api/ai/chart-review")
@RequiredArgsConstructor
public class AiChartReviewController {

    private final AiChartReviewService aiChartReviewService;

    @PostMapping
    public AiChartReviewResponse reviewChart(
            @RequestParam("file") MultipartFile file,
            @RequestParam(value = "tradeId", required = false) Long tradeId,
            @RequestParam(value = "journalText", required = false) String journalText
    ) {
        return aiChartReviewService.reviewChart(file, tradeId, journalText);
    }
}