package com.tradepilot.ai.strategy;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.tradepilot.entity.Trade;
import com.tradepilot.enums.SetupType;
import com.tradepilot.playbook.entity.PlaybookSetup;
import com.tradepilot.playbook.entity.TradePlaybookAssignment;
import com.tradepilot.playbook.enums.PlaybookCategory;
import com.tradepilot.playbook.repository.TradePlaybookAssignmentRepository;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.Optional;

@Service
public class StrategyContextService {
    private final TradePlaybookAssignmentRepository assignmentRepository;
    private final ObjectMapper objectMapper;

    public StrategyContextService(TradePlaybookAssignmentRepository assignmentRepository, ObjectMapper objectMapper) {
        this.assignmentRepository = assignmentRepository;
        this.objectMapper = objectMapper;
    }

    public StrategyContext resolve(Long userId, Trade trade, String journalText) {
        if (trade == null) {
            return resolveFromText(journalText);
        }
        Optional<TradePlaybookAssignment> assignment = trade.getId() == null || userId == null
                ? Optional.empty()
                : assignmentRepository.findByTrade_IdAndUser_Id(trade.getId(), userId);
        if (assignment.isPresent()) {
            StrategyContext context = fromPlaybook(assignment.get());
            if (context != null) {
                return context;
            }
        }
        StrategyContext setupContext = fromSetup(trade.getSetup());
        if (setupContext.strategyType() != StrategyType.GENERAL) {
            return setupContext;
        }
        String combinedText = String.join("\n",
                trade.getNotes() == null ? "" : trade.getNotes(),
                trade.getPsychologyNotes() == null ? "" : trade.getPsychologyNotes(),
                trade.getLessonLearned() == null ? "" : trade.getLessonLearned(),
                journalText == null ? "" : journalText
        );
        return resolveFromText(combinedText);
    }

    public StrategyContext resolveFromText(String text) {
        StrategyType type = inferFromText(text);
        if (type == StrategyType.GENERAL) {
            return StrategyContext.general();
        }
        return context(type, displayName(type), null, null, null, null, List.of(), List.of(), List.of(), List.of(), List.of(), "TEXT_CONTEXT", 0.45);
    }

    private StrategyContext fromPlaybook(TradePlaybookAssignment assignment) {
        PlaybookSetup setup = assignment.getSetup();
        if (setup == null) {
            return null;
        }
        StrategyType type = fromCategory(setup.getCategory());
        if (type == StrategyType.GENERAL) {
            type = inferFromText(String.join("\n",
                    nullToEmpty(setup.getName()),
                    nullToEmpty(setup.getShortDescription()),
                    nullToEmpty(setup.getDescription()),
                    readList(setup.getTagsJson()).toString(),
                    readList(setup.getMarketContextJson()).toString(),
                    readList(setup.getRiskRulesJson()).toString(),
                    readList(setup.getManagementRulesJson()).toString(),
                    readList(setup.getPostTradeRulesJson()).toString(),
                    assignment.getNotes() == null ? "" : assignment.getNotes()
            ));
        }
        if (type == StrategyType.GENERAL && setup.getCategory() == PlaybookCategory.CUSTOM) {
            type = StrategyType.CUSTOM;
        }
        return context(
                type,
                setup.getName(),
                null,
                setup.getName(),
                setup.getCategory() == null ? null : setup.getCategory().name(),
                setup.getName(),
                combine(readList(setup.getMarketContextJson()), readList(setup.getRiskRulesJson()), readList(setup.getPostTradeRulesJson())),
                readList(setup.getMarketContextJson()),
                readList(setup.getRiskRulesJson()),
                readList(setup.getManagementRulesJson()),
                readList(setup.getPostTradeRulesJson()),
                "LINKED_PLAYBOOK",
                0.95
        );
    }

    private StrategyContext fromSetup(SetupType setup) {
        if (setup == null) {
            return StrategyContext.general();
        }
        StrategyType type = switch (setup) {
            case FVG, IFVG, LIQUIDITY_SWEEP, BREAKER, SMT -> StrategyType.ICT_SMC;
            case ORB -> StrategyType.OPENING_RANGE;
            case TREND_CONTINUATION -> StrategyType.TREND_FOLLOWING;
            case RANGE_REVERSION, COUNTER_TREND -> StrategyType.MEAN_REVERSION;
            case RECLAIM -> StrategyType.SUPPORT_RESISTANCE;
            case OTHER -> StrategyType.CUSTOM;
        };
        return context(type, displayName(type), setup.name(), displaySetup(setup), null, null, List.of(), List.of(), List.of(), List.of(), List.of(), "TRADE_SETUP", 0.75);
    }

    private StrategyType fromCategory(PlaybookCategory category) {
        if (category == null) {
            return StrategyType.GENERAL;
        }
        return switch (category) {
            case FVG, IFVG, LIQUIDITY_SWEEP -> StrategyType.ICT_SMC;
            case OPENING_RANGE -> StrategyType.OPENING_RANGE;
            case TREND_FOLLOWING -> StrategyType.TREND_FOLLOWING;
            case MEAN_REVERSION -> StrategyType.MEAN_REVERSION;
            case BREAKOUT -> StrategyType.BREAKOUT;
            case CONTINUATION, PULLBACK -> StrategyType.PRICE_ACTION;
            case REVERSAL -> StrategyType.SUPPORT_RESISTANCE;
            case CUSTOM -> StrategyType.CUSTOM;
        };
    }

    private StrategyType inferFromText(String text) {
        String normalized = normalize(text);
        if (normalized.isBlank()) return StrategyType.GENERAL;
        if (containsAny(normalized, " ict ", " smc ", " fvg ", " ifvg ", " fair value gap ", " smt ", " order block ", " breaker ", " premium discount ", " premium/discount ")) {
            return StrategyType.ICT_SMC;
        }
        if (containsAny(normalized, " opening range ", " orb ", " first 15 minute range ", " first 30 minute range ")) {
            return StrategyType.OPENING_RANGE;
        }
        if (containsAny(normalized, " vwap ")) {
            return StrategyType.VWAP;
        }
        if (containsAny(normalized, " mean reversion ", " revert to mean ", " reversion ", " exhaustion ")) {
            return StrategyType.MEAN_REVERSION;
        }
        if (containsAny(normalized, " trend following ", " trend continuation ", " pullback continuation ", " higher timeframe trend ")) {
            return StrategyType.TREND_FOLLOWING;
        }
        if (containsAny(normalized, " breakout ", " range break ", " range boundary ")) {
            return StrategyType.BREAKOUT;
        }
        if (containsAny(normalized, " momentum ", " impulse move ")) {
            return StrategyType.MOMENTUM;
        }
        if (containsAny(normalized, " support resistance ", " support/resistance ", " resistance ", " support ")) {
            return StrategyType.SUPPORT_RESISTANCE;
        }
        if (containsAny(normalized, " supply demand ", " supply zone ", " demand zone ")) {
            return StrategyType.SUPPLY_DEMAND;
        }
        if (containsAny(normalized, " price action ")) {
            return StrategyType.PRICE_ACTION;
        }
        if (containsAny(normalized, " scalp ", " scalping ")) {
            return StrategyType.SCALPING;
        }
        return StrategyType.GENERAL;
    }

    private StrategyContext context(
            StrategyType type,
            String strategyName,
            String setupType,
            String setupName,
            String playbookCategory,
            String playbookName,
            List<String> userRules,
            List<String> entryCriteria,
            List<String> invalidationCriteria,
            List<String> managementRules,
            List<String> noTradeConditions,
            String strategySource,
            double confidence
    ) {
        return new StrategyContext(
                type,
                strategyName == null || strategyName.isBlank() ? displayName(type) : strategyName,
                setupType,
                setupName,
                playbookCategory,
                playbookName,
                compact(userRules),
                compact(entryCriteria),
                compact(invalidationCriteria),
                compact(managementRules),
                compact(noTradeConditions),
                concepts(type),
                strategySource,
                confidence
        );
    }

    private List<String> concepts(StrategyType type) {
        return switch (type) {
            case ICT_SMC -> List.of("fair value gap", "inversion fair value gap", "displacement", "liquidity sweep", "SMT", "premium/discount", "order block", "breaker", "market structure shift");
            case OPENING_RANGE -> List.of("opening range", "opening range high/low", "range breakout", "acceptance", "failed breakout", "range retest");
            case TREND_FOLLOWING -> List.of("higher-timeframe trend", "pullback", "continuation", "momentum confirmation", "trailing management");
            case MEAN_REVERSION -> List.of("extension", "exhaustion", "mean reversion", "VWAP or mean target", "invalidation");
            case BREAKOUT -> List.of("range boundary", "breakout quality", "acceptance", "failed breakout", "retest");
            case MOMENTUM -> List.of("momentum confirmation", "impulse continuation", "follow-through", "failed momentum");
            case SUPPORT_RESISTANCE -> List.of("support", "resistance", "reclaim", "rejection", "level acceptance");
            case SUPPLY_DEMAND -> List.of("supply zone", "demand zone", "zone reaction", "invalidation");
            case VWAP -> List.of("VWAP", "deviation from VWAP", "mean target", "extension", "reversion");
            case PRICE_ACTION -> List.of("market structure", "price action", "confirmation", "pullback", "range context");
            case SCALPING -> List.of("scalp timing", "fast invalidation", "execution precision", "trade management");
            case CUSTOM -> List.of("the user's documented custom rules", "plan adherence", "risk management", "execution quality");
            case GENERAL -> List.of("plan adherence", "risk management", "entry timing", "exit quality", "trade management", "discipline");
        };
    }

    private String displayName(StrategyType type) {
        return switch (type) {
            case ICT_SMC -> "ICT / SMC";
            case OPENING_RANGE -> "Opening Range Breakout";
            case TREND_FOLLOWING -> "Trend Following";
            case MEAN_REVERSION -> "Mean Reversion";
            case BREAKOUT -> "Breakout";
            case MOMENTUM -> "Momentum";
            case SUPPORT_RESISTANCE -> "Support / Resistance";
            case SUPPLY_DEMAND -> "Supply / Demand";
            case VWAP -> "VWAP";
            case PRICE_ACTION -> "Price Action";
            case SCALPING -> "Scalping";
            case CUSTOM -> "Custom";
            case GENERAL -> "General";
        };
    }

    private String displaySetup(SetupType setup) {
        return setup == null ? null : setup.name().replace('_', ' ');
    }

    private List<String> readList(String json) {
        if (json == null || json.isBlank()) {
            return List.of();
        }
        try {
            return objectMapper.readValue(json, objectMapper.getTypeFactory().constructCollectionType(List.class, String.class));
        } catch (Exception ignored) {
            return List.of();
        }
    }

    @SafeVarargs
    private static List<String> combine(List<String>... lists) {
        List<String> combined = new ArrayList<>();
        for (List<String> list : lists) {
            if (list != null) combined.addAll(list);
        }
        return compact(combined);
    }

    private static List<String> compact(List<String> values) {
        if (values == null) {
            return List.of();
        }
        return values.stream()
                .filter(Objects::nonNull)
                .map(String::trim)
                .filter(value -> !value.isBlank())
                .distinct()
                .limit(24)
                .toList();
    }

    private static String nullToEmpty(String value) {
        return value == null ? "" : value;
    }

    private static String normalize(String text) {
        if (text == null) {
            return "";
        }
        return " " + text.toLowerCase(Locale.ROOT).replace('_', ' ').replace('-', ' ') + " ";
    }

    private static boolean containsAny(String text, String... needles) {
        for (String needle : needles) {
            if (text.contains(needle)) {
                return true;
            }
        }
        return false;
    }
}
