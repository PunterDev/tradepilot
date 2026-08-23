package com.tradepilot;

import com.tradepilot.entity.Trade;
import com.tradepilot.entity.TradingAccount;
import com.tradepilot.enums.SessionType;
import com.tradepilot.playbook.dto.PlaybookMetricsResponse;
import com.tradepilot.playbook.entity.PlaybookChecklistItem;
import com.tradepilot.playbook.entity.TradePlaybookAssignment;
import com.tradepilot.playbook.enums.ChecklistResult;
import com.tradepilot.playbook.enums.ChecklistType;
import com.tradepilot.playbook.enums.PlaybookConfidence;
import com.tradepilot.playbook.service.PlaybookComplianceResult;
import com.tradepilot.playbook.service.PlaybookComplianceService;
import com.tradepilot.playbook.service.PlaybookMetricsService;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;

class PlaybookCalculationTests {

    private final PlaybookComplianceService complianceService = new PlaybookComplianceService();
    private final PlaybookMetricsService metricsService = new PlaybookMetricsService();

    @Test
    void complianceScoreWeightsRequiredAndOptionalRules() {
        PlaybookChecklistItem required = item(1L, ChecklistType.ENTRY, true);
        PlaybookChecklistItem optional = item(2L, ChecklistType.MANAGEMENT, false);

        PlaybookComplianceResult result = complianceService.score(List.of(required, optional), Map.of(
                1L, ChecklistResult.MET.name(),
                2L, ChecklistResult.MET.name()
        ));

        assertEquals(100, result.score());
        assertEquals(70, result.requiredScore());
        assertEquals(30, result.optionalScore());
        assertEquals(PlaybookConfidence.HIGH, result.confidence());
    }

    @Test
    void complianceScorePenalizesRequiredRuleBreaks() {
        PlaybookChecklistItem required = item(1L, ChecklistType.ENTRY, true);
        PlaybookChecklistItem optional = item(2L, ChecklistType.MANAGEMENT, false);

        PlaybookComplianceResult result = complianceService.score(List.of(required, optional), Map.of(
                1L, ChecklistResult.NOT_MET.name(),
                2L, ChecklistResult.MET.name()
        ));

        assertEquals(25, result.score());
        assertEquals(1, result.missingRequiredCount());
    }

    @Test
    void metricsUseTradeRiskBeforeAccountDefaultRisk() {
        TradingAccount account = new TradingAccount();
        account.setDefaultRiskAmount(new BigDecimal("200.00"));

        Trade first = trade(1L, account, new BigDecimal("400.00"), null);
        Trade second = trade(2L, account, new BigDecimal("-100.00"), new BigDecimal("50.00"));

        PlaybookMetricsResponse response = metricsService.calculate(List.of(assignment(first), assignment(second)));

        assertEquals(new BigDecimal("0.00"), response.totalR());
        assertEquals(new BigDecimal("0.00"), response.averageR());
        assertEquals(new BigDecimal("300.00"), response.netPnl());
    }

    private PlaybookChecklistItem item(Long id, ChecklistType type, boolean required) {
        PlaybookChecklistItem item = new PlaybookChecklistItem();
        item.setId(id);
        item.setChecklistType(type);
        item.setRequired(required);
        item.setWeight(1);
        item.setActive(true);
        return item;
    }

    private Trade trade(Long id, TradingAccount account, BigDecimal pnl, BigDecimal risk) {
        Trade trade = new Trade();
        trade.setId(id);
        trade.setAccount(account);
        trade.setSymbol("MESU6");
        trade.setMarket("FUTURES");
        trade.setTradeDate(LocalDate.of(2026, 6, 22));
        trade.setSide("LONG");
        trade.setSession(SessionType.NY_AM);
        trade.setPnlAmount(pnl);
        trade.setRiskAmount(risk);
        return trade;
    }

    private TradePlaybookAssignment assignment(Trade trade) {
        TradePlaybookAssignment assignment = new TradePlaybookAssignment();
        assignment.setTrade(trade);
        assignment.setReviewedByUser(true);
        assignment.setComplianceScore(80);
        return assignment;
    }
}
