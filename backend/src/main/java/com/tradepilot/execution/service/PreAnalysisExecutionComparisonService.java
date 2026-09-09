package com.tradepilot.execution.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.tradepilot.entity.Trade;
import com.tradepilot.entity.User;
import com.tradepilot.execution.dto.*;
import com.tradepilot.preanalysis.dto.PreAnalysisDtos.PreAnalysisAiResult;
import com.tradepilot.preanalysis.entity.*;
import com.tradepilot.preanalysis.repository.AiPreAnalysisRepository;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.Optional;

@Service
public class PreAnalysisExecutionComparisonService {
    private final AiPreAnalysisRepository repository;
    private final ObjectMapper objectMapper;

    public PreAnalysisExecutionComparisonService(AiPreAnalysisRepository repository, ObjectMapper objectMapper) {
        this.repository = repository;
        this.objectMapper = objectMapper;
    }

    public PreAnalysisComparisonResponse compareTrade(User user, Trade trade) {
        return findPlanForTrade(user, trade)
                .map(plan -> compareTrade(plan, trade))
                .orElseGet(() -> PreAnalysisComparisonResponse.notAvailable("No usable pre-analysis was found before this trade."));
    }

    public PreAnalysisComparisonResponse compareSession(User user, LocalDate date, List<Trade> trades) {
        Optional<AiPreAnalysis> plan = trades.isEmpty()
                ? findLatestPlanForSession(user, date)
                : trades.stream().map(trade -> findPlanForTrade(user, trade)).filter(Optional::isPresent).map(Optional::get).max(Comparator.comparing(AiPreAnalysis::getCreatedAt));
        if (plan.isEmpty()) {
            return PreAnalysisComparisonResponse.notAvailable("No usable pre-analysis was found before this session behavior.");
        }
        if (!trades.isEmpty() && explicitNoTradePlan(plan.get())) {
            return compareNoTradePlanAgainstTrade(plan.get(), trades.get(0));
        }
        if (trades.isEmpty()) {
            return PreAnalysisComparisonResponse.notAvailable("No trades were recorded; use the no-trade evaluation for this session decision.");
        }
        return compareTrade(plan.get(), trades.get(0));
    }

    public NoTradeEvaluationResponse evaluateNoTrade(User user, LocalDate date, List<Trade> trades) {
        Optional<AiPreAnalysis> plan = trades.isEmpty()
                ? findLatestPlanForSession(user, date)
                : trades.stream().map(trade -> findPlanForTrade(user, trade)).filter(Optional::isPresent).map(Optional::get).max(Comparator.comparing(AiPreAnalysis::getCreatedAt));
        if (plan.isEmpty()) {
            return trades.isEmpty()
                    ? new NoTradeEvaluationResponse(NoTradeEvaluationStatus.INSUFFICIENT_EVIDENCE, null, "No trades recorded. There is not enough plan evidence to determine whether this was an intentional no-trade session.")
                    : new NoTradeEvaluationResponse(NoTradeEvaluationStatus.NOT_APPLICABLE, null, "Trades were taken and no relevant no-trade plan was linked.");
        }
        if (!explicitNoTradePlan(plan.get())) {
            return trades.isEmpty()
                    ? new NoTradeEvaluationResponse(NoTradeEvaluationStatus.INSUFFICIENT_EVIDENCE, plan.get().getId(), "A pre-analysis exists, but it does not contain enough structured no-trade evidence to grade the session decision.")
                    : new NoTradeEvaluationResponse(NoTradeEvaluationStatus.NOT_APPLICABLE, plan.get().getId(), "The linked pre-analysis did not call for a no-trade session.");
        }
        if (trades.isEmpty()) {
            return new NoTradeEvaluationResponse(NoTradeEvaluationStatus.SUCCESSFUL_NO_TRADE, plan.get().getId(), "Your planned setup conditions were not satisfied and you stayed out of the market. Patience was part of the plan.");
        }
        return new NoTradeEvaluationResponse(NoTradeEvaluationStatus.PLAN_VIOLATED_BY_TRADE, plan.get().getId(), "Your pre-session plan called for no trade under the structured conditions, but a position was entered.");
    }

    Optional<AiPreAnalysis> findPlanForTrade(User user, Trade trade) {
        if (user == null || trade == null || trade.getTradeDate() == null) return Optional.empty();
        LocalDateTime start = trade.getTradeDate().atStartOfDay();
        LocalDateTime end = trade.getEntryTime() == null ? trade.getTradeDate().plusDays(1).atStartOfDay() : trade.getEntryTime();
        String root = marketRoot(trade);
        return repository.findByUserAndCreatedAtBetweenOrderByCreatedAtDesc(user, start, end).stream()
                .filter(plan -> root == null || plan.getMarket() == null || root.equals(plan.getMarket().name()))
                .findFirst();
    }

    Optional<AiPreAnalysis> findLatestPlanForSession(User user, LocalDate date) {
        if (user == null || date == null) return Optional.empty();
        return repository.findByUserAndCreatedAtBetweenOrderByCreatedAtDesc(user, date.atStartOfDay(), date.plusDays(1).atStartOfDay()).stream().findFirst();
    }

    private PreAnalysisComparisonResponse compareTrade(AiPreAnalysis plan, Trade trade) {
        PreAnalysisAiResult result = read(plan);
        List<PlanComparisonItemResponse> items = new ArrayList<>();
        items.add(biasComparison(plan, result, trade));
        items.add(setupComparison(plan, result, trade));
        items.add(confirmationComparison(result, trade));
        items.add(entryTimingComparison(result));
        items.add(invalidationComparison(result));
        items.add(instrumentComparison(plan, trade));
        items.add(noTradeComparison(plan, result, trade));
        return response(plan, items);
    }

    private PreAnalysisComparisonResponse compareNoTradePlanAgainstTrade(AiPreAnalysis plan, Trade trade) {
        PreAnalysisAiResult result = read(plan);
        List<PlanComparisonItemResponse> items = new ArrayList<>();
        items.add(noTradeComparison(plan, result, trade));
        items.add(instrumentComparison(plan, trade));
        items.add(biasComparison(plan, result, trade));
        return response(plan, items);
    }

    private PlanComparisonItemResponse biasComparison(AiPreAnalysis plan, PreAnalysisAiResult result, Trade trade) {
        Bias bias = result != null && result.directionalContext() != null && result.directionalContext().bias() != null ? result.directionalContext().bias() : plan.getBias();
        if (bias == null || trade.getSide() == null || bias == Bias.NEUTRAL) {
            return item("BIAS_ALIGNMENT", PlanComparisonStatus.NOT_EVALUABLE, "Bias alignment cannot be evaluated from the available structured data.");
        }
        String side = trade.getSide().toUpperCase(Locale.ROOT);
        boolean aligned = (bias == Bias.BULLISH && side.contains("LONG")) || (bias == Bias.BEARISH && side.contains("SHORT"));
        return aligned
                ? item("BIAS_ALIGNMENT", PlanComparisonStatus.ALIGNED, "Trade direction matched the planned " + bias.name().toLowerCase(Locale.ROOT) + " scenario.")
                : item("BIAS_ALIGNMENT", PlanComparisonStatus.VIOLATED, "Trade direction did not match the structured " + bias.name().toLowerCase(Locale.ROOT) + " pre-analysis bias.");
    }

    private PlanComparisonItemResponse setupComparison(AiPreAnalysis plan, PreAnalysisAiResult result, Trade trade) {
        SetupStatus status = result == null ? plan.getSetupStatus() : result.setupStatus();
        Integer alignment = result != null && result.setupAlignmentScore() != null ? result.setupAlignmentScore() : plan.getSetupAlignmentScore();
        if (status == null && alignment == null) {
            return item("SETUP_ALIGNMENT", PlanComparisonStatus.NOT_EVALUABLE, "Setup alignment cannot be evaluated from the available structured data.");
        }
        if (status == SetupStatus.ENTRY_VALID || status == SetupStatus.ARMED || (alignment != null && alignment >= 70)) {
            return item("SETUP_ALIGNMENT", PlanComparisonStatus.ALIGNED, "The trade was linked to a pre-analysis with usable setup alignment evidence.");
        }
        if (status == SetupStatus.WATCHING || status == SetupStatus.SETUP_FORMING || status == SetupStatus.WAITING_FOR_RETRACE || (alignment != null && alignment >= 40)) {
            return item("SETUP_ALIGNMENT", PlanComparisonStatus.PARTIAL, "The plan showed a forming setup, so alignment is only partial.");
        }
        if (status == SetupStatus.NO_TRADE || status == SetupStatus.INVALIDATED) {
            return item("SETUP_ALIGNMENT", PlanComparisonStatus.VIOLATED, "The linked pre-analysis did not mark the setup as ready for entry.");
        }
        return item("SETUP_ALIGNMENT", PlanComparisonStatus.NOT_EVALUABLE, "Setup alignment cannot be evaluated from the available structured data.");
    }

    private PlanComparisonItemResponse confirmationComparison(PreAnalysisAiResult result, Trade trade) {
        if (result == null || result.conditions() == null || result.conditions().confirmation() == null || result.conditions().confirmation().status() == null) {
            return item("CONFIRMATION_ADHERENCE", PlanComparisonStatus.NOT_EVALUABLE, "Confirmation adherence cannot be evaluated without structured confirmation evidence.");
        }
        ConditionStatus status = result.conditions().confirmation().status();
        if (status == ConditionStatus.SATISFIED) {
            return item("CONFIRMATION_ADHERENCE", PlanComparisonStatus.ALIGNED, "The structured pre-analysis marked confirmation as satisfied.");
        }
        if (status == ConditionStatus.PARTIAL || status == ConditionStatus.WAITING) {
            return item("CONFIRMATION_ADHERENCE", PlanComparisonStatus.PARTIAL, "Confirmation was not fully satisfied in the linked pre-analysis.");
        }
        if (status == ConditionStatus.FAILED && trade != null) {
            return item("CONFIRMATION_ADHERENCE", PlanComparisonStatus.VIOLATED, "A trade was entered after the linked pre-analysis marked confirmation as failed.");
        }
        return item("CONFIRMATION_ADHERENCE", PlanComparisonStatus.NOT_EVALUABLE, "Confirmation adherence cannot be evaluated from the available structured data.");
    }

    private PlanComparisonItemResponse entryTimingComparison(PreAnalysisAiResult result) {
        EntryAssessmentStatus status = result == null || result.entryAssessment() == null ? null : result.entryAssessment().status();
        if (status == null) return item("ENTRY_TIMING_ADHERENCE", PlanComparisonStatus.NOT_EVALUABLE, "Entry timing cannot be evaluated without structured entry-assessment data.");
        if (status == EntryAssessmentStatus.VALID) return item("ENTRY_TIMING_ADHERENCE", PlanComparisonStatus.ALIGNED, "The linked pre-analysis marked entry as valid.");
        if (status == EntryAssessmentStatus.WAIT) return item("ENTRY_TIMING_ADHERENCE", PlanComparisonStatus.PARTIAL, "The linked pre-analysis indicated waiting was still required.");
        if (status == EntryAssessmentStatus.INVALID || status == EntryAssessmentStatus.NO_TRADE) return item("ENTRY_TIMING_ADHERENCE", PlanComparisonStatus.VIOLATED, "The linked pre-analysis did not support taking an entry.");
        return item("ENTRY_TIMING_ADHERENCE", PlanComparisonStatus.NOT_EVALUABLE, "Entry timing cannot be evaluated from the available structured data.");
    }

    private PlanComparisonItemResponse invalidationComparison(PreAnalysisAiResult result) {
        if (result == null || result.invalidation() == null || result.invalidation().summary() == null || result.invalidation().summary().isBlank()) {
            return item("INVALIDATION_ADHERENCE", PlanComparisonStatus.NOT_EVALUABLE, "Invalidation adherence cannot be evaluated without structured invalidation evidence.");
        }
        return item("INVALIDATION_ADHERENCE", PlanComparisonStatus.NOT_EVALUABLE, "The plan recorded invalidation criteria, but current trade data does not prove whether they were respected.");
    }

    private PlanComparisonItemResponse instrumentComparison(AiPreAnalysis plan, Trade trade) {
        String root = marketRoot(trade);
        if (root == null || plan.getMarket() == null) return item("SESSION_INSTRUMENT_ALIGNMENT", PlanComparisonStatus.NOT_EVALUABLE, "Instrument alignment cannot be evaluated from the available structured data.");
        if (root.equals(plan.getMarket().name())) return item("SESSION_INSTRUMENT_ALIGNMENT", PlanComparisonStatus.ALIGNED, "Trade instrument matched the linked pre-analysis market.");
        return item("SESSION_INSTRUMENT_ALIGNMENT", PlanComparisonStatus.VIOLATED, "Trade instrument did not match the linked pre-analysis market.");
    }

    private PlanComparisonItemResponse noTradeComparison(AiPreAnalysis plan, PreAnalysisAiResult result, Trade trade) {
        if (!explicitNoTradePlan(plan, result)) return item("NO_TRADE_RULE", PlanComparisonStatus.NOT_EVALUABLE, "No structured no-trade rule was applicable.");
        return trade == null
                ? item("NO_TRADE_RULE", PlanComparisonStatus.ALIGNED, "The structured no-trade plan was respected.")
                : item("NO_TRADE_RULE", PlanComparisonStatus.VIOLATED, "A trade was taken despite a structured no-trade plan.");
    }

    private boolean explicitNoTradePlan(AiPreAnalysis plan) {
        return explicitNoTradePlan(plan, read(plan));
    }

    private boolean explicitNoTradePlan(AiPreAnalysis plan, PreAnalysisAiResult result) {
        boolean statusNoTrade = plan.getSetupStatus() == SetupStatus.NO_TRADE || (result != null && result.setupStatus() == SetupStatus.NO_TRADE);
        boolean entryNoTrade = result != null && result.entryAssessment() != null && result.entryAssessment().status() == EntryAssessmentStatus.NO_TRADE;
        boolean hasNoTradeScenario = result != null && result.noTradeScenario() != null && hasText(result.noTradeScenario().condition());
        return (statusNoTrade || entryNoTrade) && hasNoTradeScenario;
    }

    private PreAnalysisComparisonResponse response(AiPreAnalysis plan, List<PlanComparisonItemResponse> items) {
        long violations = items.stream().filter(i -> i.status() == PlanComparisonStatus.VIOLATED).count();
        long partial = items.stream().filter(i -> i.status() == PlanComparisonStatus.PARTIAL).count();
        long aligned = items.stream().filter(i -> i.status() == PlanComparisonStatus.ALIGNED).count();
        PlanAlignmentStatus status = violations > 0 ? PlanAlignmentStatus.PLAN_VIOLATED
                : partial > 0 ? PlanAlignmentStatus.PARTIAL_ALIGNMENT
                : aligned > 0 ? PlanAlignmentStatus.FULL_ALIGNMENT
                : PlanAlignmentStatus.NOT_EVALUABLE;
        return new PreAnalysisComparisonResponse(status, true, plan.getId(), plan.getCreatedAt(), plan.getMarket() == null ? null : plan.getMarket().name(), items);
    }

    private PlanComparisonItemResponse item(String type, PlanComparisonStatus status, String message) {
        return new PlanComparisonItemResponse(type, status, message);
    }

    private PreAnalysisAiResult read(AiPreAnalysis analysis) {
        if (analysis == null || analysis.getAnalysisJson() == null || analysis.getAnalysisJson().isBlank()) return null;
        try {
            return objectMapper.readValue(analysis.getAnalysisJson(), PreAnalysisAiResult.class);
        } catch (Exception ignored) {
            return null;
        }
    }

    private String marketRoot(Trade trade) {
        if (trade == null) return null;
        String symbol = Optional.ofNullable(trade.getContractSymbol()).orElseGet(() -> Optional.ofNullable(trade.getOriginalBrokerSymbol()).orElse(trade.getSymbol()));
        if (symbol == null) return null;
        String normalized = symbol.toUpperCase(Locale.ROOT);
        if (normalized.startsWith("MES") || normalized.startsWith("ES")) return PreAnalysisMarket.ES.name();
        if (normalized.startsWith("MNQ") || normalized.startsWith("NQ")) return PreAnalysisMarket.NQ.name();
        return null;
    }

    private boolean hasText(String value) {
        return value != null && !value.isBlank();
    }
}
