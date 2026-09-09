package com.tradepilot;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.tradepilot.preanalysis.dto.PreAnalysisDtos.PreAnalysisAiResult;
import com.tradepilot.preanalysis.service.PreAnalysisDecisionService;
import java.util.List;
import java.util.stream.Stream;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import static org.assertj.core.api.Assertions.assertThat;

class PreAnalysisDtoContractTests {
    ObjectMapper objectMapper = new ObjectMapper().findAndRegisterModules();

    @ParameterizedTest
    @MethodSource("invalidationLevelVariants")
    void preAnalysisAiResultDeserializesNumericInvalidationLevels(String levelsJson, List<Double> expected) throws Exception {
        PreAnalysisAiResult result = objectMapper.readValue(aiResultJson(levelsJson), PreAnalysisAiResult.class);

        assertThat(result.invalidation().levels()).containsExactlyElementsOf(expected);
        assertThat(result.invalidation().levels()).allSatisfy(level -> assertThat(level).isInstanceOf(Double.class));
        assertThat(result.importantLevels()).hasSize(1);
        assertThat(result.importantLevels().get(0).price()).isEqualTo(7655.0);
    }

    @Test
    void richTraderAnalysisDeserializesAndNormalizes() throws Exception {
        PreAnalysisAiResult parsed = objectMapper.readValue(richAiResultJson(), PreAnalysisAiResult.class);
        PreAnalysisAiResult normalized = new PreAnalysisDecisionService().normalizeAndValidate(parsed);

        assertThat(normalized.marketRead().marketBias()).isEqualTo("BULLISH");
        assertThat(normalized.contextScores().structure().score()).isEqualTo(8);
        assertThat(normalized.contextScores().location().score()).isEqualTo(4);
        assertThat(normalized.keyLevels()).extracting("label").contains("Previous Day High", "Daily Open");
        assertThat(normalized.bullishScenario().targets()).extracting("price").containsExactly(29245.0, 29292.75);
        assertThat(normalized.bearishScenario().confirmations()).contains("PDH lost with bearish displacement");
        assertThat(normalized.noTradeScenario().reason()).contains("meaningful displacement");
        assertThat(normalized.tradePlan().avoid()).contains("Do not chase current expansion.");
        assertThat(normalized.crossMarket().available()).isFalse();
        assertThat(normalized.crossMarket().smtStatus()).isEqualTo("NOT_ENOUGH_DATA");
    }

    @Test
    void aiSetupStatusWaitingForConfirmationDoesNotBreakDeserialization() throws Exception {
        String json = richAiResultJson().replace("\"setupStatus\": \"WAITING_FOR_RETRACE\"", "\"setupStatus\": \"WAITING_FOR_CONFIRMATION\"");

        PreAnalysisAiResult parsed = objectMapper.readValue(json, PreAnalysisAiResult.class);
        PreAnalysisAiResult normalized = new PreAnalysisDecisionService().normalizeAndValidate(parsed);

        assertThat(parsed.aiSetupStatus()).isEqualTo("WAITING_FOR_CONFIRMATION");
        assertThat(parsed.setupStatus()).isNull();
        assertThat(normalized.setupStatus()).isEqualTo(com.tradepilot.preanalysis.entity.SetupStatus.WAITING_FOR_RETRACE);
        assertThat(normalized.setupAlignmentScore()).isEqualTo(67);

        String persistedJson = objectMapper.writeValueAsString(normalized);
        assertThat(persistedJson).contains("\"setupStatus\":\"WAITING_FOR_RETRACE\"");
        assertThat(persistedJson).doesNotContain("aiSetupStatus");
    }

    @Test
    void aiSetupStatusEntryValidCannotOverrideWatchingConditions() throws Exception {
        String json = aiResultJson("[]")
                .replace("\"setupStatus\": \"NO_TRADE\"", "\"setupStatus\": \"ENTRY_VALID\"")
                .replace("\"FAILED\"", "\"WAITING\"");

        PreAnalysisAiResult parsed = objectMapper.readValue(json, PreAnalysisAiResult.class);
        PreAnalysisAiResult normalized = new PreAnalysisDecisionService().normalizeAndValidate(parsed);

        assertThat(normalized.setupStatus()).isEqualTo(com.tradepilot.preanalysis.entity.SetupStatus.WATCHING);
        assertThat(normalized.setupAlignmentScore()).isZero();
    }

    @Test
    void entryPostureAllowsCanonicalWaitForConfirmationAndAliasesLegacyPhrase() throws Exception {
        String canonical = richAiResultJson().replace("\"WAIT_FOR_RETRACEMENT\"", "\"WAIT_FOR_CONFIRMATION\"");
        String legacy = richAiResultJson().replace("\"WAIT_FOR_RETRACEMENT\"", "\"WAITING_FOR_CONFIRMATION\"");

        PreAnalysisAiResult canonicalResult = new PreAnalysisDecisionService().normalizeAndValidate(objectMapper.readValue(canonical, PreAnalysisAiResult.class));
        PreAnalysisAiResult legacyResult = new PreAnalysisDecisionService().normalizeAndValidate(objectMapper.readValue(legacy, PreAnalysisAiResult.class));

        assertThat(canonicalResult.marketRead().entryPosture()).isEqualTo("WAIT_FOR_CONFIRMATION");
        assertThat(legacyResult.marketRead().entryPosture()).isEqualTo("WAIT_FOR_CONFIRMATION");
    }

    @Test
    void unknownQualitativeEntryPostureDoesNotFailAnalysis() throws Exception {
        String json = richAiResultJson().replace("\"WAIT_FOR_RETRACEMENT\"", "\"BANANA_MODE\"");

        PreAnalysisAiResult normalized = new PreAnalysisDecisionService().normalizeAndValidate(objectMapper.readValue(json, PreAnalysisAiResult.class));

        assertThat(normalized.marketRead().entryPosture()).isNull();
        assertThat(normalized.setupStatus()).isEqualTo(com.tradepilot.preanalysis.entity.SetupStatus.WAITING_FOR_RETRACE);
    }

    static Stream<Arguments> invalidationLevelVariants() {
        return Stream.of(
                Arguments.of("[7691.25]", List.of(7691.25)),
                Arguments.of("[7655]", List.of(7655.0)),
                Arguments.of("[7655, 7691.25]", List.of(7655.0, 7691.25)),
                Arguments.of("[]", List.of())
        );
    }

    private String aiResultJson(String levelsJson) {
        return """
                {
                  "market": "ES",
                  "timeframe": "5m",
                  "playbookName": "Break + Acceptance + Displacement + FVG/IFVG Retracement",
                  "marketState": "TEST",
                  "structureQualityScore": 7.5,
                  "directionalContext": {"bias": "NEUTRAL", "confidenceLabel": "LOW", "reasoning": "Test fixture."},
                  "setupStatus": "NO_TRADE",
                  "conditions": {
                    "break": {"status": "FAILED", "evidence": "No confirmed break."},
                    "acceptance": {"status": "FAILED", "evidence": "No acceptance."},
                    "displacement": {"status": "FAILED", "evidence": "No displacement."},
                    "fvgIfvg": {"status": "FAILED", "evidence": "No imbalance."},
                    "retrace": {"status": "FAILED", "evidence": "No retrace."},
                    "confirmation": {"status": "FAILED", "evidence": "No confirmation."}
                  },
                  "setupAlignmentScore": 0,
                  "importantLevels": [{"label": "Previous Week Low", "price": 7655.0, "role": "BROKEN_SUPPORT"}],
                  "entryAssessment": {"status": "NO_TRADE", "summary": "No trade.", "whatMustHappenNext": []},
                  "invalidation": {"summary": "Acceptance above structure invalidates the setup.", "levels": %s},
                  "potentialTargets": [],
                  "ruleViolations": [],
                  "warnings": [],
                  "narrative": "Test narrative."
                }
                """.formatted(levelsJson);
    }

    private String richAiResultJson() {
        return """
                {
                  "market": "NQ",
                  "timeframe": "5m",
                  "playbookName": "Break + Acceptance + Displacement + FVG/IFVG Retracement",
                  "marketState": "BULLISH_EXPANSION",
                  "structureQualityScore": 8.0,
                  "directionalContext": {"bias": "BULLISH", "confidenceLabel": "MODERATE", "reasoning": "Bullish displacement through Daily Open and PDH, but price is extended."},
                  "setupStatus": "WAITING_FOR_RETRACE",
                  "conditions": {
                    "break": {"status": "SATISFIED", "evidence": "PDH broke with intent."},
                    "acceptance": {"status": "SATISFIED", "evidence": "Held above the broken level."},
                    "displacement": {"status": "SATISFIED", "evidence": "Strong bullish candle expanded through resistance."},
                    "fvgIfvg": {"status": "SATISFIED", "evidence": "Visible imbalance after displacement."},
                    "retrace": {"status": "WAITING", "evidence": "No clean retracement yet."},
                    "confirmation": {"status": "WAITING", "evidence": "No renewed displacement after retrace."}
                  },
                  "setupAlignmentScore": 0,
                  "importantLevels": [{"label": "Previous Day High", "price": 29207.75, "role": "DECISION_LEVEL"}],
                  "entryAssessment": {"status": "WAIT", "summary": "Bullish structure, poor location. Do not chase.", "whatMustHappenNext": ["Retrace into PDH/Daily Open", "Bullish displacement after retracement"]},
                  "invalidation": {"summary": "Acceptance below Daily Open invalidates the bullish retracement idea.", "levels": [29177.0]},
                  "potentialTargets": [{"price": 29245.0, "reason": "nearby upside liquidity"}],
                  "ruleViolations": [],
                  "warnings": ["Do not chase current expansion."],
                  "narrative": "Strong bullish displacement has occurred through Daily Open and PDH, but current price is extended into resistance.",
                  "marketRead": {
                    "marketBias": "BULLISH",
                    "marketState": "BULLISH_EXPANSION",
                    "summary": "Bullish structure is intact, but current location is poor directly beneath resistance.",
                    "entryPosture": "WAIT_FOR_RETRACEMENT"
                  },
                  "contextScores": {
                    "structure": {"score": 8, "label": "BULLISH", "reason": "Strong displacement through Daily Open and PDH."},
                    "location": {"score": 4, "label": "POOR_TO_CHASE", "reason": "Price is extended into resistance."},
                    "confirmation": {"score": 3, "label": "NOT_CONFIRMED", "reason": "No retracement and reclaim confirmation yet."},
                    "relativeStrength": null
                  },
                  "keyLevels": [
                    {"price": 29207.75, "label": "Previous Day High", "category": "PREVIOUS_DAY_HIGH", "role": "DECISION_LEVEL", "importance": "HIGH", "reason": "Broken level that should act as support if bullish continuation is valid."},
                    {"price": 29177.0, "label": "Daily Open", "category": "DAILY_OPEN", "role": "SUPPORT", "importance": "HIGH", "reason": "Structural pivot and bullish invalidation area."}
                  ],
                  "bullishScenario": {
                    "status": "WAITING_FOR_CONFIRMATION",
                    "thesis": "Bullish continuation becomes more credible after retracement into PDH/Daily Open followed by displacement.",
                    "entryZoneLow": 29177.0,
                    "entryZoneHigh": 29207.75,
                    "trigger": "Bullish MSS/displacement after retracement.",
                    "confirmations": ["Sell-side taken into support", "Bullish displacement away from the retracement zone"],
                    "invalidation": "Acceptance below Daily Open.",
                    "targets": [{"price": 29245.0, "reason": "nearby upside liquidity", "order": 1}, {"price": 29292.75, "reason": "London high", "order": 2}],
                    "quality": "MEDIUM",
                    "notes": ["Do not chase current expansion."]
                  },
                  "bearishScenario": {
                    "status": "WAITING_FOR_CONFIRMATION",
                    "thesis": "If PDH fails and Daily Open is lost, treat the breakout as failed.",
                    "entryZoneLow": null,
                    "entryZoneHigh": null,
                    "trigger": "Failed reclaim below PDH.",
                    "confirmations": ["PDH lost with bearish displacement", "Daily Open acceptance lower"],
                    "invalidation": "Reclaim and acceptance above PDH.",
                    "targets": [{"price": 29120.0, "reason": "lower liquidity", "order": 1}],
                    "quality": "LOW",
                    "notes": []
                  },
                  "noTradeScenario": {"condition": "Price remains between PDH and Daily Open.", "reason": "No trade while price is trapped without meaningful displacement."},
                  "tradePlan": {
                    "preferredScenario": "BULLISH_RETRACEMENT",
                    "waitFor": "Retracement into PDH/Daily Open followed by bullish displacement.",
                    "confirmationNeeded": ["Bullish displacement after retracement", "Acceptance back above the decision level"],
                    "avoid": ["Do not chase current expansion."],
                    "invalidation": "Sustained acceptance below Daily Open.",
                    "targetSummary": "Use the next visible liquidity levels above."
                  },
                  "crossMarket": {
                    "available": false,
                    "instrument": "NQ",
                    "comparisonInstrument": "ES",
                    "leader": "UNKNOWN",
                    "laggard": "UNKNOWN",
                    "relativeStrengthState": "NOT_ENOUGH_DATA",
                    "smtStatus": "CONFIRMED_BEARISH",
                    "interpretation": "Correlated ES context was not supplied."
                  }
                }
                """;
    }
}
