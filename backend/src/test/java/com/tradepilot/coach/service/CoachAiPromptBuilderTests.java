package com.tradepilot.coach.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.tradepilot.coach.dto.CoachDeterministicSummary;
import com.tradepilot.coach.entity.CoachSnapshotType;
import com.tradepilot.leakdetector.dto.BehavioralBaselineDto;
import com.tradepilot.leakdetector.dto.BehavioralCounterfactualDto;
import com.tradepilot.leakdetector.dto.BehavioralFindingDto;
import com.tradepilot.leakdetector.dto.EvidenceStrength;
import com.tradepilot.leakdetector.dto.LeakResultDto;
import com.tradepilot.leakdetector.dto.LeakSeverity;
import com.tradepilot.leakdetector.dto.LeakType;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class CoachAiPromptBuilderTests {
    private final CoachAiPromptBuilder promptBuilder = new CoachAiPromptBuilder(new ObjectMapper().findAndRegisterModules());

    @Test
    void suppliesStructuredDeterministicEvidenceAndSafetyBoundaries() throws Exception {
        String prompt = promptBuilder.buildPrompt(CoachSnapshotType.DAILY, CoachDeterministicSummary.emptyDaily("test"), behavioralLeak(false));

        assertThat(prompt).contains("POST_LOSS_OVERTRADING", "MODERATE", "insight-trust-v1", "counterfactual");
        assertThat(prompt).contains("Do not ask to discover behavioral mistakes");
        assertThat(prompt).contains("do not describe the counterfactual as savings or improvement");
        assertThat(prompt).doesNotContain("Review these trades and find behavioral mistakes");
    }

    @Test
    void systemPromptProhibitsUnsafeAdviceAndReplacementMetrics() {
        String prompt = promptBuilder.systemPrompt();

        assertThat(prompt).contains("Never change its finding type, evidence strength, occurrence count, sample size, threshold, metrics, or counterfactual values.");
        assertThat(prompt).contains("increasing size or leverage", "trading more to recover", "revenge trading", "widening stops", "violating account limits");
    }

    private LeakResultDto behavioralLeak(boolean supported) {
        LeakResultDto leak = LeakResultDto.of(LeakType.POST_LOSS_OVERTRADING, "Post-loss overtrading", "deterministic", "", 5, BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO, null, null, null, false, 70, LeakSeverity.HIGH, "Pause", 5, List.of(), LocalDateTime.now());
        leak.setBehavioralFinding(new BehavioralFindingDto(true, null, 10, 40, 10, EvidenceStrength.MODERATE,
                new BehavioralBaselineDto(30, new BigDecimal("55"), new BigDecimal("0.50"), new BigDecimal("100")),
                new BehavioralBaselineDto(10, new BigDecimal("20"), new BigDecimal("-0.50"), new BigDecimal("-100")),
                new BehavioralCounterfactualDto(true, supported, 4, 1, 3, new BigDecimal("300"), new BigDecimal("100"), BigDecimal.ZERO, supported ? new BigDecimal("200") : new BigDecimal("-200"), supported ? new BigDecimal("200") : new BigDecimal("-200")),
                "insight-trust-v1", "post-loss-overtrading-v1", null));
        return leak;
    }
}
