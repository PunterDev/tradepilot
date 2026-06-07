package com.tradepilot.controller;

import com.tradepilot.dto.discipline.RuleViolationDto;
import com.tradepilot.entity.User;
import com.tradepilot.service.DisciplineEngineService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import java.util.List;

@RestController
@RequestMapping("/api/discipline")
@RequiredArgsConstructor
public class DisciplineController {
    private final DisciplineEngineService disciplineEngineService;

    @GetMapping("/trades/{tradeId}/violations")
    public List<RuleViolationDto> getTradeViolations(
            @AuthenticationPrincipal User user,
            @PathVariable Long tradeId
    ) {
        return disciplineEngineService.getViolationsForTrade(user, tradeId);
    }
}
