package com.tradepilot.tradingdna.controller;

import com.tradepilot.entity.User;
import com.tradepilot.featureusage.FeatureKey;
import com.tradepilot.featureusage.FeatureUsageService;
import com.tradepilot.tradingdna.dto.TradingDnaResponse;
import com.tradepilot.tradingdna.service.TradingDnaService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/ai")
@RequiredArgsConstructor
@Slf4j
public class TradingDnaController {
    private final TradingDnaService tradingDnaService;
    private final FeatureUsageService featureUsageService;

    @GetMapping("/trading-dna")
    public TradingDnaResponse getTradingDna(
            @AuthenticationPrincipal User user,
            @RequestParam(required = false) Long accountId,
            @RequestParam(defaultValue = "30") Integer days,
            @RequestParam(required = false) Boolean includeAllAccounts,
            @RequestParam(required = false) Boolean forceRefresh
    ) {
        log.info("Trading DNA controller request. currentUserId={}, accountId={}, includeAllAccounts={}, forceRefresh={}, days={}",
                user == null ? null : user.getId(),
                accountId,
                includeAllAccounts,
                forceRefresh,
                days
        );
        TradingDnaResponse response = tradingDnaService.getTradingDna(user, accountId, days, includeAllAccounts, forceRefresh);
        featureUsageService.track(user, FeatureKey.TRADING_DNA);
        return response;
    }
}
