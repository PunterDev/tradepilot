package com.tradepilot.ai.memory.controller;

import com.tradepilot.ai.memory.dto.TradingMemoryBackfillRequest;
import com.tradepilot.ai.memory.dto.TradingMemoryBackfillResponse;
import com.tradepilot.ai.memory.dto.TradingMemoryHealthResponse;
import com.tradepilot.ai.memory.dto.TradingMemoryRetryRequest;
import com.tradepilot.ai.memory.dto.TradingMemoryRetryResponse;
import com.tradepilot.ai.memory.service.TradingMemoryBackfillService;
import com.tradepilot.ai.memory.service.TradingMemoryHealthService;
import com.tradepilot.ai.memory.service.TradingMemoryRetryService;
import jakarta.validation.Valid;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping({"/api/v1/admin/trading-memory", "/api/admin/trading-memory"})
public class AdminTradingMemoryController {
    private final TradingMemoryBackfillService backfillService;
    private final TradingMemoryHealthService healthService;
    private final TradingMemoryRetryService retryService;

    public AdminTradingMemoryController(TradingMemoryBackfillService backfillService, TradingMemoryHealthService healthService, TradingMemoryRetryService retryService) {
        this.backfillService = backfillService;
        this.healthService = healthService;
        this.retryService = retryService;
    }

    @GetMapping("/health")
    @PreAuthorize("hasRole('ADMIN')")
    public TradingMemoryHealthResponse health() {
        return healthService.health();
    }

    @PostMapping("/backfill")
    @PreAuthorize("hasRole('ADMIN')")
    public TradingMemoryBackfillResponse backfill(@Valid @RequestBody TradingMemoryBackfillRequest request) {
        return backfillService.backfill(request);
    }

    @PostMapping("/retry-failed")
    @PreAuthorize("hasRole('ADMIN')")
    public TradingMemoryRetryResponse retryFailed(@RequestBody(required = false) TradingMemoryRetryRequest request) {
        return retryService.retryFailed(request);
    }

    @PostMapping("/retry/{tradeMemoryId}")
    @PreAuthorize("hasRole('ADMIN')")
    public TradingMemoryRetryResponse retryOne(@PathVariable UUID tradeMemoryId) {
        return retryService.retryOne(tradeMemoryId);
    }
}
