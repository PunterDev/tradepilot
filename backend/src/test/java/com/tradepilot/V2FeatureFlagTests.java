package com.tradepilot;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.tradepilot.billing.PlanType;
import com.tradepilot.billing.SubscriptionStatus;
import com.tradepilot.entity.TradingAccount;
import com.tradepilot.entity.User;
import com.tradepilot.enums.PropFirmType;
import com.tradepilot.enums.TradingAccountStatus;
import com.tradepilot.enums.TradingAccountType;
import com.tradepilot.enums.UserRole;
import com.tradepilot.featureusage.FeatureUsageRepository;
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

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest(properties = "tradepilot.features.v2-enabled=false")
@AutoConfigureMockMvc
@ActiveProfiles("test")
class V2FeatureFlagTests {
    @Autowired MockMvc mockMvc;
    @Autowired ObjectMapper objectMapper;
    @Autowired UserRepository userRepository;
    @Autowired TradingAccountRepository tradingAccountRepository;
    @Autowired FeatureUsageRepository featureUsageRepository;
    @Autowired PasswordEncoder passwordEncoder;

    @Test
    void publicFeatureAvailabilityReportsV2DisabledAndCoachEnabled() throws Exception {
        String response = mockMvc.perform(get("/api/v1/features"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.v2FeaturesEnabled").value(false))
                .andExpect(jsonPath("$.features.playbook").value(false))
                .andExpect(jsonPath("$.features.tradingDna").value(false))
                .andExpect(jsonPath("$.features.journey").value(false))
                .andExpect(jsonPath("$.features.coach").value(true))
                .andReturn()
                .getResponse()
                .getContentAsString();

        JsonNode json = objectMapper.readTree(response);
        assertThat(json.get("features").get("coach").asBoolean()).isTrue();
    }

    @Test
    void disabledV2EndpointsRejectBeforeFeatureUsageTracking() throws Exception {
        User user = createUser("v2-disabled@example.com");
        TradingAccount account = createAccount(user);
        String token = login(user.getEmail());
        long usageBefore = featureUsageRepository.count();

        expectFeatureDisabled("/api/ai/trading-dna?accountId=" + account.getId(), token);
        expectFeatureDisabled("/api/v1/ai/performance-timeline?accountId=" + account.getId(), token);
        expectFeatureDisabled("/api/playbook", token);
        mockMvc.perform(post("/api/v1/ai/playbook/draft-from-description")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"description\":\"Opening range breakout\"}"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.errorCode").value("FEATURE_DISABLED"));

        assertThat(featureUsageRepository.count()).isEqualTo(usageBefore);
    }

    @Test
    void coachRemainsAvailableWhenV2IsDisabled() throws Exception {
        User user = createUser("v2-disabled-coach@example.com");
        TradingAccount account = createAccount(user);
        String token = login(user.getEmail());

        mockMvc.perform(get("/api/coach/daily")
                        .header("Authorization", "Bearer " + token)
                        .param("accountId", account.getId().toString())
                        .param("date", "2026-06-22"))
                .andExpect(status().isOk());
    }

    private void expectFeatureDisabled(String path, String token) throws Exception {
        mockMvc.perform(get(path).header("Authorization", "Bearer " + token))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.errorCode").value("FEATURE_DISABLED"));
    }

    private String login(String email) throws Exception {
        String response = mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "email": "%s",
                                  "password": "password123"
                                }
                                """.formatted(email)))
                .andExpect(status().isOk())
                .andReturn()
                .getResponse()
                .getContentAsString();
        return objectMapper.readTree(response).get("token").asText();
    }

    private User createUser(String email) {
        return userRepository.save(User.builder()
                .name("V2 Flag User")
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
                .name("V2 Flag Account")
                .propFirm(PropFirmType.TOPSTEP)
                .accountType(TradingAccountType.PERSONAL)
                .startingBalance(BigDecimal.valueOf(50_000))
                .status(TradingAccountStatus.ACTIVE)
                .build());
    }
}
