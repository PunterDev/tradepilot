package com.tradepilot;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.tradepilot.billing.AiCreditUsageRepository;
import com.tradepilot.billing.Feature;
import com.tradepilot.billing.PlanType;
import com.tradepilot.billing.SubscriptionStatus;
import com.tradepilot.entity.Trade;
import com.tradepilot.entity.TradingAccount;
import com.tradepilot.entity.User;
import com.tradepilot.enums.OutcomeType;
import com.tradepilot.enums.PropFirmType;
import com.tradepilot.enums.SessionType;
import com.tradepilot.enums.SetupType;
import com.tradepilot.enums.Side;
import com.tradepilot.enums.TradingAccountStatus;
import com.tradepilot.enums.TradingAccountType;
import com.tradepilot.enums.UserRole;
import com.tradepilot.repository.TradeRepository;
import com.tradepilot.repository.TradingAccountRepository;
import com.tradepilot.repository.UserRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class PerformanceTimelineEndpointTests {
    @Autowired MockMvc mockMvc;
    @Autowired ObjectMapper objectMapper;
    @Autowired UserRepository userRepository;
    @Autowired TradingAccountRepository tradingAccountRepository;
    @Autowired TradeRepository tradeRepository;
    @Autowired AiCreditUsageRepository creditUsageRepository;
    @Autowired PasswordEncoder passwordEncoder;

    @Test
    void legacyAndV1GetRoutesReturnTimelineWithoutConsumingCredits() throws Exception {
        User user = createUser("timeline-get-routes@example.com");
        TradingAccount account = createAccount(user);
        createEligibleMonth(user, account, "timeline-get");
        String token = tokenFor(user);
        long before = performanceTimelineCredits(user);

        String legacy = getTimeline(token, "/api/ai/performance-timeline", account.getId());
        String v1 = getTimeline(token, "/api/v1/ai/performance-timeline", account.getId());

        JsonNode legacyJson = objectMapper.readTree(legacy);
        JsonNode v1Json = objectMapper.readTree(v1);
        assertThat(legacyJson.get("accountId").asLong()).isEqualTo(account.getId());
        assertThat(v1Json.get("accountId").asLong()).isEqualTo(account.getId());
        assertThat(legacyJson.get("creditCost").asInt()).isZero();
        assertThat(v1Json.get("creditCost").asInt()).isZero();
        assertThat(performanceTimelineCredits(user)).isEqualTo(before);
    }

    @Test
    void regenerateConsumesExactlyThreePerformanceTimelineCredits() throws Exception {
        User user = createUser("timeline-regenerate-credits@example.com");
        TradingAccount account = createAccount(user);
        createEligibleMonth(user, account, "timeline-regenerate");
        String token = tokenFor(user);
        long before = performanceTimelineCredits(user);

        String response = mockMvc.perform(post("/api/v1/ai/performance-timeline/regenerate")
                        .param("accountId", account.getId().toString())
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andReturn()
                .getResponse()
                .getContentAsString();

        JsonNode json = objectMapper.readTree(response);
        assertThat(json.get("creditCost").asInt()).isEqualTo(3);
        assertThat(performanceTimelineCredits(user)).isEqualTo(before + 3);
    }

    private String getTimeline(String token, String path, Long accountId) throws Exception {
        return mockMvc.perform(get(path)
                        .param("accountId", accountId.toString())
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andReturn()
                .getResponse()
                .getContentAsString();
    }

    private User createUser(String email) {
        return userRepository.save(User.builder()
                .name("Timeline Endpoint User")
                .email(email)
                .passwordHash(passwordEncoder.encode("password123"))
                .emailVerified(true)
                .role(UserRole.USER)
                .plan(PlanType.PRO)
                .subscriptionStatus(SubscriptionStatus.ACTIVE)
                .build());
    }

    private TradingAccount createAccount(User user) {
        return tradingAccountRepository.save(TradingAccount.builder()
                .user(user)
                .name("Journey Account")
                .propFirm(PropFirmType.TOPSTEP)
                .accountType(TradingAccountType.PERSONAL)
                .startingBalance(BigDecimal.valueOf(50_000))
                .defaultRiskAmount(BigDecimal.valueOf(100))
                .status(TradingAccountStatus.ACTIVE)
                .build());
    }

    private void createEligibleMonth(User user, TradingAccount account, String keyPrefix) {
        LocalDate date = LocalDate.of(2026, 6, 1);
        for (int i = 0; i < 6; i++) {
            BigDecimal pnl = i % 2 == 0 ? BigDecimal.valueOf(150) : BigDecimal.valueOf(-50);
            tradeRepository.save(Trade.builder()
                    .user(user)
                    .account(account)
                    .symbol("MESU6")
                    .market("FUTURES")
                    .tradeDate(date.plusDays(i))
                    .entryTime(LocalDateTime.of(date.plusDays(i), java.time.LocalTime.of(9, 30)))
                    .exitTime(LocalDateTime.of(date.plusDays(i), java.time.LocalTime.of(9, 45)))
                    .side(i % 2 == 0 ? Side.LONG.name() : Side.SHORT.name())
                    .setup(SetupType.ORB)
                    .session(SessionType.NY_AM)
                    .entryPrice(BigDecimal.valueOf(5300))
                    .exitPrice(BigDecimal.valueOf(5310))
                    .quantity(1)
                    .riskAmount(BigDecimal.valueOf(100))
                    .pnlAmount(pnl)
                    .pnlR(pnl.divide(BigDecimal.valueOf(100), 2, java.math.RoundingMode.HALF_UP))
                    .outcome(pnl.signum() >= 0 ? OutcomeType.WIN : OutcomeType.LOSS)
                    .followedPlan(i != 1)
                    .externalTradeKey(keyPrefix + "-" + i)
                    .build());
        }
    }

    private long performanceTimelineCredits(User user) {
        return creditUsageRepository.findAll().stream()
                .filter(usage -> user.getId().equals(usage.getUserId()))
                .filter(usage -> usage.getFeature() == Feature.PERFORMANCE_TIMELINE)
                .mapToLong(usage -> usage.getCredits() == null ? 0 : usage.getCredits())
                .sum();
    }

    private String tokenFor(User user) throws Exception {
        String body = """
                {
                  "email": "%s",
                  "password": "password123"
                }
                """.formatted(user.getEmail());

        String response = mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isOk())
                .andReturn()
                .getResponse()
                .getContentAsString();

        return objectMapper.readTree(response).get("token").asText();
    }
}
