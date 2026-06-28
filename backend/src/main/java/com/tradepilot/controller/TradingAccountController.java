package com.tradepilot.controller;

import com.tradepilot.dto.account.TradingAccountRequest;
import com.tradepilot.dto.account.TradingAccountResponse;
import com.tradepilot.dto.account.TradingAccountRiskRequest;
import com.tradepilot.entity.User;
import com.tradepilot.service.TradingAccountService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/accounts")
@RequiredArgsConstructor
public class TradingAccountController {

    private final TradingAccountService tradingAccountService;

    @PostMapping
    public TradingAccountResponse create(
            @AuthenticationPrincipal User user,
            @Valid @RequestBody TradingAccountRequest request
    ) {
        return tradingAccountService.create(user, request);
    }

    @GetMapping
    public List<TradingAccountResponse> getAccounts(@AuthenticationPrincipal User user) {
        return tradingAccountService.getAccounts(user);
    }

    @PatchMapping("/{id}/risk")
    public TradingAccountResponse updateRiskAmount(
            @PathVariable Long id,
            @AuthenticationPrincipal User user,
            @Valid @RequestBody TradingAccountRiskRequest request
    ) {
        return tradingAccountService.updateRiskAmount(user, id, request);
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deleteAccount(
            @PathVariable Long id,
            @AuthenticationPrincipal User user
    ) {
        tradingAccountService.deleteAccount(user, id);
    }
}
