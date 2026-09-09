package com.tradepilot.controller;

import com.tradepilot.dto.account.TradingAccountRequest;
import com.tradepilot.dto.account.TradingAccountResponse;
import com.tradepilot.dto.account.TradingAccountRiskRequest;
import com.tradepilot.entity.User;
import com.tradepilot.featureusage.FeatureKey;
import com.tradepilot.featureusage.FeatureUsageAction;
import com.tradepilot.featureusage.FeatureUsageEventType;
import com.tradepilot.featureusage.FeatureUsageService;
import com.tradepilot.service.TradingAccountService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import java.util.List;

@RestController
@RequestMapping("/api/accounts")
public class TradingAccountController {
    private final TradingAccountService tradingAccountService;
    private final FeatureUsageService featureUsageService;

    @PostMapping
    public TradingAccountResponse create(@AuthenticationPrincipal User user, @Valid @RequestBody TradingAccountRequest request) {
        TradingAccountResponse response = tradingAccountService.create(user, request);
        featureUsageService.track(user, FeatureKey.ACTIVATION, FeatureUsageAction.TRADING_ACCOUNT_CREATED, FeatureUsageEventType.USER_ACTION, response.getId(), "TRADING_ACCOUNT", response.getId().toString(), "accounts", null);
        return response;
    }

    @GetMapping
    public List<TradingAccountResponse> getAccounts(@AuthenticationPrincipal User user) {
        return tradingAccountService.getAccounts(user);
    }

    @PatchMapping("/{id}/risk")
    public TradingAccountResponse updateRiskAmount(@PathVariable Long id, @AuthenticationPrincipal User user, @Valid @RequestBody TradingAccountRiskRequest request) {
        return tradingAccountService.updateRiskAmount(user, id, request);
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deleteAccount(@PathVariable Long id, @AuthenticationPrincipal User user) {
        tradingAccountService.deleteAccount(user, id);
    }

    @java.lang.SuppressWarnings("all")
    public TradingAccountController(final TradingAccountService tradingAccountService, final FeatureUsageService featureUsageService) {
        this.tradingAccountService = tradingAccountService;
        this.featureUsageService = featureUsageService;
    }
}
