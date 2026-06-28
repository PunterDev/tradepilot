package com.tradepilot.controller;

import com.tradepilot.billing.Feature;
import com.tradepilot.billing.FeatureAccessService;
import com.tradepilot.dto.common.PagedResponse;
import com.tradepilot.dto.trade.*;
import com.tradepilot.entity.User;
import com.tradepilot.enums.*;
import com.tradepilot.featureusage.FeatureKey;
import com.tradepilot.featureusage.FeatureUsageService;
import com.tradepilot.service.TradeDetailService;
import com.tradepilot.service.TradeService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;

@RestController
@RequestMapping("/api/trades")
@RequiredArgsConstructor
public class TradeController {
    private final TradeService tradeService;
    private final TradeDetailService tradeDetailService;
    private final FeatureUsageService featureUsageService;
    private final FeatureAccessService featureAccessService;

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public TradeResponse createTrade(@Valid @RequestBody TradeRequest request, @AuthenticationPrincipal User user) {
        return tradeService.createTrade(user, request);
    }

    @GetMapping
    public PagedResponse<TradeResponse> getTrades(
            @AuthenticationPrincipal User user,
            @RequestParam(required = false) Long accountId,
            @RequestParam(required = false) String symbol,
            @RequestParam(required = false) SetupType setup,
            @RequestParam(required = false) SessionType session,
            @RequestParam(required = false) OutcomeType outcome,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "25") int size,
            @RequestParam(defaultValue = "tradeDate") String sortBy,
            @RequestParam(defaultValue = "desc") String sortDir
    ) {
        TradeFilterRequest filter = new TradeFilterRequest();
        filter.setAccountId(accountId);
        filter.setSymbol(symbol);
        filter.setSetup(setup);
        filter.setSession(session);
        filter.setOutcome(outcome);
        filter.setStartDate(startDate);
        filter.setEndDate(endDate);

        return tradeService.getTrades(user, filter, page, size, sortBy, sortDir);
    }

    @GetMapping("/{id}")
    public TradeResponse getTrade(@PathVariable Long id, @AuthenticationPrincipal User user) {
        return tradeService.getTradeById(user, id);
    }

    @GetMapping("/{id}/detail")
    public TradeDetailResponse getTradeDetail(@PathVariable Long id, @AuthenticationPrincipal User user) {
        TradeDetailResponse response = tradeDetailService.getTradeDetail(user, id);
        featureUsageService.track(user, FeatureKey.TRADE_DETAIL);
        if (response.getAiReview() != null) {
            featureUsageService.track(user, FeatureKey.AI_REVIEW);
        }
        return response;
    }

    @PutMapping("/{id}")
    public TradeResponse updateTrade(@PathVariable Long id, @Valid @RequestBody TradeRequest request, @AuthenticationPrincipal User user) {
        return tradeService.updateTrade(user, id, request);
    }

    @PatchMapping("/{id}/journal")
    public TradeResponse updateTradeJournal(
            @PathVariable Long id,
            @Valid @RequestBody TradeJournalRequest request,
            @AuthenticationPrincipal User user
    ) {
        return tradeService.updateTradeJournal(user, id, request);
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deleteTrade(@PathVariable Long id, @AuthenticationPrincipal User user) {
        tradeService.deleteTrade(user, id);
    }

    @PostMapping("/import")
    public List<TradeResponse> importTrades(
            @RequestBody List<TradeImportRequest> requests,
            @AuthenticationPrincipal User user
    ) {
        featureAccessService.requireFeature(user, Feature.CSV_IMPORT);
        return tradeService.importTrades(user, requests);
    }

    @PostMapping("/repair-tradovate-directions")
    public java.util.Map<String, Object> repairTradovateDirections(
            @AuthenticationPrincipal User user,
            @RequestParam(required = false) Long accountId,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date
    ) {
        featureAccessService.requireFeature(user, Feature.CSV_IMPORT);
        return tradeService.repairTradovateDirections(user, accountId, date);
    }
}
