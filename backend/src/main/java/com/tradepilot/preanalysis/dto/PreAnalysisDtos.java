package com.tradepilot.preanalysis.dto;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonDeserializer;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.tradepilot.preanalysis.entity.*;
import java.io.IOException;
import java.time.LocalDateTime;
import java.util.List;

public final class PreAnalysisDtos {
    private static final org.slf4j.Logger log = org.slf4j.LoggerFactory.getLogger(PreAnalysisDtos.class);

    private PreAnalysisDtos() {}

    public record AnalysisResponse(
            Long id,
            PreAnalysisMarket market,
            PreAnalysisTimeframe timeframe,
            String displayTimeframe,
            Long playbookId,
            String playbookName,
            String imageUrl,
            String traderNote,
            SetupStatus setupStatus,
            Bias bias,
            Integer setupAlignmentScore,
            Double structureQualityScore,
            PreAnalysisAiResult analysis,
            FeedbackResponse feedback,
            LocalDateTime createdAt
    ) {}

    public record AnalysisSummaryResponse(
            Long id,
            PreAnalysisMarket market,
            PreAnalysisTimeframe timeframe,
            String displayTimeframe,
            SetupStatus setupStatus,
            Bias bias,
            Integer setupAlignmentScore,
            Integer feedbackRating,
            LocalDateTime createdAt
    ) {}

    public record FeedbackRequest(Integer rating, FeedbackReasonCode reasonCode, String comment) {}
    public record FeedbackResponse(Long id, Integer rating, FeedbackReasonCode reasonCode, String comment, LocalDateTime createdAt) {}

    public record PreAnalysisAiResult(
            @JsonDeserialize(using = PreAnalysisMarketDeserializer.class) PreAnalysisMarket market,
            String timeframe,
            String playbookName,
            String marketState,
            Double structureQualityScore,
            DirectionalContext directionalContext,
            @JsonProperty("setupStatus") String aiSetupStatus,
            Conditions conditions,
            Integer setupAlignmentScore,
            List<ImportantLevel> importantLevels,
            EntryAssessment entryAssessment,
            Invalidation invalidation,
            List<PotentialTarget> potentialTargets,
            List<String> ruleViolations,
            List<String> warnings,
            String narrative,
            MarketRead marketRead,
            ContextScores contextScores,
            List<KeyLevel> keyLevels,
            Scenario bullishScenario,
            Scenario bearishScenario,
            NoTradeScenario noTradeScenario,
            TradePlan tradePlan,
            CrossMarket crossMarket
    ) {
        public PreAnalysisAiResult(
                PreAnalysisMarket market,
                String timeframe,
                String playbookName,
                String marketState,
                Double structureQualityScore,
                DirectionalContext directionalContext,
                SetupStatus setupStatus,
                Conditions conditions,
                Integer setupAlignmentScore,
                List<ImportantLevel> importantLevels,
                EntryAssessment entryAssessment,
                Invalidation invalidation,
                List<PotentialTarget> potentialTargets,
                List<String> ruleViolations,
                List<String> warnings,
                String narrative
        ) {
            this(market, timeframe, playbookName, marketState, structureQualityScore, directionalContext, setupStatus == null ? null : setupStatus.name(), conditions, setupAlignmentScore, importantLevels, entryAssessment, invalidation, potentialTargets, ruleViolations, warnings, narrative, null, null, null, null, null, null, null, null);
        }

        public PreAnalysisAiResult(
                PreAnalysisMarket market,
                String timeframe,
                String playbookName,
                String marketState,
                Double structureQualityScore,
                DirectionalContext directionalContext,
                SetupStatus setupStatus,
                Conditions conditions,
                Integer setupAlignmentScore,
                List<ImportantLevel> importantLevels,
                EntryAssessment entryAssessment,
                Invalidation invalidation,
                List<PotentialTarget> potentialTargets,
                List<String> ruleViolations,
                List<String> warnings,
                String narrative,
                MarketRead marketRead,
                ContextScores contextScores,
                List<KeyLevel> keyLevels,
                Scenario bullishScenario,
                Scenario bearishScenario,
                NoTradeScenario noTradeScenario,
                TradePlan tradePlan,
                CrossMarket crossMarket
        ) {
            this(market, timeframe, playbookName, marketState, structureQualityScore, directionalContext, setupStatus == null ? null : setupStatus.name(), conditions, setupAlignmentScore, importantLevels, entryAssessment, invalidation, potentialTargets, ruleViolations, warnings, narrative, marketRead, contextScores, keyLevels, bullishScenario, bearishScenario, noTradeScenario, tradePlan, crossMarket);
        }

        public SetupStatus setupStatus() {
            return parseEnum(SetupStatus.class, aiSetupStatus, "setupStatus");
        }
    }

    public record DirectionalContext(@JsonDeserialize(using = BiasDeserializer.class) Bias bias, String confidenceLabel, String reasoning) {}
    public record Conditions(@JsonProperty("break") ConditionEvidence breakCondition, ConditionEvidence acceptance, ConditionEvidence displacement, ConditionEvidence fvgIfvg, ConditionEvidence retrace, ConditionEvidence confirmation) {}
    public record ConditionEvidence(@JsonDeserialize(using = ConditionStatusDeserializer.class) ConditionStatus status, String evidence) {}
    public record ImportantLevel(String label, Double price, String role) {}
    public record EntryAssessment(@JsonDeserialize(using = EntryAssessmentStatusDeserializer.class) EntryAssessmentStatus status, String summary, List<String> whatMustHappenNext) {}
    public record Invalidation(String summary, List<Double> levels) {}
    public record PotentialTarget(Double price, String reason) {}
    public record MarketRead(String marketBias, String marketState, String summary, String entryPosture) {}
    public record ContextScores(QualitativeScore structure, QualitativeScore location, QualitativeScore confirmation, QualitativeScore relativeStrength) {}
    public record QualitativeScore(Integer score, String label, String reason) {}
    public record KeyLevel(Double price, String label, String category, String role, String importance, String reason) {}
    public record Scenario(String status, String thesis, Double entryZoneLow, Double entryZoneHigh, String trigger, List<String> confirmations, String invalidation, List<ScenarioTarget> targets, String quality, List<String> notes) {}
    public record ScenarioTarget(Double price, String reason, Integer order) {}
    public record NoTradeScenario(String condition, String reason) {}
    public record TradePlan(String preferredScenario, String waitFor, List<String> confirmationNeeded, List<String> avoid, String invalidation, String targetSummary) {}
    public record CrossMarket(Boolean available, String instrument, String comparisonInstrument, String leader, String laggard, String relativeStrengthState, String smtStatus, String interpretation) {}

    public static final class PreAnalysisMarketDeserializer extends JsonDeserializer<PreAnalysisMarket> {
        @Override
        public PreAnalysisMarket deserialize(JsonParser parser, DeserializationContext context) throws IOException {
            return parseEnum(PreAnalysisMarket.class, parser.getValueAsString(), "market");
        }
    }

    public static final class BiasDeserializer extends JsonDeserializer<Bias> {
        @Override
        public Bias deserialize(JsonParser parser, DeserializationContext context) throws IOException {
            return parseEnum(Bias.class, parser.getValueAsString(), "directionalContext.bias");
        }
    }

    public static final class ConditionStatusDeserializer extends JsonDeserializer<ConditionStatus> {
        @Override
        public ConditionStatus deserialize(JsonParser parser, DeserializationContext context) throws IOException {
            return parseEnum(ConditionStatus.class, parser.getValueAsString(), "conditions.status");
        }
    }

    public static final class EntryAssessmentStatusDeserializer extends JsonDeserializer<EntryAssessmentStatus> {
        @Override
        public EntryAssessmentStatus deserialize(JsonParser parser, DeserializationContext context) throws IOException {
            return parseEnum(EntryAssessmentStatus.class, parser.getValueAsString(), "entryAssessment.status");
        }
    }

    private static <T extends Enum<T>> T parseEnum(Class<T> type, String rawValue, String field) {
        if (rawValue == null || rawValue.isBlank()) {
            return null;
        }
        String normalized = rawValue.trim().replace(' ', '_').replace('-', '_').toUpperCase();
        try {
            T parsed = Enum.valueOf(type, normalized);
            if (!normalized.equals(rawValue)) {
                log.warn("Pre-analysis AI value normalized. field={} rawValue={} normalizedValue={}", field, rawValue, normalized);
            }
            return parsed;
        } catch (IllegalArgumentException ex) {
            log.warn("Pre-analysis AI value normalized. field={} rawValue={} normalizedValue=null", field, rawValue);
            return null;
        }
    }
}
