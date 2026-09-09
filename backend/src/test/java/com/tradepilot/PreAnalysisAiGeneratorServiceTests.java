package com.tradepilot;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.tradepilot.config.OpenAiProperties;
import com.tradepilot.observability.ObservabilityService;
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
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.web.client.RestTemplate;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class PreAnalysisAiGeneratorServiceTests {
    @Mock RestTemplate restTemplate;
    @Mock ObservabilityService observabilityService;
    PreAnalysisAiGeneratorService service;

    @BeforeEach
    void setUp() {
        ObjectMapper objectMapper = new ObjectMapper();
        OpenAiProperties openAiProperties = new OpenAiProperties();
        openAiProperties.setApiKey("test-key");
        openAiProperties.setModel("gpt-test");
        service = new PreAnalysisAiGeneratorService(
                restTemplate,
                objectMapper,
                openAiProperties,
                new PreAnalysisPromptBuilder(objectMapper),
                observabilityService,
                new PreAnalysisDecisionService()
        );
    }

    @Test
    void unknownAiSetupStatusIsIgnoredWithoutRetryingOrThrowing() throws Exception {
        when(restTemplate.exchange(
                eq("https://api.openai.com/v1/chat/completions"),
                eq(HttpMethod.POST),
                any(HttpEntity.class),
                eq(String.class)
        )).thenReturn(ResponseEntity.ok(openAiEnvelope(aiResultJsonWithInvalidSetupStatus())));

        PreAnalysisAiResult result = service.generate(42L, playbook(), PreAnalysisMarket.ES, PreAnalysisTimeframe.M5, "", new byte[] {1, 2, 3}, "image/png");

        assertThat(result.aiSetupStatus()).isEqualTo("WAITING_FOR_RETRACE");
        assertThat(result.setupStatus()).isEqualTo(SetupStatus.WAITING_FOR_RETRACE);
        assertThat(result.setupAlignmentScore()).isEqualTo(67);

        verify(restTemplate).exchange(
                eq("https://api.openai.com/v1/chat/completions"),
                eq(HttpMethod.POST),
                any(HttpEntity.class),
                eq(String.class)
        );
    }

    @Test
    void structurallyUnusableAiResponseFallsBackWithoutRetrying() throws Exception {
        when(restTemplate.exchange(
                eq("https://api.openai.com/v1/chat/completions"),
                eq(HttpMethod.POST),
                any(HttpEntity.class),
                eq(String.class)
        )).thenReturn(ResponseEntity.ok(openAiEnvelope("{}")));

        PreAnalysisAiResult result = service.generate(42L, playbook(), PreAnalysisMarket.ES, PreAnalysisTimeframe.M5, "", new byte[] {1, 2, 3}, "image/png");

        assertThat(result.setupStatus()).isEqualTo(SetupStatus.NO_TRADE);
        assertThat(result.warnings()).contains("AI response could not be safely interpreted, so TradePilot returned a no-trade fallback.");
    }

    @Test
    void waitingForConfirmationSetupStatusIsIgnoredAndCanonicalStatusWins() throws Exception {
        when(restTemplate.exchange(
                eq("https://api.openai.com/v1/chat/completions"),
                eq(HttpMethod.POST),
                any(HttpEntity.class),
                eq(String.class)
        )).thenReturn(ResponseEntity.ok(openAiEnvelope(aiResultJsonWithSetupStatus("WAITING_FOR_CONFIRMATION"))));

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

    private String openAiEnvelope(String contentJson) {
        return """
                {
                  "choices": [{"message": {"content": %s}}],
                  "usage": {"prompt_tokens": 1, "completion_tokens": 1, "total_tokens": 2}
                }
                """.formatted(quote(contentJson));
    }

    private String quote(String value) {
        try {
            return new ObjectMapper().writeValueAsString(value);
        } catch (Exception ex) {
            throw new IllegalStateException(ex);
        }
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
