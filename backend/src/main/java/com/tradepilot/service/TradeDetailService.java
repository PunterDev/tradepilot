package com.tradepilot.service;

import com.tradepilot.dto.trade.TradeDetailResponse;
import com.tradepilot.dto.trade.TradeViolationResponse;
import com.tradepilot.entity.Trade;
import com.tradepilot.entity.TradeRuleViolation;
import com.tradepilot.entity.User;
import com.tradepilot.repository.TradeRepository;
import com.tradepilot.repository.TradeRuleViolationRepository;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.time.DayOfWeek;
import java.util.ArrayList;
import java.util.List;

@Service
public class TradeDetailService {
    private final TradeService tradeService;
    private final TradeRepository tradeRepository;
    private final TradeRuleViolationRepository tradeRuleViolationRepository;
    private final AiTradeReviewService aiTradeReviewService;

    @Transactional(readOnly = true)
    public TradeDetailResponse getTradeDetail(User user, Long tradeId) {
        Trade trade = tradeService.getOwnedTrade(user, tradeId);
        Long previousTradeId = previousTradeId(user, trade);
        Long nextTradeId = nextTradeId(user, trade);
        var review = aiTradeReviewService.getLatestCompletedReviewForTrade(user, tradeId);
        List<TradeViolationResponse> violations = tradeRuleViolationRepository.findByUserIdAndTradeIdOrderByCreatedAtDesc(user.getId(), tradeId).stream().map(this::mapViolation).toList();
        return TradeDetailResponse.of(tradeService.mapToResponse(trade), previousTradeId, nextTradeId, review, review != null, buildRelatedInsights(trade, violations), violations);
    }

    private Long previousTradeId(User user, Trade trade) {
        if (trade.getTradeDate() == null || trade.getEntryTime() == null) {
            return null;
        }
        return tradeRepository.findPreviousTradeId(user, trade.getId(), trade.getTradeDate(), trade.getEntryTime(), PageRequest.of(0, 1)).stream().findFirst().orElse(null);
    }

    private Long nextTradeId(User user, Trade trade) {
        if (trade.getTradeDate() == null || trade.getEntryTime() == null) {
            return null;
        }
        return tradeRepository.findNextTradeId(user, trade.getId(), trade.getTradeDate(), trade.getEntryTime(), PageRequest.of(0, 1)).stream().findFirst().orElse(null);
    }

    private TradeViolationResponse mapViolation(TradeRuleViolation violation) {
        return TradeViolationResponse.of(violation.getId(), violation.getRuleCode(), violation.getRuleTitle(), violation.getCategory(), violation.getSeverity(), violation.getReason(), violation.getSource(), violation.getTradeDate(), violation.getCreatedAt());
    }

    private List<String> buildRelatedInsights(Trade trade, List<TradeViolationResponse> violations) {
        List<String> insights = new ArrayList<>();
        if (trade.getMistakeTag() != null) {
            insights.add("Mistake pattern flagged on this trade: " + humanize(trade.getMistakeTag().name()) + ".");
        }
        if (trade.getFollowedPlan() != null && !trade.getFollowedPlan()) {
            insights.add("Execution drift: this trade was marked as not following the original plan.");
        }
        if (trade.getTradeDate() != null && trade.getTradeDate().getDayOfWeek() == DayOfWeek.FRIDAY) {
            insights.add("Friday context: compare this outcome with your Friday and OPEX behavior before sizing up.");
        }
        if (trade.getSession() != null) {
            insights.add("Session context: " + humanize(trade.getSession().name()) + " trades should be reviewed against your session breakdown.");
        }
        if (trade.getSetup() != null) {
            insights.add("Setup context: " + humanize(trade.getSetup().name()) + " is part of your recurring setup profile.");
        }
        if (!violations.isEmpty()) {
            insights.add("Discipline context: " + violations.size() + " rule " + (violations.size() == 1 ? "violation was" : "violations were") + " attached to this trade.");
        }
        if (insights.isEmpty()) {
            insights.add("No recurring behavioral flags were attached yet. Generate an AI review to enrich this trade\'s pattern history.");
        }
        return insights;
    }

    private String humanize(String value) {
        return value == null ? "" : value.replace('_', ' ').toLowerCase();
    }

    @java.lang.SuppressWarnings("all")
    public TradeDetailService(final TradeService tradeService, final TradeRepository tradeRepository, final TradeRuleViolationRepository tradeRuleViolationRepository, final AiTradeReviewService aiTradeReviewService) {
        this.tradeService = tradeService;
        this.tradeRepository = tradeRepository;
        this.tradeRuleViolationRepository = tradeRuleViolationRepository;
        this.aiTradeReviewService = aiTradeReviewService;
    }
}
