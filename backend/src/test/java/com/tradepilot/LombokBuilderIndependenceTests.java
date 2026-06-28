package com.tradepilot;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.tradepilot.billing.PlanType;
import com.tradepilot.coach.dto.CoachDeterministicSummary;
import com.tradepilot.dto.session.SessionReplayInsightsResponse;
import com.tradepilot.retention.dto.RetentionStatus;
import com.tradepilot.retention.dto.RetentionUserResponse;
import com.tradepilot.tradingdna.dto.TradingDnaContext;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Method;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class LombokBuilderIndependenceTests {

    private final ObjectMapper objectMapper = new ObjectMapper().findAndRegisterModules();

    @Test
    void affectedDtosDoNotExposeLombokBuilderMethods() {
        assertThat(hasBuilderMethod(CoachDeterministicSummary.class)).isFalse();
        assertThat(hasBuilderMethod(TradingDnaContext.class)).isFalse();
        assertThat(hasBuilderMethod(SessionReplayInsightsResponse.class)).isFalse();
        assertThat(hasBuilderMethod(RetentionUserResponse.class)).isFalse();
    }

    @Test
    void coachSummaryFactoryPreservesJsonContract() throws Exception {
        CoachDeterministicSummary summary = CoachDeterministicSummary.empty("Import trades first.");

        JsonNode json = objectMapper.valueToTree(summary);

        assertThat(summary.getTotalTrades()).isZero();
        assertThat(json.get("totalTrades").asInt()).isZero();
        assertThat(json.get("netPnl").decimalValue()).isEqualByComparingTo(BigDecimal.ZERO);
        assertThat(json.get("warnings").get(0).asText()).isEqualTo("Import trades first.");
    }

    @Test
    void tradingDnaContextFactoryPreservesJsonContract() {
        TradingDnaContext context = TradingDnaContext.of(
                1L,
                2L,
                false,
                30,
                LocalDate.of(2026, 6, 1),
                LocalDate.of(2026, 6, 30),
                12,
                BigDecimal.valueOf(250),
                BigDecimal.valueOf(58.3),
                "FVG",
                "SMT",
                "NY_AM",
                "PM_SESSION",
                Map.of("FOMO", 2L),
                Map.of("FVG", BigDecimal.valueOf(300)),
                Map.of("CALM", BigDecimal.valueOf(150)),
                Map.of("4", BigDecimal.valueOf(200)),
                Map.of("Rule break", 1L),
                List.of("FVG | CALM | Confidence 4"),
                List.of("Primary trading session detected"),
                List.of("Strong patience"),
                List.of("Clean execution"),
                List.of("Late entry"),
                List.of("Stay selective"),
                List.of("Avoid overtrading"),
                List.of(),
                78,
                82,
                74,
                80
        );

        assertThat(context.getUserId()).isEqualTo(1L);
        assertThat(context.getTotalTrades()).isEqualTo(12);
        assertThat(context.getSetupPnl()).containsEntry("FVG", BigDecimal.valueOf(300));
        assertThat(context.getDeterministicConsistencyScore()).isEqualTo(80);
    }

    @Test
    void sessionReplayInsightsFactoryPreservesJsonContract() throws Exception {
        SessionReplayInsightsResponse response = SessionReplayInsightsResponse.of(
                null,
                null,
                null,
                null,
                null,
                null,
                35,
                5,
                10,
                BigDecimal.valueOf(125),
                14,
                BigDecimal.valueOf(-80),
                null,
                null
        );

        JsonNode json = objectMapper.valueToTree(response);

        assertThat(response.getBestTradingHour()).isEqualTo(10);
        assertThat(json.get("bestTradingHour").asInt()).isEqualTo(10);
        assertThat(json.get("worstTradingHourPnl").decimalValue()).isEqualByComparingTo(BigDecimal.valueOf(-80));
    }

    @Test
    void retentionUserFactoryPreservesJsonContract() {
        RetentionUserResponse response = RetentionUserResponse.of(
                42L,
                "retention@example.com",
                "Retention User",
                PlanType.PRO,
                null,
                12,
                3,
                RetentionStatus.POWER_USER
        );

        JsonNode json = objectMapper.valueToTree(response);

        assertThat(response.getUserId()).isEqualTo(42L);
        assertThat(json.get("userId").asLong()).isEqualTo(42L);
        assertThat(json.get("email").asText()).isEqualTo("retention@example.com");
        assertThat(json.get("plan").asText()).isEqualTo("PRO");
        assertThat(json.get("activityCount30d").asLong()).isEqualTo(12);
        assertThat(json.get("status").asText()).isEqualTo("POWER_USER");
    }

    private boolean hasBuilderMethod(Class<?> type) {
        for (Method method : type.getDeclaredMethods()) {
            if ("builder".equals(method.getName())) {
                return true;
            }
        }
        return false;
    }
}
