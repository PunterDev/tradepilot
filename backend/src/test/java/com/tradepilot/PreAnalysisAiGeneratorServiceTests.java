package com.tradepilot;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.tradepilot.ai.openai.OpenAiGeneratedResponse;
import com.tradepilot.ai.openai.OpenAiResponsesClient;
import com.tradepilot.ai.openai.OpenAiUsage;
import com.tradepilot.preanalysis.dto.PreAnalysisDtos.PreAnalysisAiResult;
import com.tradepilot.preanalysis.dto.TradingPlaybookDto;
import com.tradepilot.preanalysis.entity.SetupStatus;
import com.tradepilot.preanalysis.entity.PreAnalysisMarket;
import com.tradepilot.preanalysis.entity.PreAnalysisTimeframe;
import com.tradepilot.preanalysis.service.PreAnalysisAiGeneratorService;
import com.tradepilot.preanalysis.service.PreAnalysisDecisionService;
import com.tradepilot.preanalysis.service.PreAnalysisPromptBuilder;
import java.time.LocalDateTime;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class PreAnalysisAiGeneratorServiceTests {
    @Mock OpenAiResponsesClient openAiResponsesClient;
    PreAnalysisAiGeneratorService service;

    @BeforeEach
    void setUp() {
        ObjectMapper objectMapper = new ObjectMapper();
        service = new PreAnalysisAiGeneratorService(
                objectMapper,
                openAiResponsesClient,
                new PreAnalysisPromptBuilder(objectMapper),
                new PreAnalysisDecisionService()
        );
        when(openAiResponsesClient.isConfigured()).thenReturn(true);
    }

    @Test
    void unknownAiSetupStatusIsIgnoredWithoutRetryingOrThrowing() throws Exception {
        when(openAiResponsesClient.generateJson(org.mockito.ArgumentMatchers.anyLong(), org.mockito.ArgumentMatchers.anyString(), org.mockito.ArgumentMatchers.anyString(), org.mockito.ArgumentMatchers.anyString(), org.mockito.ArgumentMatchers.any(), org.mockito.ArgumentMatchers.anyString()))
                .thenReturn(new OpenAiGeneratedResponse(aiResultJsonWithInvalidSetupStatus(), new OpenAiUsage(1, 0, 1, 0, 2)));

        PreAnalysisAiResult result = service.generate(42L, playbook(), PreAnalysisMarket.ES, PreAnalysisTimeframe.M5, "", new byte[] {1, 2, 3}, "image/png");

        assertThat(result.aiSetupStatus()).isEqualTo("WAITING_FOR_RETRACE");
        assertThat(result.setupStatus()).isEqualTo(SetupStatus.WAITING_FOR_RETRACE);
        assertThat(result.setupAlignmentScore()).isEqualTo(67);

    }

    @Test
    void structurallyUnusableAiResponseFallsBackWithoutRetrying() throws Exception {
        when(openAiResponsesClient.generateJson(org.mockito.ArgumentMatchers.anyLong(), org.mockito.ArgumentMatchers.anyString(), org.mockito.ArgumentMatchers.anyString(), org.mockito.ArgumentMatchers.anyString(), org.mockito.ArgumentMatchers.any(), org.mockito.ArgumentMatchers.anyString()))
                .thenReturn(new OpenAiGeneratedResponse("{}", new OpenAiUsage(1, 0, 1, 0, 2)));

        PreAnalysisAiResult result = service.generate(42L, playbook(), PreAnalysisMarket.ES, PreAnalysisTimeframe.M5, "", new byte[] {1, 2, 3}, "image/png");

        assertThat(result.setupStatus()).isEqualTo(SetupStatus.NO_TRADE);
        assertThat(result.warnings()).contains("AI response could not be safely interpreted, so TradePilot returned a no-trade fallback.");
    }

    @Test
    void waitingForConfirmationSetupStatusIsIgnoredAndCanonicalStatusWins() throws Exception {
        when(openAiResponsesClient.generateJson(org.mockito.ArgumentMatchers.anyLong(), org.mockito.ArgumentMatchers.anyString(), org.mockito.ArgumentMatchers.anyString(), org.mockito.ArgumentMatchers.anyString(), org.mockito.ArgumentMatchers.any(), org.mockito.ArgumentMatchers.anyString()))
                .thenReturn(new OpenAiGeneratedResponse(aiResultJsonWithSetupStatus("WAITING_FOR_CONFIRMATION"), new OpenAiUsage(1, 0, 1, 0, 2)));

        PreAnalysisAiResult result = service.generate(42L, playbook(), PreAnalysisMarket.ES, PreAnalysisTimeframe.M5, "", new byte[] {1, 2, 3}, "image/png");

        assertThat(result.aiSetupStatus()).isEqualTo("WAITING_FOR_RETRACE");
        assertThat(result.setupStatus()).isEqualTo(SetupStatus.WAITING_FOR_RETRACE);
        assertThat(result.setupAlignmentScore()).isEqualTo(67);
    }

    private TradingPlaybookDto playbook() {
        return new TradingPlaybookDto(
                10L,
                "Break + Acceptance",
                "Founder setup",
                List.of("ES"),
                List.of("15m"),
                List.of("5m"),
                List.of("1m"),
                List.of("Break", "Acceptance", "Displacement"),
                List.of("Do not chase"),
                List.of("Structural invalidation"),
                List.of("Next liquidity"),
                List.of("Chop"),
                "",
                true,
                LocalDateTime.now(),
                LocalDateTime.now()
        );
    }

    private String aiResultJsonWithInvalidSetupStatus() {
        return aiResultJsonWithSetupStatus("BOGUS");
    }

    private String aiResultJsonWithSetupStatus(String setupStatus) {
        return """
                {
                  "market": "ES",
                  "timeframe": "5m",
                  "playbookName": "Break + Acceptance",
                  "marketState": "TEST",
                  "structureQualityScore": 7.5,
                  "directionalContext": {"bias": "NEUTRAL", "confidenceLabel": "LOW", "reasoning": "Test fixture."},
                  "setupStatus": "%s",
                  "conditions": {
                    "break": {"status": "SATISFIED", "evidence": "Confirmed break."},
                    "acceptance": {"status": "SATISFIED", "evidence": "Confirmed acceptance."},
                    "displacement": {"status": "SATISFIED", "evidence": "Displacement."},
                    "fvgIfvg": {"status": "SATISFIED", "evidence": "Imbalance."},
                    "retrace": {"status": "WAITING", "evidence": "No retrace."},
                    "confirmation": {"status": "WAITING", "evidence": "No confirmation."}
                  },
                  "setupAlignmentScore": 0,
                  "importantLevels": [{"label": "Previous Week Low", "price": 7655.0, "role": "BROKEN_SUPPORT"}],
                  "entryAssessment": {"status": "NO_TRADE", "summary": "No trade.", "whatMustHappenNext": []},
                  "invalidation": {"summary": "Acceptance above structure invalidates.", "levels": [7655, 7691.25]},
                  "potentialTargets": [],
                  "ruleViolations": [],
                  "warnings": [],
                  "narrative": "Test narrative."
                }
                """.formatted(setupStatus);
    }
}
