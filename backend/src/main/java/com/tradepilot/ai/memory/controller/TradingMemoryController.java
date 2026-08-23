package com.tradepilot.ai.memory.controller;

import com.tradepilot.ai.memory.dto.TradeMemoryStatusResponse;
import com.tradepilot.ai.memory.dto.TradingMemoryResponse;
import com.tradepilot.ai.memory.service.TradeSimilarityService;
import com.tradepilot.entity.User;
import com.tradepilot.featureusage.FeatureKey;
import com.tradepilot.featureusage.FeatureUsageAction;
import com.tradepilot.featureusage.FeatureUsageEventType;
import com.tradepilot.featureusage.FeatureUsageService;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping({"/api/v1/trades/{tradeId}", "/api/trades/{tradeId}"})
public class TradingMemoryController {
    private final TradeSimilarityService similarityService;
    private final FeatureUsageService featureUsageService;

    public TradingMemoryController(TradeSimilarityService similarityService, FeatureUsageService featureUsageService) {
        this.similarityService = similarityService;
        this.featureUsageService = featureUsageService;
    }

    @GetMapping("/trading-memory")
    public TradingMemoryResponse getTradingMemory(
            @AuthenticationPrincipal User user,
            @PathVariable Long tradeId,
            @RequestParam(required = false) Integer limit,
            @RequestParam(defaultValue = "false") boolean sameSetup,
            @RequestParam(defaultValue = "false") boolean sameInstrument,
            @RequestParam(defaultValue = "false") boolean sameDirection,
            @RequestParam(required = false) Integer lookbackDays) {
        TradingMemoryResponse response = similarityService.getTradingMemory(user, tradeId, limit, sameSetup, sameInstrument, sameDirection, lookbackDays);
        featureUsageService.track(user, FeatureKey.TRADING_MEMORY, FeatureUsageAction.TRADING_MEMORY_VIEWED, FeatureUsageEventType.USER_ACTION, null, null);
        return response;
    }

    @PostMapping("/trading-memory/regenerate")
    public TradeMemoryStatusResponse regenerate(@AuthenticationPrincipal User user, @PathVariable Long tradeId, @RequestParam(defaultValue = "false") boolean force) {
        return similarityService.regenerate(user, tradeId, force);
    }

    @GetMapping("/similar-trades")
    public TradingMemoryResponse similarTrades(
            @AuthenticationPrincipal User user,
            @PathVariable Long tradeId,
            @RequestParam(required = false) Integer limit,
            @RequestParam(defaultValue = "false") boolean sameSetup,
            @RequestParam(defaultValue = "false") boolean sameInstrument,
            @RequestParam(defaultValue = "false") boolean sameDirection,
            @RequestParam(required = false) Integer lookbackDays) {
        return similarityService.similarTrades(user, tradeId, limit, sameSetup, sameInstrument, sameDirection, lookbackDays);
    }
}
