package com.tradepilot;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.tradepilot.billing.PlanType;
import com.tradepilot.billing.SubscriptionStatus;
import com.tradepilot.entity.Trade;
import com.tradepilot.entity.TradingAccount;
import com.tradepilot.entity.User;
import com.tradepilot.enums.OutcomeType;
import com.tradepilot.enums.PropFirmType;
import com.tradepilot.enums.SessionType;
import com.tradepilot.enums.SetupType;
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
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.time.LocalDate;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class RbacSecurityTests {

    @Autowired MockMvc mockMvc;
    @Autowired ObjectMapper objectMapper;
    @Autowired UserRepository userRepository;
    @Autowired TradingAccountRepository tradingAccountRepository;
    @Autowired TradeRepository tradeRepository;
    @Autowired PasswordEncoder passwordEncoder;

    @Test
    void protectedEndpointsRejectMissingAndInvalidAuthentication() throws Exception {
        mockMvc.perform(get("/api/v1/accounts"))
                .andExpect(status().isUnauthorized());

        mockMvc.perform(get("/api/v1/accounts").header("Authorization", "Bearer definitely-not-a-jwt"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void adminRoutesRequireAdminForVersionedAndLegacyPrefixes() throws Exception {
        User admin = createUser("rbac-admin@example.com", UserRole.ADMIN);
        User normal = createUser("rbac-user@example.com", UserRole.USER);
        String adminToken = tokenFor(admin);
        String userToken = tokenFor(normal);

        mockMvc.perform(get("/api/v1/admin/users").header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isOk());
        mockMvc.perform(get("/api/v1/admin/users").header("Authorization", "Bearer " + userToken))
                .andExpect(status().isForbidden());
        mockMvc.perform(get("/api/admin/users").header("Authorization", "Bearer " + userToken))
                .andExpect(status().isForbidden());

        mockMvc.perform(get("/api/v1/admin/retention/summary").header("Authorization", "Bearer " + userToken))
                .andExpect(status().isForbidden());
        mockMvc.perform(get("/api/v1/admin/billing/summary").header("Authorization", "Bearer " + userToken))
                .andExpect(status().isForbidden());
        mockMvc.perform(get("/api/v1/admin/feature-usage/summary").header("Authorization", "Bearer " + userToken))
                .andExpect(status().isForbidden());
        mockMvc.perform(get("/api/v1/admin/contact-messages").header("Authorization", "Bearer " + userToken))
                .andExpect(status().isForbidden());
        mockMvc.perform(get("/api/v1/admin/observability/summary").header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isOk());
        mockMvc.perform(get("/api/v1/admin/observability/summary").header("Authorization", "Bearer " + userToken))
                .andExpect(status().isForbidden());
    }

    @Test
    void usersCannotReadMutateOrImportIntoAnotherUsersAccount() throws Exception {
        User userA = createUser("rbac-account-a@example.com", UserRole.USER);
        User userB = createUser("rbac-account-b@example.com", UserRole.USER);
        TradingAccount accountA = createAccount(userA, "A Account");
        TradingAccount accountB = createAccount(userB, "B Account");
        String tokenA = tokenFor(userA);

        String accountsJson = mockMvc.perform(get("/api/v1/accounts").header("Authorization", "Bearer " + tokenA))
                .andExpect(status().isOk())
                .andReturn()
                .getResponse()
                .getContentAsString();
        assertThat(accountsJson).contains("A Account").doesNotContain("B Account");

        mockMvc.perform(patch("/api/v1/accounts/{id}/risk", accountB.getId())
                        .header("Authorization", "Bearer " + tokenA)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"defaultRiskAmount\":200}"))
                .andExpect(status().isNotFound());

        mockMvc.perform(delete("/api/v1/accounts/{id}", accountB.getId())
                        .header("Authorization", "Bearer " + tokenA))
                .andExpect(status().isNotFound());

        mockMvc.perform(post("/api/v1/trades/import")
                        .header("Authorization", "Bearer " + tokenA)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(importBody(accountB.getId(), "cross-account-import")))
                .andExpect(status().isNotFound());

        mockMvc.perform(get("/api/v1/stats/summary")
                        .header("Authorization", "Bearer " + tokenA)
                        .param("accountId", accountB.getId().toString()))
                .andExpect(status().isNotFound());

        assertThat(tradingAccountRepository.findById(accountA.getId())).isPresent();
        assertThat(tradingAccountRepository.findById(accountB.getId())).isPresent();
    }

    @Test
    void usersCannotReadMutateDeleteOrReviewAnotherUsersTrade() throws Exception {
        User userA = createUser("rbac-trade-a@example.com", UserRole.USER);
        User userB = createUser("rbac-trade-b@example.com", UserRole.USER);
        TradingAccount accountA = createAccount(userA, "A Trade Account");
        TradingAccount accountB = createAccount(userB, "B Trade Account");
        Trade tradeA = createTrade(userA, accountA, BigDecimal.valueOf(125));
        Trade tradeB = createTrade(userB, accountB, BigDecimal.valueOf(-80));
        String tokenA = tokenFor(userA);

        mockMvc.perform(get("/api/v1/trades/{id}", tradeB.getId())
                        .header("Authorization", "Bearer " + tokenA))
                .andExpect(status().isNotFound());
        mockMvc.perform(get("/api/v1/trades/{id}/detail", tradeB.getId())
                        .header("Authorization", "Bearer " + tokenA))
                .andExpect(status().isNotFound());
        mockMvc.perform(put("/api/v1/trades/{id}", tradeB.getId())
                        .header("Authorization", "Bearer " + tokenA)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(tradeRequestBody("MES", "LONG", "2026-06-22", 50)))
                .andExpect(status().isNotFound());
        mockMvc.perform(patch("/api/v1/trades/{id}/journal", tradeB.getId())
                        .header("Authorization", "Bearer " + tokenA)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"setup\":\"IFVG\",\"confidence\":4,\"emotion\":\"CALM\",\"notes\":\"private\"}"))
                .andExpect(status().isNotFound());
        mockMvc.perform(delete("/api/v1/trades/{id}", tradeB.getId())
                        .header("Authorization", "Bearer " + tokenA))
                .andExpect(status().isNotFound());

        mockMvc.perform(post("/api/v1/ai/trade-review")
                        .header("Authorization", "Bearer " + tokenA)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(aiTradeReviewBody(tradeB.getId())))
                .andExpect(status().isNotFound());

        MockMultipartFile chart = new MockMultipartFile("file", "chart.png", "image/png", "not-a-real-image".getBytes());
        mockMvc.perform(multipart("/api/v1/ai/chart-review")
                        .file(chart)
                        .param("tradeId", tradeB.getId().toString())
                        .header("Authorization", "Bearer " + tokenA))
                .andExpect(status().isNotFound());

        assertThat(tradeRepository.findById(tradeA.getId())).isPresent();
        assertThat(tradeRepository.findById(tradeB.getId())).isPresent();
    }

    @Test
    void aiAndAnalyticsAccountScopedRoutesRejectAnotherUsersAccount() throws Exception {
        User userA = createUser("rbac-ai-a@example.com", UserRole.USER);
        User userB = createUser("rbac-ai-b@example.com", UserRole.USER);
        createAccount(userA, "AI A Account");
        TradingAccount accountB = createAccount(userB, "AI B Account");
        String tokenA = tokenFor(userA);

        mockMvc.perform(get("/api/v1/analytics/calendar-summary")
                        .header("Authorization", "Bearer " + tokenA)
                        .param("accountId", accountB.getId().toString()))
                .andExpect(status().isNotFound());
        mockMvc.perform(get("/api/v1/coach/daily")
                        .header("Authorization", "Bearer " + tokenA)
                        .param("accountId", accountB.getId().toString())
                        .param("date", "2026-06-22"))
                .andExpect(status().isNotFound());
        mockMvc.perform(get("/api/v1/coach/weekly")
                        .header("Authorization", "Bearer " + tokenA)
                        .param("accountId", accountB.getId().toString())
                        .param("weekStart", "2026-06-22"))
                .andExpect(status().isNotFound());
        mockMvc.perform(get("/api/v1/sessions/review")
                        .header("Authorization", "Bearer " + tokenA)
                        .param("accountId", accountB.getId().toString())
                        .param("date", "2026-06-22"))
                .andExpect(status().isNotFound());
        mockMvc.perform(get("/api/v1/ai/leak-detector")
                        .header("Authorization", "Bearer " + tokenA)
                        .param("accountId", accountB.getId().toString()))
                .andExpect(status().isNotFound());

        String dnaResponse = mockMvc.perform(get("/api/v1/ai/trading-dna")
                        .header("Authorization", "Bearer " + tokenA)
                        .param("accountId", accountB.getId().toString())
                        .param("days", "30"))
                .andExpect(status().isOk())
                .andReturn()
                .getResponse()
                .getContentAsString();
        JsonNode dnaJson = objectMapper.readTree(dnaResponse);
        assertThat(dnaJson.get("status").asText()).isEqualTo("INSUFFICIENT_DATA");
        assertThat(dnaJson.get("totalTrades").asInt()).isZero();
    }

    private User createUser(String email, UserRole role) {
        return userRepository.save(User.builder()
                .name(email)
                .email(email)
                .passwordHash(passwordEncoder.encode("password123"))
                .emailVerified(true)
                .role(role)
                .plan(PlanType.PRO)
                .subscriptionStatus(SubscriptionStatus.ACTIVE)
                .build());
    }

    private String tokenFor(User user) throws Exception {
        String response = mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"email":"%s","password":"password123"}
                                """.formatted(user.getEmail())))
                .andExpect(status().isOk())
                .andReturn()
                .getResponse()
                .getContentAsString();
        return objectMapper.readTree(response).get("token").asText();
    }

    private TradingAccount createAccount(User user, String name) {
        return tradingAccountRepository.save(TradingAccount.builder()
                .user(user)
                .name(name)
                .propFirm(PropFirmType.APEX)
                .accountNumber(name.replace(' ', '-'))
                .accountType(TradingAccountType.FUNDED)
                .startingBalance(BigDecimal.valueOf(50_000))
                .status(TradingAccountStatus.ACTIVE)
                .build());
    }

    private Trade createTrade(User user, TradingAccount account, BigDecimal pnl) {
        LocalDate date = LocalDate.of(2026, 6, 22);
        return tradeRepository.save(Trade.builder()
                .user(user)
                .account(account)
                .symbol("MESU6")
                .market("FUTURES")
                .tradeDate(date)
                .entryTime(date.atTime(9, 30))
                .exitTime(date.atTime(9, 45))
                .side("LONG")
                .setup(SetupType.ORB)
                .session(SessionType.NY_AM)
                .entryPrice(BigDecimal.valueOf(6000))
                .exitPrice(BigDecimal.valueOf(6002))
                .quantity(1)
                .pnlAmount(pnl)
                .outcome(pnl.signum() >= 0 ? OutcomeType.WIN : OutcomeType.LOSS)
                .build());
    }

    private String importBody(Long accountId, String externalKey) {
        return """
                [{
                  "accountId": %d,
                  "fileName": "import.csv",
                  "externalTradeKey": "%s",
                  "symbol": "MESU6",
                  "market": "FUTURES",
                  "tradeDate": "2026-06-22",
                  "entryTime": "2026-06-22T09:30:00",
                  "exitTime": "2026-06-22T09:45:00",
                  "side": "LONG",
                  "setup": "ORB",
                  "session": "NY_AM",
                  "entryPrice": 6000,
                  "exitPrice": 6002,
                  "quantity": 1,
                  "pnlAmount": 50,
                  "outcome": "WIN"
                }]
                """.formatted(accountId, externalKey);
    }

    private String tradeRequestBody(String symbol, String side, String date, int pnl) {
        return """
                {
                  "symbol": "%s",
                  "market": "FUTURES",
                  "tradeDate": "%s",
                  "entryTime": "%sT09:30:00",
                  "exitTime": "%sT09:45:00",
                  "side": "%s",
                  "setup": "ORB",
                  "session": "NY_AM",
                  "entryPrice": 6000,
                  "exitPrice": 6002,
                  "quantity": 1,
                  "pnlAmount": %d,
                  "outcome": "WIN"
                }
                """.formatted(symbol, date, date, date, side, pnl);
    }

    private String aiTradeReviewBody(Long tradeId) {
        return """
                {
                  "tradeId": %d,
                  "journalText": "Review this trade",
                  "trade": {
                    "id": %d,
                    "symbol": "MESU6",
                    "side": "LONG",
                    "pnl": -80,
                    "quantity": 1
                  },
                  "forceRefresh": false
                }
                """.formatted(tradeId, tradeId);
    }
}
