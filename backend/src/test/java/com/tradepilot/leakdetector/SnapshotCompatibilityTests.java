package com.tradepilot.leakdetector;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
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

class SnapshotCompatibilityTests {
    private final ObjectMapper objectMapper = new ObjectMapper().findAndRegisterModules();

    @Test
    void legacyLeakSnapshotWithoutTrustFieldsDeserializes() throws Exception {
        String legacy = "[{\"leakType\":\"OVERTRADING\",\"title\":\"Overtrading Leak\",\"summary\":\"Legacy result\",\"tradeCount\":4,\"winRate\":25.00,\"totalPnl\":-400.00,\"avgPnl\":-100.00,\"estimatedMonthlyImpact\":-500.00,\"confidenceScore\":61,\"severity\":\"MEDIUM\",\"recommendedFix\":\"Legacy rule\",\"relatedTradeIds\":[],\"generatedAt\":\"2026-01-01T10:00:00\"}]";
        LeakResultDto result = objectMapper.readValue(legacy, new TypeReference<List<LeakResultDto>>() {}).get(0);
        assertThat(result.getTitle()).isEqualTo("Overtrading Leak");
        assertThat(result.getBehavioralFinding()).isNull();
    }

    @Test
    void newTrustFindingRoundTripsThroughSnapshotJson() throws Exception {
        LeakResultDto result = LeakResultDto.of(LeakType.POST_LOSS_OVERTRADING, "Post-loss overtrading", "Structured result", "", 10, BigDecimal.valueOf(20), BigDecimal.valueOf(-300), BigDecimal.valueOf(-30), BigDecimal.valueOf(-500), null, null, null, false, 70, LeakSeverity.HIGH, "Pause", 10, List.of(), LocalDateTime.of(2026, 1, 1, 10, 0));
        result.setBehavioralFinding(new BehavioralFindingDto(true, null, 10, 89, 12, EvidenceStrength.MODERATE,
                new BehavioralBaselineDto(70, BigDecimal.valueOf(51), BigDecimal.valueOf(0.18), BigDecimal.valueOf(18)),
                new BehavioralBaselineDto(19, BigDecimal.valueOf(28), BigDecimal.valueOf(-0.42), BigDecimal.valueOf(-42)),
                new BehavioralCounterfactualDto(true, true, 17, 6, 11, BigDecimal.valueOf(1820), BigDecimal.valueOf(710), BigDecimal.valueOf(-500), BigDecimal.valueOf(610), BigDecimal.valueOf(1110)),
                "insight-trust-v1", "post-loss-overtrading-v1", null));

        LeakResultDto roundTripped = objectMapper.readValue(objectMapper.writeValueAsString(List.of(result)), new TypeReference<List<LeakResultDto>>() {}).get(0);
        assertThat(roundTripped.getBehavioralFinding().evidenceStrength()).isEqualTo(EvidenceStrength.MODERATE);
        assertThat(roundTripped.getBehavioralFinding().counterfactual().supported()).isTrue();
        assertThat(roundTripped.getBehavioralFinding().engineVersion()).isEqualTo("insight-trust-v1");
    }
}
