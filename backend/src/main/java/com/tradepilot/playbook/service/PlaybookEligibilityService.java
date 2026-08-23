package com.tradepilot.playbook.service;

import com.tradepilot.playbook.dto.PlaybookEligibilityResponse;
import com.tradepilot.playbook.entity.PlaybookChecklistItem;
import com.tradepilot.playbook.entity.PlaybookSetup;
import com.tradepilot.playbook.entity.TradePlaybookAssignment;
import com.tradepilot.playbook.enums.ChecklistType;
import com.tradepilot.playbook.enums.PlaybookDetailState;
import com.tradepilot.playbook.enums.PlaybookRequirement;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

@Service
public class PlaybookEligibilityService {
    public static final int TOTAL_DEFINITION_SECTIONS = 6;
    public static final int BEHAVIOR_COMPARISON_MINIMUM_TRADES = 5;
    public static final int AI_REVIEW_MINIMUM_TRADES = 10;
    public static final int MATURE_MINIMUM_TRADES = 30;

    private final PlaybookJsonService jsonService;

    public PlaybookEligibilityService(PlaybookJsonService jsonService) {
        this.jsonService = jsonService;
    }

    public PlaybookEligibilityResponse evaluate(PlaybookSetup setup, List<PlaybookChecklistItem> items, List<TradePlaybookAssignment> assignments) {
        boolean marketContext = hasList(setup.getMarketContextJson());
        boolean entry = hasActive(items, ChecklistType.ENTRY);
        boolean invalidation = hasActive(items, ChecklistType.INVALIDATION);
        boolean risk = hasList(setup.getRiskRulesJson()) || hasActive(items, ChecklistType.RISK);
        boolean management = hasList(setup.getManagementRulesJson()) || hasActive(items, ChecklistType.MANAGEMENT);
        boolean postTrade = hasList(setup.getPostTradeRulesJson()) || hasActive(items, ChecklistType.POST_TRADE);

        int completed = countTrue(marketContext, entry, invalidation, risk, management, postTrade);
        boolean definitionComplete = completed == TOTAL_DEFINITION_SECTIONS;
        long linkedTrades = assignments.size();
        long reviewedTrades = assignments.stream().filter(assignment -> Boolean.TRUE.equals(assignment.getReviewedByUser())).count();
        boolean behaviorEligible = reviewedTrades >= BEHAVIOR_COMPARISON_MINIMUM_TRADES;
        boolean aiEligible = reviewedTrades >= AI_REVIEW_MINIMUM_TRADES;

        List<PlaybookRequirement> missing = new ArrayList<>();
        if (!marketContext) missing.add(PlaybookRequirement.MARKET_CONTEXT_REQUIRED);
        if (!entry) missing.add(PlaybookRequirement.ENTRY_RULE_REQUIRED);
        if (!invalidation) missing.add(PlaybookRequirement.INVALIDATION_RULE_REQUIRED);
        if (!risk) missing.add(PlaybookRequirement.RISK_RULE_REQUIRED);
        if (!management) missing.add(PlaybookRequirement.MANAGEMENT_RULE_REQUIRED);
        if (!postTrade) missing.add(PlaybookRequirement.POST_TRADE_RULE_REQUIRED);
        if (linkedTrades == 0) missing.add(PlaybookRequirement.FIRST_LINKED_TRADE_REQUIRED);
        if (reviewedTrades < BEHAVIOR_COMPARISON_MINIMUM_TRADES) missing.add(PlaybookRequirement.FIVE_REVIEWED_TRADES_REQUIRED);
        if (reviewedTrades < AI_REVIEW_MINIMUM_TRADES) missing.add(PlaybookRequirement.TEN_REVIEWED_TRADES_REQUIRED);

        PlaybookDetailState state = state(definitionComplete, linkedTrades, reviewedTrades);
        return new PlaybookEligibilityResponse(
                state,
                definitionComplete,
                completed,
                TOTAL_DEFINITION_SECTIONS,
                linkedTrades,
                reviewedTrades,
                behaviorEligible,
                BEHAVIOR_COMPARISON_MINIMUM_TRADES,
                aiEligible,
                AI_REVIEW_MINIMUM_TRADES,
                List.copyOf(missing),
                recommendedAction(state)
        );
    }

    private PlaybookDetailState state(boolean definitionComplete, long linkedTrades, long reviewedTrades) {
        if (!definitionComplete) return PlaybookDetailState.INCOMPLETE_SETUP;
        if (linkedTrades == 0) return PlaybookDetailState.NO_LINKED_TRADES;
        if (reviewedTrades < BEHAVIOR_COMPARISON_MINIMUM_TRADES) return PlaybookDetailState.EARLY_EVIDENCE;
        if (reviewedTrades < AI_REVIEW_MINIMUM_TRADES) return PlaybookDetailState.COMPARISON_ELIGIBLE;
        if (reviewedTrades >= MATURE_MINIMUM_TRADES) return PlaybookDetailState.MATURE_SETUP;
        return PlaybookDetailState.AI_REVIEW_ELIGIBLE;
    }

    private String recommendedAction(PlaybookDetailState state) {
        if (state == PlaybookDetailState.INCOMPLETE_SETUP) return "COMPLETE_SETUP";
        if (state == PlaybookDetailState.NO_LINKED_TRADES) return "LINK_FIRST_TRADE";
        if (state == PlaybookDetailState.EARLY_EVIDENCE) return "REVIEW_LINKED_TRADES";
        if (state == PlaybookDetailState.COMPARISON_ELIGIBLE) return "REVIEW_RULE_COMPLIANCE";
        return "GENERATE_AI_REVIEW";
    }

    private boolean hasActive(List<PlaybookChecklistItem> items, ChecklistType type) {
        return items.stream().anyMatch(item -> item.getChecklistType() == type
                && !Boolean.FALSE.equals(item.getActive())
                && hasText(item.getLabel()));
    }

    private boolean hasList(String json) {
        return jsonService.readStringList(json).stream().anyMatch(this::hasText);
    }

    private boolean hasText(String value) {
        return value != null && !value.isBlank();
    }

    private int countTrue(boolean... values) {
        int count = 0;
        for (boolean value : values) {
            if (value) count++;
        }
        return count;
    }
}
