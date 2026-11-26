package com.tradepilot;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.tradepilot.billing.AiUsageEventRepository;
import com.tradepilot.billing.PlanType;
import com.tradepilot.billing.SubscriptionStatus;
import com.tradepilot.entity.Trade;
import com.tradepilot.entity.TradingAccount;
import com.tradepilot.entity.User;
import com.tradepilot.config.FeatureProperties;
import com.tradepilot.entitlement.FeatureEntitlementKey;
import com.tradepilot.entitlement.FeatureEntitlementService;
import com.tradepilot.enums.OutcomeType;
import com.tradepilot.enums.PropFirmType;
import com.tradepilot.enums.SessionType;
import com.tradepilot.enums.TradingAccountStatus;
import com.tradepilot.enums.TradingAccountType;
import com.tradepilot.enums.UserRole;
import com.tradepilot.repository.TradeRepository;
import com.tradepilot.repository.TradingAccountRepository;
import com.tradepilot.repository.UserRepository;
import com.tradepilot.survival.repository.NextSessionProtectionCommitmentRepository;
import com.tradepilot.survival.repository.NextSessionProtectionEvaluationRepository;
import com.tradepilot.survival.repository.NextSessionProtectionRuleResultRepository;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class AccountSurvivalEntitlementIntegrationTest {
    @Autowired MockMvc mockMvc;
    @Autowired ObjectMapper objectMapper;
    @Autowired UserRepository users;
    @Autowired TradingAccountRepository accounts;
    @Autowired TradeRepository trades;
    @Autowired NextSessionProtectionCommitmentRepository commitments;
    @Autowired NextSessionProtectionEvaluationRepository evaluations;
    @Autowired NextSessionProtectionRuleResultRepository ruleResults;
    @Autowired AiUsageEventRepository aiUsageEvents;
    @Autowired PasswordEncoder passwordEncoder;
    @Autowired FeatureProperties featureProperties;
    @Autowired FeatureEntitlementService featureEntitlements;

    @Test
    void freePreviewIsAvailableButPremiumMutationsRemainServerDeniedAndPrivacySafe() throws Exception {
        User free = user(PlanType.FREE, SubscriptionStatus.NONE, 100_000);
        User pro = user(PlanType.PRO, SubscriptionStatus.ACTIVE, 0);
        TradingAccount account = account(free);
        String freeToken = login(free);
        String proToken = login(pro);
        LocalDate date = LocalDate.of(2026, 12, 1);
        long commitmentCount = commitments.count();
        long aiUsageCount = aiUsageEvents.count();

        mockMvc.perform(get(base(account) + "/access").header("Authorization", bearer(freeToken)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.rolloutAvailable").value(true))
                .andExpect(jsonPath("$.premiumAccess").value(false))
                .andExpect(jsonPath("$.upgradeRequired").value(true));
        mockMvc.perform(put(base(account) + "/profile").header("Authorization", bearer(freeToken))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"currentBalance\":50000,\"failureThreshold\":48000,\"plannedRiskPerTrade\":100}"))
                .andExpect(status().isOk());
        mockMvc.perform(get(base(account)).param("date", date.toString()).header("Authorization", bearer(freeToken)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.turningPoint.crossAccountEvidence").doesNotExist());

        String payload = commitment(date, 2);
        mockMvc.perform(put(base(account) + "/commitment")
                        .header("Authorization", bearer(freeToken))
                        .header("X-Plan", "PRO").param("premiumAccess", "true")
                        .contentType(MediaType.APPLICATION_JSON).content(payload))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.errorCode").value("SUBSCRIPTION_REQUIRED"))
                .andExpect(jsonPath("$.feature").value("ACCOUNT_SURVIVAL"));
        assertThat(commitments.count()).isEqualTo(commitmentCount);
        assertThat(aiUsageEvents.count()).isEqualTo(aiUsageCount);
        assertThat(users.findById(free.getId()).orElseThrow().getPurchasedAiCreditsBalance()).isEqualTo(100_000);

        mockMvc.perform(put(base(account) + "/commitment").header("Authorization", bearer(proToken))
                        .contentType(MediaType.APPLICATION_JSON).content(payload))
                .andExpect(status().isNotFound());
        mockMvc.perform(get(base(account) + "/access").header("Authorization", bearer(proToken)))
                .andExpect(status().isNotFound());
        mockMvc.perform(get(base(account) + "/access")).andExpect(status().isUnauthorized());
    }

    @Test
    void downgradePreservesAcceptedConfigurationAndHistoryAndUpgradeResumesWithoutAiCredits() throws Exception {
        User user = user(PlanType.PRO, SubscriptionStatus.ACTIVE, 77);
        TradingAccount account = account(user);
        String token = login(user);
        LocalDate commitmentDate = LocalDate.of(2026, 12, 3);
        LocalDate evaluatedDate = commitmentDate.plusDays(1);

        mockMvc.perform(put(base(account) + "/commitment").header("Authorization", bearer(token))
                        .contentType(MediaType.APPLICATION_JSON).content(commitment(commitmentDate, 3)))
                .andExpect(status().isOk());
        mockMvc.perform(post(base(account) + "/commitment/" + commitmentDate + "/accept")
                        .header("Authorization", bearer(token)))
                .andExpect(status().isOk()).andExpect(jsonPath("$.status").value("ACCEPTED"));
        trades.save(trade(user, account, evaluatedDate));
        mockMvc.perform(post(base(account) + "/commitment/" + commitmentDate + "/evaluate")
                        .header("Authorization", bearer(token)))
                .andExpect(status().isOk()).andExpect(jsonPath("$.available").value(true));
        long evaluationCount = evaluations.count();
        long ruleCount = ruleResults.count();
        long aiCount = aiUsageEvents.count();

        user.setPlan(PlanType.FREE);
        user.setSubscriptionStatus(SubscriptionStatus.CANCELED);
        users.saveAndFlush(user);
        mockMvc.perform(get(base(account) + "/commitment").param("date", commitmentDate.toString())
                        .header("Authorization", bearer(token)))
                .andExpect(status().isOk()).andExpect(jsonPath("$.maxAttempts").value(3));
        mockMvc.perform(get(base(account) + "/commitment/" + commitmentDate + "/evaluation")
                        .header("Authorization", bearer(token)))
                .andExpect(status().isOk()).andExpect(jsonPath("$.available").value(true));
        mockMvc.perform(get(base(account) + "/commitment/" + commitmentDate + "/evaluations")
                        .header("Authorization", bearer(token)))
                .andExpect(status().isOk()).andExpect(jsonPath("$.evaluations.length()").value(1));
        mockMvc.perform(post(base(account) + "/commitment/" + commitmentDate + "/evaluate")
                        .header("Authorization", bearer(token)))
                .andExpect(status().isForbidden()).andExpect(jsonPath("$.errorCode").value("SUBSCRIPTION_REQUIRED"));
        assertThat(evaluations.count()).isEqualTo(evaluationCount);
        assertThat(ruleResults.count()).isEqualTo(ruleCount);
        assertThat(commitments.findByAccount_IdAndUserAndEffectiveSessionDate(account.getId(), user, commitmentDate)
                .orElseThrow().getMaxAttempts()).isEqualTo(3);

        user.setPlan(PlanType.PRO);
        user.setSubscriptionStatus(SubscriptionStatus.PAST_DUE);
        users.saveAndFlush(user);
        mockMvc.perform(post(base(account) + "/commitment/" + commitmentDate + "/evaluate")
                        .header("Authorization", bearer(token)))
                .andExpect(status().isOk()).andExpect(jsonPath("$.available").value(true));
        assertThat(evaluations.count()).isEqualTo(evaluationCount);
        assertThat(ruleResults.count()).isEqualTo(ruleCount);
        assertThat(aiUsageEvents.count()).isEqualTo(aiCount);
        assertThat(users.findById(user.getId()).orElseThrow().getPurchasedAiCreditsBalance()).isEqualTo(77);
    }

    @Test
    void rolloutModesAreAuthoritativeFailClosedAndNeverBypassOwnership() throws Exception {
        User pro = user(PlanType.PRO, SubscriptionStatus.ACTIVE, 0);
        User otherPro = user(PlanType.PRO, SubscriptionStatus.ACTIVE, 0);
        TradingAccount account = account(pro);
        String token = login(pro);
        String otherToken = login(otherPro);
        LocalDate date = LocalDate.of(2026, 12, 8);
        String originalMode = featureProperties.getAccountSurvivalRolloutMode();
        try {
            featureProperties.setAccountSurvivalRolloutMode("disabled");
            mockMvc.perform(get(base(account) + "/access").header("Authorization", bearer(token)))
                    .andExpect(status().isOk()).andExpect(jsonPath("$.rolloutAvailable").value(false));
            mockMvc.perform(put(base(account) + "/commitment").header("Authorization", bearer(token))
                            .contentType(MediaType.APPLICATION_JSON).content(commitment(date, 2)))
                    .andExpect(status().isNotFound()).andExpect(jsonPath("$.errorCode").value("FEATURE_DISABLED"));

            featureProperties.setAccountSurvivalRolloutMode("invalid-enable-value");
            mockMvc.perform(put(base(account) + "/commitment").header("Authorization", bearer(token))
                            .contentType(MediaType.APPLICATION_JSON).content(commitment(date, 2)))
                    .andExpect(status().isNotFound()).andExpect(jsonPath("$.errorCode").value("FEATURE_DISABLED"));

            featureProperties.setAccountSurvivalRolloutMode("beta");
            mockMvc.perform(get(base(account) + "/access").header("Authorization", bearer(token)))
                    .andExpect(status().isOk()).andExpect(jsonPath("$.rolloutAvailable").value(false));
            featureEntitlements.setEntitlement(pro.getId(), FeatureEntitlementKey.ACCOUNT_SURVIVAL_BETA, true, "A5 integration test");
            mockMvc.perform(get(base(account) + "/access").header("Authorization", bearer(token)))
                    .andExpect(status().isOk()).andExpect(jsonPath("$.premiumAccess").value(true));
            mockMvc.perform(put(base(account) + "/commitment").header("Authorization", bearer(token))
                            .contentType(MediaType.APPLICATION_JSON).content(commitment(date, 2)))
                    .andExpect(status().isOk());
            mockMvc.perform(get(base(account) + "/access").header("Authorization", bearer(otherToken)))
                    .andExpect(status().isNotFound());

            featureProperties.setAccountSurvivalRolloutMode("general-availability");
            mockMvc.perform(get(base(account) + "/access").header("Authorization", bearer(token)))
                    .andExpect(status().isOk()).andExpect(jsonPath("$.premiumAccess").value(true));
        } finally {
            featureProperties.setAccountSurvivalRolloutMode(originalMode);
        }
    }

    @Test
    void normalizedSubscriptionPlansKeepTrialAndGraceAccessButDenyCancelledAndExpiredUsers() throws Exception {
        User user = user(PlanType.PRO, SubscriptionStatus.TRIALING, 0);
        TradingAccount account = account(user);
        String token = login(user);

        assertPremiumAccess(account, token, true);
        user.setSubscriptionStatus(SubscriptionStatus.PAST_DUE);
        users.saveAndFlush(user);
        assertPremiumAccess(account, token, true);
        user.setPlan(PlanType.FREE);
        user.setSubscriptionStatus(SubscriptionStatus.CANCELED);
        users.saveAndFlush(user);
        assertPremiumAccess(account, token, false);
        user.setSubscriptionStatus(SubscriptionStatus.NONE);
        users.saveAndFlush(user);
        assertPremiumAccess(account, token, false);
    }

    private void assertPremiumAccess(TradingAccount account, String token, boolean expected) throws Exception {
        mockMvc.perform(get(base(account) + "/access").header("Authorization", bearer(token)))
                .andExpect(status().isOk()).andExpect(jsonPath("$.premiumAccess").value(expected));
    }

    private User user(PlanType plan, SubscriptionStatus status, int credits) {
        String id = UUID.randomUUID().toString();
        return users.save(User.builder().name("Survival entitlement user").email(id + "@example.com")
                .passwordHash(passwordEncoder.encode("password123")).emailVerified(true).role(UserRole.USER)
                .plan(plan).subscriptionStatus(status).purchasedAiCreditsBalance(credits).build());
    }

    private TradingAccount account(User user) {
        return accounts.save(TradingAccount.builder().user(user).name("Entitlement account " + user.getId())
                .propFirm(PropFirmType.TOPSTEP).accountType(TradingAccountType.PERSONAL)
                .startingBalance(new BigDecimal("50000")).status(TradingAccountStatus.ACTIVE).build());
    }

    private Trade trade(User user, TradingAccount account, LocalDate date) {
        return Trade.builder().user(user).account(account).symbol("MNQ").market("FUTURES").tradeDate(date)
                .entryTime(LocalDateTime.of(date, java.time.LocalTime.of(10, 0)))
                .exitTime(LocalDateTime.of(date, java.time.LocalTime.of(10, 10)))
                .side("LONG").session(SessionType.NY_AM).quantity(1).pnlAmount(new BigDecimal("-50"))
                .outcome(OutcomeType.LOSS).externalTradeKey(UUID.randomUUID().toString()).build();
    }

    private String login(User user) throws Exception {
        String response = mockMvc.perform(post("/api/auth/login").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\":\"%s\",\"password\":\"password123\"}".formatted(user.getEmail())))
                .andExpect(status().isOk()).andReturn().getResponse().getContentAsString();
        return objectMapper.readTree(response).get("token").asText();
    }

    private String commitment(LocalDate date, int attempts) {
        return "{\"effectiveSessionDate\":\"%s\",\"maxAttempts\":%d,\"maxNormalizedExposure\":20,\"maxSessionLoss\":200,\"postLossPauseMinutes\":20}".formatted(date, attempts);
    }

    private String base(TradingAccount account) { return "/api/accounts/" + account.getId() + "/survival"; }
    private String bearer(String token) { return "Bearer " + token; }
}
