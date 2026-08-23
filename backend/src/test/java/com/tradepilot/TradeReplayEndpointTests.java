package com.tradepilot;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.tradepilot.billing.PlanType;
import com.tradepilot.billing.SubscriptionStatus;
import com.tradepilot.entity.Trade;
import com.tradepilot.entity.TradingAccount;
import com.tradepilot.entity.User;
import com.tradepilot.enums.*;
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

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest(properties = {
        "tradepilot.features.v3-enabled=true",
        "tradepilot.market-data.provider=disabled"
})
@AutoConfigureMockMvc
@ActiveProfiles("test")
class TradeReplayEndpointTests {
    @Autowired MockMvc mockMvc;
    @Autowired ObjectMapper objectMapper;
    @Autowired UserRepository userRepository;
    @Autowired TradingAccountRepository accountRepository;
    @Autowired TradeRepository tradeRepository;
    @Autowired PasswordEncoder passwordEncoder;

    @Test
    void providerDisabledReturnsClearStateWithoutFailingApp() throws Exception {
        User user = createUser("replay-provider-disabled@example.com");
        TradingAccount account = createAccount(user);
        Trade trade = createTrade(user, account, "ESU6", LocalDateTime.now().minusDays(3));
        String token = login(user.getEmail());

        mockMvc.perform(post("/api/v1/trades/" + trade.getId() + "/replay/generate")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("PROVIDER_NOT_CONFIGURED"))
                .andExpect(jsonPath("$.failureCode").value("PROVIDER_NOT_CONFIGURED"));
    }

    @Test
    void recentTradeWaitsForMarketDataWithoutProviderCall() throws Exception {
        User user = createUser("replay-waiting@example.com");
        TradingAccount account = createAccount(user);
        Trade trade = createTrade(user, account, "MNQU6", LocalDateTime.now().minusHours(2));
        String token = login(user.getEmail());

        mockMvc.perform(post("/api/v1/trades/" + trade.getId() + "/replay/generate")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("WAITING_FOR_MARKET_DATA"))
                .andExpect(jsonPath("$.eligibleAt").exists());
    }

    @Test
    void userCannotAccessAnotherUsersReplay() throws Exception {
        User owner = createUser("replay-owner@example.com");
        User other = createUser("replay-other@example.com");
        TradingAccount account = createAccount(owner);
        Trade trade = createTrade(owner, account, "MESU6", LocalDateTime.now().minusDays(3));
        String token = login(other.getEmail());

        mockMvc.perform(get("/api/v1/trades/" + trade.getId() + "/replay")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isNotFound());
    }

    @Test
    void rootOnlyTradeRequiresUserConfirmedContractBeforeProviderRequest() throws Exception {
        User user = createUser("replay-contract-confirm@example.com");
        TradingAccount account = createAccount(user);
        LocalDateTime entryTime = LocalDateTime.now().minusDays(3);
        Trade trade = createTrade(user, account, "NQ", entryTime);
        String token = login(user.getEmail());
        String contractSymbol = "NQU" + (entryTime.getYear() % 10);

        mockMvc.perform(post("/api/v1/trades/" + trade.getId() + "/replay/generate")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("CONTRACT_RESOLUTION_FAILED"))
                .andExpect(jsonPath("$.failureCode").value("CONTRACT_REQUIRED"));

        mockMvc.perform(get("/api/v1/trades/" + trade.getId() + "/replay/contract-candidates")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.rootInstrument").value("NQ"))
                .andExpect(jsonPath("$.candidates[0].normalizedSymbol").exists());

        mockMvc.perform(put("/api/v1/trades/" + trade.getId() + "/replay/contract")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"contractSymbol":"%s"}
                                """.formatted(contractSymbol)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("PROVIDER_NOT_CONFIGURED"))
                .andExpect(jsonPath("$.resolvedProviderSymbol").value(contractSymbol));
    }

    private String login(String email) throws Exception {
        String response = mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"email":"%s","password":"password123"}
                                """.formatted(email)))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        return objectMapper.readTree(response).get("token").asText();
    }

    private User createUser(String email) {
        return userRepository.save(User.builder()
                .name("Replay User")
                .email(email)
                .passwordHash(passwordEncoder.encode("password123"))
                .emailVerified(true)
                .role(UserRole.USER)
                .plan(PlanType.PRO)
                .subscriptionStatus(SubscriptionStatus.ACTIVE)
                .build());
    }

    private TradingAccount createAccount(User user) {
        return accountRepository.save(TradingAccount.builder()
                .user(user)
                .name("Replay Account")
                .propFirm(PropFirmType.TOPSTEP)
                .accountType(TradingAccountType.PERSONAL)
                .startingBalance(BigDecimal.valueOf(50_000))
                .status(TradingAccountStatus.ACTIVE)
                .build());
    }

    private Trade createTrade(User user, TradingAccount account, String symbol, LocalDateTime entryTime) {
        return tradeRepository.save(Trade.builder()
                .user(user)
                .account(account)
                .symbol(symbol)
                .market("FUTURES")
                .tradeDate(entryTime.toLocalDate())
                .entryTime(entryTime)
                .exitTime(entryTime.plusMinutes(12))
                .side("LONG")
                .session(SessionType.NY_AM)
                .entryPrice(new BigDecimal("5000.00"))
                .exitPrice(new BigDecimal("5003.00"))
                .quantity(1)
                .pnlAmount(new BigDecimal("150.00"))
                .outcome(OutcomeType.WIN)
                .build());
    }
}
