package com.tradepilot;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.tradepilot.billing.PlanType;
import com.tradepilot.billing.SubscriptionStatus;
import com.tradepilot.config.ApiVersioningFilter;
import com.tradepilot.entity.TradingAccount;
import com.tradepilot.entity.User;
import com.tradepilot.enums.PropFirmType;
import com.tradepilot.enums.TradingAccountStatus;
import com.tradepilot.enums.TradingAccountType;
import com.tradepilot.enums.UserRole;
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
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class ApiVersioningTests {

    @Autowired MockMvc mockMvc;
    @Autowired ObjectMapper objectMapper;
    @Autowired UserRepository userRepository;
    @Autowired TradingAccountRepository tradingAccountRepository;
    @Autowired PasswordEncoder passwordEncoder;

    @Test
    void versionedAuthLoginAndMeWorkAndIncludeVersionHeader() throws Exception {
        User user = createUser("api-v1-auth@example.com", UserRole.USER);

        String token = login("/api/v1/auth/login", user.getEmail());

        mockMvc.perform(get("/api/v1/auth/me").header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(header().string(ApiVersioningFilter.VERSION_HEADER, ApiVersioningFilter.CURRENT_PRODUCT_VERSION));
    }

    @Test
    void legacyAuthLoginStillWorksWithDeprecationHeader() throws Exception {
        User user = createUser("api-legacy-auth@example.com", UserRole.USER);

        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(loginBody(user.getEmail())))
                .andExpect(status().isOk())
                .andExpect(header().string(ApiVersioningFilter.VERSION_HEADER, ApiVersioningFilter.CURRENT_PRODUCT_VERSION))
                .andExpect(header().string("Deprecation", "true"))
                .andExpect(header().string("Link", "</api/v1>; rel=\"successor-version\""));
    }

    @Test
    void versionedCoreAuthenticatedRoutesWork() throws Exception {
        User user = createUser("api-v1-core@example.com", UserRole.USER);
        TradingAccount account = createAccount(user);
        String token = login("/api/v1/auth/login", user.getEmail());

        mockMvc.perform(get("/api/v1/billing/usage").header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(header().string(ApiVersioningFilter.VERSION_HEADER, ApiVersioningFilter.CURRENT_PRODUCT_VERSION));

        mockMvc.perform(get("/api/v1/coach/daily")
                        .header("Authorization", "Bearer " + token)
                        .param("accountId", account.getId().toString())
                        .param("date", "2026-06-22"))
                .andExpect(status().isOk())
                .andExpect(header().string(ApiVersioningFilter.VERSION_HEADER, ApiVersioningFilter.CURRENT_PRODUCT_VERSION));

        mockMvc.perform(get("/api/v1/ai/trading-dna")
                        .header("Authorization", "Bearer " + token)
                        .param("days", "30")
                        .param("forceRefresh", "false")
                        .param("accountId", account.getId().toString())
                        .param("includeAllAccounts", "false"))
                .andExpect(status().isOk())
                .andExpect(header().string(ApiVersioningFilter.VERSION_HEADER, ApiVersioningFilter.CURRENT_PRODUCT_VERSION));
    }

    @Test
    void versionedAdminRoutesRequireAdminRole() throws Exception {
        User admin = createUser("api-v1-admin@example.com", UserRole.ADMIN);
        User normal = createUser("api-v1-normal@example.com", UserRole.USER);

        mockMvc.perform(get("/api/v1/admin/users").header("Authorization", "Bearer " + login("/api/v1/auth/login", admin.getEmail())))
                .andExpect(status().isOk())
                .andExpect(header().string(ApiVersioningFilter.VERSION_HEADER, ApiVersioningFilter.CURRENT_PRODUCT_VERSION));

        mockMvc.perform(get("/api/v1/admin/users").header("Authorization", "Bearer " + login("/api/v1/auth/login", normal.getEmail())))
                .andExpect(status().isForbidden())
                .andExpect(header().string(ApiVersioningFilter.VERSION_HEADER, ApiVersioningFilter.CURRENT_PRODUCT_VERSION));
    }

    @Test
    void stripeWebhookLegacyRouteRemainsStable() throws Exception {
        mockMvc.perform(post("/api/stripe/webhook").content("{}"))
                .andExpect(status().isBadRequest())
                .andExpect(header().string(ApiVersioningFilter.VERSION_HEADER, ApiVersioningFilter.CURRENT_PRODUCT_VERSION))
                .andExpect(header().doesNotExist("Deprecation"));
    }

    private String login(String path, String email) throws Exception {
        String response = mockMvc.perform(post(path)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(loginBody(email)))
                .andExpect(status().isOk())
                .andExpect(header().string(ApiVersioningFilter.VERSION_HEADER, ApiVersioningFilter.CURRENT_PRODUCT_VERSION))
                .andReturn()
                .getResponse()
                .getContentAsString();
        return objectMapper.readTree(response).get("token").asText();
    }

    private String loginBody(String email) {
        return """
                {
                  "email": "%s",
                  "password": "password123"
                }
                """.formatted(email);
    }

    private User createUser(String email, UserRole role) {
        return userRepository.save(User.builder()
                .name("API Version User")
                .email(email)
                .passwordHash(passwordEncoder.encode("password123"))
                .emailVerified(true)
                .role(role)
                .plan(PlanType.PRO)
                .subscriptionStatus(SubscriptionStatus.ACTIVE)
                .build());
    }

    private TradingAccount createAccount(User user) {
        return tradingAccountRepository.save(TradingAccount.builder()
                .user(user)
                .name("API Version Account")
                .propFirm(PropFirmType.TOPSTEP)
                .accountType(TradingAccountType.PERSONAL)
                .startingBalance(BigDecimal.valueOf(50_000))
                .status(TradingAccountStatus.ACTIVE)
                .build());
    }
}
