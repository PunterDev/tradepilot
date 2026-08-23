package com.tradepilot.tradereplay;

import com.tradepilot.entity.User;
import com.tradepilot.tradereplay.dto.ConfirmReplayContractRequest;
import com.tradepilot.tradereplay.dto.ContractCandidatesResponse;
import com.tradepilot.tradereplay.dto.ReplayCandleResponse;
import com.tradepilot.tradereplay.dto.TradeReplayResponse;
import com.tradepilot.tradereplay.service.TradeReplayService;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping({"/api/v1/trades/{tradeId}/replay", "/api/trades/{tradeId}/replay"})
public class TradeReplayController {
    private final TradeReplayService tradeReplayService;

    public TradeReplayController(TradeReplayService tradeReplayService) {
        this.tradeReplayService = tradeReplayService;
    }

    @GetMapping
    public TradeReplayResponse getReplay(@PathVariable Long tradeId, @AuthenticationPrincipal User user) {
        return tradeReplayService.getReplay(user, tradeId, true);
    }

    @PostMapping("/generate")
    public TradeReplayResponse generate(@PathVariable Long tradeId, @AuthenticationPrincipal User user) {
        return tradeReplayService.generate(user, tradeId, false);
    }

    @PostMapping("/regenerate")
    public TradeReplayResponse regenerate(@PathVariable Long tradeId, @AuthenticationPrincipal User user) {
        return tradeReplayService.generate(user, tradeId, true);
    }

    @GetMapping("/candles")
    public List<ReplayCandleResponse> candles(@PathVariable Long tradeId, @AuthenticationPrincipal User user) {
        return tradeReplayService.candles(user, tradeId);
    }

    @GetMapping("/contract-candidates")
    public ContractCandidatesResponse contractCandidates(@PathVariable Long tradeId, @AuthenticationPrincipal User user) {
        return tradeReplayService.contractCandidates(user, tradeId);
    }

    @PutMapping("/contract")
    public TradeReplayResponse confirmContract(@PathVariable Long tradeId, @AuthenticationPrincipal User user, @RequestBody ConfirmReplayContractRequest request) {
        return tradeReplayService.confirmContract(user, tradeId, request);
    }
}
