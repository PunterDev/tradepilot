package com.tradepilot.preanalysis.service;

import com.tradepilot.preanalysis.dto.PreAnalysisDtos.*;
import com.tradepilot.preanalysis.entity.ConditionStatus;
import com.tradepilot.preanalysis.entity.SetupStatus;
import org.springframework.stereotype.Service;
import java.util.List;
import java.util.Set;

@Service
public class PreAnalysisDecisionService {
    private static final org.slf4j.Logger log = org.slf4j.LoggerFactory.getLogger(PreAnalysisDecisionService.class);
    private static final int SCORED_CONDITION_COUNT = 6;
    private static final Set<String> ENTRY_POSTURES = Set.of("FAVORABLE", "ACCEPTABLE", "POOR_TO_CHASE", "WAIT_FOR_RETRACEMENT", "WAIT_FOR_CONFIRMATION", "NO_TRADE");

    public PreAnalysisAiResult normalizeAndValidate(PreAnalysisAiResult result) {
        validate(result);
        int satisfiedCount = satisfiedCount(result.conditions());
        SetupStatus normalizedStatus = statusFromConditions(result.conditions(), satisfiedCount);
        int score = Math.round((satisfiedCount * 100.0f) / SCORED_CONDITION_COUNT);
        return new PreAnalysisAiResult(
                result.market(),
                result.timeframe(),
                result.playbookName(),
                result.marketState(),
                result.structureQualityScore(),
                result.directionalContext(),
                normalizedStatus,
                result.conditions(),
                score,
                nullToEmpty(result.importantLevels()),
                result.entryAssessment(),
                result.invalidation(),
                nullToEmpty(result.potentialTargets()),
                nullToEmpty(result.ruleViolations()),
                nullToEmpty(result.warnings()),
                result.narrative(),
                normalizeMarketRead(result.marketRead(), result.contextScores()),
                normalizeContextScores(result.contextScores()),
                normalizeKeyLevels(result.keyLevels()),
                normalizeScenario(result.bullishScenario(), normalizedStatus),
                normalizeScenario(result.bearishScenario(), normalizedStatus),
                result.noTradeScenario(),
                normalizeTradePlan(result.tradePlan()),
                normalizeCrossMarket(result.crossMarket())
        );
    }

    public int satisfiedCount(Conditions conditions) {
        return (int) List.of(
                conditions.breakCondition(),
                conditions.acceptance(),
                conditions.displacement(),
                conditions.fvgIfvg(),
                conditions.retrace(),
                conditions.confirmation()
        ).stream().filter(this::isSatisfied).count();
    }

    public void validate(PreAnalysisAiResult result) {
        if (result == null || result.directionalContext() == null || result.entryAssessment() == null || result.conditions() == null) {
            throw new IllegalArgumentException("AI response did not match the pre-analysis schema.");
        }
        if (result.conditions().breakCondition() == null || result.conditions().acceptance() == null || result.conditions().displacement() == null || result.conditions().fvgIfvg() == null || result.conditions().retrace() == null || result.conditions().confirmation() == null) {
            throw new IllegalArgumentException("AI response omitted required setup conditions.");
        }
    }

    private SetupStatus statusFromConditions(Conditions conditions, int satisfiedCount) {
        boolean breakSatisfied = isSatisfied(conditions.breakCondition());
        boolean acceptanceSatisfied = isSatisfied(conditions.acceptance());
        boolean displacementSatisfied = isSatisfied(conditions.displacement());
        boolean fvgSatisfied = isSatisfied(conditions.fvgIfvg());
        boolean retraceSatisfied = isSatisfied(conditions.retrace());
        boolean confirmationSatisfied = isSatisfied(conditions.confirmation());

        if (breakSatisfied && acceptanceSatisfied && displacementSatisfied && fvgSatisfied && retraceSatisfied && confirmationSatisfied) {
            return SetupStatus.ENTRY_VALID;
        }
        if (breakSatisfied && acceptanceSatisfied && displacementSatisfied && fvgSatisfied && !retraceSatisfied) {
            return SetupStatus.WAITING_FOR_RETRACE;
        }
        if (breakSatisfied && acceptanceSatisfied && displacementSatisfied) {
            return SetupStatus.ARMED;
        }
        if (satisfiedCount > 0 || hasPartialCondition(conditions)) {
            return SetupStatus.SETUP_FORMING;
        }
        if (!hasFailedCondition(conditions)) {
            return SetupStatus.WATCHING;
        }
        return SetupStatus.NO_TRADE;
    }

    private boolean isSatisfied(ConditionEvidence evidence) {
        return evidence.status() == ConditionStatus.SATISFIED;
    }

    private boolean hasPartialCondition(Conditions conditions) {
        return List.of(
                conditions.breakCondition(),
                conditions.acceptance(),
                conditions.displacement(),
                conditions.fvgIfvg(),
                conditions.retrace(),
                conditions.confirmation()
        ).stream().anyMatch(evidence -> evidence.status() == ConditionStatus.PARTIAL);
    }

    private boolean hasFailedCondition(Conditions conditions) {
        return List.of(
                conditions.breakCondition(),
                conditions.acceptance(),
                conditions.displacement(),
                conditions.fvgIfvg(),
                conditions.retrace(),
                conditions.confirmation()
        ).stream().anyMatch(evidence -> evidence.status() == ConditionStatus.FAILED);
    }

    private MarketRead normalizeMarketRead(MarketRead marketRead, ContextScores scores) {
        if (marketRead == null) {
            return null;
        }
        String marketBias = cleanUpper(marketRead.marketBias());
        String summary = marketRead.summary();
        if (marketBias != null && summary != null) {
            String lowerSummary = summary.toLowerCase();
            if (marketBias.contains("BULLISH") && lowerSummary.contains("strong bearish")) {
                marketBias = "NEUTRAL";
            } else if (marketBias.contains("BEARISH") && lowerSummary.contains("strong bullish")) {
                marketBias = "NEUTRAL";
            }
        }
        String entryPosture = normalizeEntryPosture(marketRead.entryPosture(), scores);
        return new MarketRead(marketBias, cleanUpper(marketRead.marketState()), summary, entryPosture);
    }

    private ContextScores normalizeContextScores(ContextScores scores) {
        if (scores == null) {
            return null;
        }
        return new ContextScores(
                normalizeScore(scores.structure()),
                normalizeScore(scores.location()),
                normalizeScore(scores.confirmation()),
                normalizeScore(scores.relativeStrength())
        );
    }

    private QualitativeScore normalizeScore(QualitativeScore score) {
        if (score == null) {
            return null;
        }
        return new QualitativeScore(clamp(score.score(), 0, 10), cleanUpper(score.label()), score.reason());
    }

    private List<KeyLevel> normalizeKeyLevels(List<KeyLevel> levels) {
        return nullToEmpty(levels).stream()
                .filter(level -> level != null && level.price() != null)
                .map(level -> new KeyLevel(level.price(), level.label(), cleanUpper(level.category()), cleanUpper(level.role()), cleanUpper(level.importance()), level.reason()))
                .distinct()
                .toList();
    }

    private Scenario normalizeScenario(Scenario scenario, SetupStatus authoritativeStatus) {
        if (scenario == null) {
            return null;
        }
        Double low = scenario.entryZoneLow();
        Double high = scenario.entryZoneHigh();
        if (low != null && high != null && low > high) {
            double previousLow = low;
            low = high;
            high = previousLow;
        }
        String status = cleanUpper(scenario.status());
        if ("READY".equals(status) && authoritativeStatus != SetupStatus.ENTRY_VALID) {
            status = "WAITING_FOR_CONFIRMATION";
        }
        return new Scenario(
                status,
                scenario.thesis(),
                low,
                high,
                scenario.trigger(),
                nullToEmpty(scenario.confirmations()),
                scenario.invalidation(),
                normalizeScenarioTargets(scenario.targets()),
                cleanUpper(scenario.quality()),
                nullToEmpty(scenario.notes())
        );
    }

    private List<ScenarioTarget> normalizeScenarioTargets(List<ScenarioTarget> targets) {
        return nullToEmpty(targets).stream()
                .filter(target -> target != null && target.price() != null)
                .map(target -> new ScenarioTarget(target.price(), target.reason(), target.order()))
                .distinct()
                .toList();
    }

    private TradePlan normalizeTradePlan(TradePlan tradePlan) {
        if (tradePlan == null) {
            return null;
        }
        return new TradePlan(
                cleanUpper(tradePlan.preferredScenario()),
                tradePlan.waitFor(),
                nullToEmpty(tradePlan.confirmationNeeded()),
                nullToEmpty(tradePlan.avoid()),
                tradePlan.invalidation(),
                tradePlan.targetSummary()
        );
    }

    private CrossMarket normalizeCrossMarket(CrossMarket crossMarket) {
        if (crossMarket == null) {
            return null;
        }
        boolean available = Boolean.TRUE.equals(crossMarket.available());
        String smtStatus = cleanUpper(crossMarket.smtStatus());
        if (!available && smtStatus != null && smtStatus.startsWith("CONFIRMED_")) {
            smtStatus = "NOT_ENOUGH_DATA";
        }
        return new CrossMarket(
                available,
                cleanUpper(crossMarket.instrument()),
                cleanUpper(crossMarket.comparisonInstrument()),
                available ? cleanUpper(crossMarket.leader()) : "UNKNOWN",
                available ? cleanUpper(crossMarket.laggard()) : "UNKNOWN",
                available ? cleanUpper(crossMarket.relativeStrengthState()) : "NOT_ENOUGH_DATA",
                smtStatus == null ? "NOT_ENOUGH_DATA" : smtStatus,
                crossMarket.interpretation()
        );
    }

    private String normalizeEntryPosture(String entryPosture, ContextScores scores) {
        String normalized = cleanUpper(entryPosture);
        if ("WAITING_FOR_CONFIRMATION".equals(normalized)) {
            log.warn("Pre-analysis AI value normalized. field=entryPosture rawValue={} normalizedValue=WAIT_FOR_CONFIRMATION", entryPosture);
            normalized = "WAIT_FOR_CONFIRMATION";
        }
        if (normalized != null && !ENTRY_POSTURES.contains(normalized)) {
            log.warn("Pre-analysis AI value normalized. field=entryPosture rawValue={} normalizedValue=null", entryPosture);
            normalized = null;
        }
        Integer locationScore = scores == null || scores.location() == null ? null : scores.location().score();
        if ("FAVORABLE".equals(normalized) && locationScore != null && locationScore <= 3) {
            return "POOR_TO_CHASE";
        }
        return normalized;
    }

    private Integer clamp(Integer value, int min, int max) {
        if (value == null) {
            return null;
        }
        return Math.max(min, Math.min(max, value));
    }

    private String cleanUpper(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        return value.trim().replace(' ', '_').replace('-', '_').toUpperCase();
    }

    private <T> List<T> nullToEmpty(List<T> values) {
        return values == null ? List.of() : values;
    }
}
