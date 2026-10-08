package com.tradepilot;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.tradepilot.billing.PlanType;
import com.tradepilot.billing.SubscriptionStatus;
import com.tradepilot.entity.Trade;
import com.tradepilot.entity.TradingAccount;
import com.tradepilot.entity.User;
import com.tradepilot.entitlement.FeatureEntitlementKey;
import com.tradepilot.entitlement.FeatureEntitlementService;
import com.tradepilot.enums.OutcomeType;
import com.tradepilot.enums.PropFirmType;
import com.tradepilot.enums.SessionType;
import com.tradepilot.enums.SetupType;
import com.tradepilot.enums.TradingAccountStatus;
import com.tradepilot.enums.TradingAccountType;
import com.tradepilot.enums.UserRole;
import com.tradepilot.playbook.entity.PlaybookSetup;
import com.tradepilot.playbook.entity.TradePlaybookAssignment;
import com.tradepilot.playbook.enums.PlaybookCategory;
import com.tradepilot.playbook.enums.PlaybookDirection;
import com.tradepilot.playbook.enums.PlaybookStatus;
import com.tradepilot.playbook.repository.PlaybookSetupRepository;
import com.tradepilot.playbook.repository.TradePlaybookAssignmentRepository;
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
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
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
    @Autowired PlaybookSetupRepository playbookSetupRepository;
    @Autowired TradePlaybookAssignmentRepository playbookAssignmentRepository;
    @Autowired PasswordEncoder passwordEncoder;
    @Autowired FeatureEntitlementService entitlementService;

    @Test
    void protectedEndpointsRejectMissingAndInvalidAuthentication() throws Exception {
        mockMvc.perform(get("/api/v1/accounts"))
                .andExpect(status().isUnauthorized());

        mockMvc.perform(get("/api/v1/accounts").header("Authorization", "Bearer definitely-not-a-jwt"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void privateApiResponsesAreNotSharedCacheableAndIncludeBaselineSecurityHeaders() throws Exception {
        mockMvc.perform(get("/api/v1/accounts"))
                .andExpect(status().isUnauthorized())
                .andExpect(header().string("Cache-Control", "no-cache, no-store, max-age=0, must-revalidate"))
                .andExpect(header().string("X-Content-Type-Options", "nosniff"))
                .andExpect(header().string("Referrer-Policy", "strict-origin-when-cross-origin"))
                .andExpect(header().string("Permissions-Policy", "camera=(), microphone=(), geolocation=(), payment=()"));
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
        mockMvc.perform(get("/api/v1/admin/market-data/coverage").header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isOk());
        mockMvc.perform(get("/api/v1/admin/market-data/coverage").header("Authorization", "Bearer " + userToken))
                .andExpect(status().isForbidden());
        mockMvc.perform(get("/api/v1/admin/data-trust/quality").header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.importTrust").exists())
                .andExpect(jsonPath("$.importTrust.reviewed").exists())
                .andExpect(jsonPath("$.importTrust.matched").exists())
                .andExpect(jsonPath("$.importTrust.mismatched").exists())
                .andExpect(jsonPath("$.importTrust.notReviewed").exists())
                .andExpect(jsonPath("$.insightTrust").exists())
                .andExpect(jsonPath("$.insightTrust.rated").exists())
                .andExpect(jsonPath("$.insightTrust.positive").exists())
                .andExpect(jsonPath("$.insightTrust.negative").exists());
        mockMvc.perform(get("/api/v1/admin/data-trust/quality").header("Authorization", "Bearer " + userToken))
                .andExpect(status().isForbidden());
        mockMvc.perform(get("/api/v1/admin/data-trust/quality"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void backtestRoutesRequirePrivateEntitlementRatherThanAdminRole() throws Exception {
        User founder = createUser("rbac-backtest-founder@example.com", UserRole.USER);
        User admin = createUser("rbac-backtest-admin@example.com", UserRole.ADMIN);
        User normal = createUser("rbac-backtest-user@example.com", UserRole.USER);
        entitlementService.setEntitlement(founder.getId(), FeatureEntitlementKey.PLAYBOOK_BACKTEST_BETA, true, "test founder access");
        String founderToken = tokenFor(founder);
        String adminToken = tokenFor(admin);
        String userToken = tokenFor(normal);

        mockMvc.perform(get("/api/v1/backtests/access").header("Authorization", "Bearer " + founderToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.allowed").value(true));
        mockMvc.perform(get("/api/v1/backtests/access").header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.allowed").value(false));

        mockMvc.perform(get("/api/v1/backtests").header("Authorization", "Bearer " + founderToken))
                .andExpect(status().isOk());
        for (String token : java.util.List.of(adminToken, userToken)) {
            mockMvc.perform(post("/api/v1/backtests")
                            .header("Authorization", "Bearer " + token)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(com.tradepilot.backtest.model.BacktestConfig.conservative(
                                    com.tradepilot.marketdata.model.MarketInstrument.NQ,
                                    LocalDate.of(2026, 9, 7), LocalDate.of(2026, 9, 10)))))
                    .andExpect(status().isForbidden());
            mockMvc.perform(get("/api/v1/backtests").header("Authorization", "Bearer " + token))
                    .andExpect(status().isForbidden());
            mockMvc.perform(get("/api/v1/backtests/999").header("Authorization", "Bearer " + token))
                    .andExpect(status().isForbidden());
            mockMvc.perform(get("/api/v1/backtests/999/trades").header("Authorization", "Bearer " + token))
                    .andExpect(status().isForbidden());
            mockMvc.perform(get("/api/v1/backtests/999/trades/999/chart").header("Authorization", "Bearer " + token))
                    .andExpect(status().isForbidden());
        }

        mockMvc.perform(get("/api/v1/backtests/999").header("Authorization", "Bearer " + founderToken))
                .andExpect(status().isNotFound());
        mockMvc.perform(get("/api/v1/backtests/999/trades").header("Authorization", "Bearer " + founderToken))
                .andExpect(status().isNotFound());
        mockMvc.perform(get("/api/v1/backtests/999/trades/999/chart").header("Authorization", "Bearer " + founderToken))
                .andExpect(status().isNotFound());
    }

    @Test
    void authMeRequiresAuthenticationAndAcceptsUserAndAdminTokens() throws Exception {
        User admin = createUser("rbac-auth-me-admin@example.com", UserRole.ADMIN);
        User normal = createUser("rbac-auth-me-user@example.com", UserRole.USER);

        mockMvc.perform(get("/api/auth/me"))
                .andExpect(status().isUnauthorized());
        mockMvc.perform(get("/api/auth/me").header("Authorization", "Bearer definitely-not-a-jwt"))
                .andExpect(status().isUnauthorized());
        mockMvc.perform(get("/api/auth/me").header("Authorization", "Bearer " + tokenFor(normal)))
                .andExpect(status().isOk());
        mockMvc.perform(get("/api/v1/auth/me").header("Authorization", "Bearer " + tokenFor(admin)))
                .andExpect(status().isOk());
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

    @Test
    void playbookRoutesRejectAnotherUsersSetupTradeAndAssignmentIds() throws Exception {
        User userA = createUser("rbac-playbook-a@example.com", UserRole.USER);
        User userB = createUser("rbac-playbook-b@example.com", UserRole.USER);
        TradingAccount accountA = createAccount(userA, "Playbook A Account");
        TradingAccount accountB = createAccount(userB, "Playbook B Account");
        Trade tradeA = createTrade(userA, accountA, BigDecimal.valueOf(95));
        Trade tradeB = createTrade(userB, accountB, BigDecimal.valueOf(120));
        PlaybookSetup setupA = createSetup(userA, accountA, "A Setup");
        PlaybookSetup setupB = createSetup(userB, accountB, "B Setup");
        TradePlaybookAssignment assignmentB = playbookAssignmentRepository.save(assignment(userB, accountB, tradeB, setupB));
        String tokenA = tokenFor(userA);

        mockMvc.perform(get("/api/v1/playbook/{setupId}", setupB.getId())
                        .header("Authorization", "Bearer " + tokenA))
                .andExpect(status().isNotFound());
        mockMvc.perform(get("/api/v1/playbook/{setupId}", 9_999_999L)
                        .header("Authorization", "Bearer " + tokenA))
                .andExpect(status().isNotFound());

        mockMvc.perform(put("/api/v1/playbook/{setupId}", setupB.getId())
                        .header("Authorization", "Bearer " + tokenA)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(playbookRequestBody(accountA.getId(), "Hijack")))
                .andExpect(status().isNotFound());

        mockMvc.perform(post("/api/v1/playbook/{setupId}/trades/{tradeId}", setupA.getId(), tradeB.getId())
                        .header("Authorization", "Bearer " + tokenA)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isNotFound());

        mockMvc.perform(patch("/api/v1/playbook/assignments/{assignmentId}", assignmentB.getId())
                        .header("Authorization", "Bearer " + tokenA)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"notes\":\"must not update\"}"))
                .andExpect(status().isNotFound());
        mockMvc.perform(patch("/api/v1/playbook/assignments/{assignmentId}", 9_999_999L)
                        .header("Authorization", "Bearer " + tokenA)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"notes\":\"must not update\"}"))
                .andExpect(status().isNotFound());

        assertThat(playbookSetupRepository.findByIdAndUser_Id(setupB.getId(), userB.getId())).isPresent();
        assertThat(playbookAssignmentRepository.findByIdAndUser_Id(assignmentB.getId(), userB.getId()))
                .get()
                .extracting(TradePlaybookAssignment::getNotes)
                .isNull();
        assertThat(tradeRepository.findById(tradeA.getId())).isPresent();
    }

    @Test
    void replayAndTradingMemoryRoutesRejectAnotherUsersTradeIds() throws Exception {
        User userA = createUser("rbac-replay-a@example.com", UserRole.USER);
        User userB = createUser("rbac-replay-b@example.com", UserRole.USER);
        TradingAccount accountB = createAccount(userB, "Replay B Account");
        Trade tradeB = createTrade(userB, accountB, BigDecimal.valueOf(-40));
        String tokenA = tokenFor(userA);

        mockMvc.perform(get("/api/v1/trades/{tradeId}/replay", tradeB.getId())
                        .header("Authorization", "Bearer " + tokenA))
                .andExpect(status().isNotFound());

        mockMvc.perform(post("/api/v1/trades/{tradeId}/replay/generate", tradeB.getId())
                        .header("Authorization", "Bearer " + tokenA))
                .andExpect(status().isNotFound());

        mockMvc.perform(get("/api/v1/trades/{tradeId}/trading-memory", tradeB.getId())
                        .header("Authorization", "Bearer " + tokenA))
                .andExpect(status().isNotFound());
        mockMvc.perform(get("/api/v1/trades/{tradeId}/trading-memory", 9_999_999L)
                        .header("Authorization", "Bearer " + tokenA))
                .andExpect(status().isNotFound());

        mockMvc.perform(post("/api/v1/trades/{tradeId}/trading-memory/regenerate", tradeB.getId())
                        .header("Authorization", "Bearer " + tokenA))
                .andExpect(status().isNotFound());
        mockMvc.perform(get("/api/v1/trades/{tradeId}/similar-trades", tradeB.getId())
                        .header("Authorization", "Bearer " + tokenA))
                .andExpect(status().isNotFound());

        assertThat(tradeRepository.findById(tradeB.getId())).isPresent();
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

    private PlaybookSetup createSetup(User user, TradingAccount account, String name) {
        PlaybookSetup setup = new PlaybookSetup();
        setup.setUser(user);
        setup.setAccount(account);
        setup.setName(name);
        setup.setShortDescription("Authorization test setup");
        setup.setDescription("Authorization test setup");
        setup.setCategory(PlaybookCategory.CUSTOM);
        setup.setStatus(PlaybookStatus.DRAFT);
        setup.setDirection(PlaybookDirection.BOTH);
        return playbookSetupRepository.save(setup);
    }

    private TradePlaybookAssignment assignment(User user, TradingAccount account, Trade trade, PlaybookSetup setup) {
        TradePlaybookAssignment assignment = new TradePlaybookAssignment();
        assignment.setUser(user);
        assignment.setAccount(account);
        assignment.setTrade(trade);
        assignment.setSetup(setup);
        return assignment;
    }

    private String playbookRequestBody(Long accountId, String name) {
        return """
                {
                  "accountId": %d,
                  "name": "%s",
                  "shortDescription": "Updated",
                  "description": "Updated by authorization test",
                  "category": "CUSTOM",
                  "status": "DRAFT",
                  "direction": "BOTH",
                  "instruments": [],
                  "sessions": [],
                  "timeframes": [],
                  "tags": [],
                  "marketContext": [],
                  "riskRules": [],
                  "managementRules": [],
                  "postTradeRules": [],
                  "checklistItems": []
                }
                """.formatted(accountId, name);
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
