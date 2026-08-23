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
import com.tradepilot.security.JwtService;
import com.tradepilot.tradereplay.service.ContractSymbolSource;
import com.tradepilot.tradereplay.service.HistoricalMarketDataProvider;
import com.tradepilot.tradereplay.service.HistoricalMarketDataProviderRegistry;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest(properties = {
        "tradepilot.features.v3-enabled=true",
        "tradepilot.market-data.provider=databento"
})
@AutoConfigureMockMvc
@ActiveProfiles("test")
class TradeReplayDemoEligibilityTests {
    @Autowired MockMvc mockMvc;
    @Autowired ObjectMapper objectMapper;
    @Autowired UserRepository userRepository;
    @Autowired TradingAccountRepository accountRepository;
    @Autowired TradeRepository tradeRepository;
    @Autowired PasswordEncoder passwordEncoder;
    @Autowired JwtService jwtService;

    @MockBean HistoricalMarketDataProviderRegistry providerRegistry;

    @Test
    void realImportedTradeCanReachProviderSelection() throws Exception {
        HistoricalMarketDataProvider provider = Mockito.mock(HistoricalMarketDataProvider.class);
        when(provider.providerName()).thenReturn("disabled");
        when(providerRegistry.activeProvider()).thenReturn(provider);
        User user = createUser("real-replay-provider@example.com");
        TradingAccount account = createAccount(user);
        Trade trade = createTrade(user, account, "MESU6", "TRADOVATE-REAL-1", ContractSymbolSource.IMPORTED);

        mockMvc.perform(post("/api/v1/trades/" + trade.getId() + "/replay/generate")
                        .header("Authorization", bearer(user)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("PROVIDER_NOT_CONFIGURED"));

        verify(providerRegistry).activeProvider();
    }

    @Test
    void replayCompatibleDemoTradeCanReachProviderSelection() throws Exception {
        HistoricalMarketDataProvider provider = Mockito.mock(HistoricalMarketDataProvider.class);
        when(provider.providerName()).thenReturn("disabled");
        when(providerRegistry.activeProvider()).thenReturn(provider);
        User user = createUser("replay-demo-provider@example.com");
        TradingAccount account = createAccount(user);
        Trade trade = createTrade(user, account, "MESU6", "DEV-DEMO-v5-REPLAY-" + user.getId() + "-7", ContractSymbolSource.ADMIN_BACKFILL);

        mockMvc.perform(post("/api/v1/trades/" + trade.getId() + "/replay/generate")
                        .header("Authorization", bearer(user)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("PROVIDER_NOT_CONFIGURED"));

        verify(providerRegistry).activeProvider();
    }

    @Test
    void generalDemoTradeIsBlockedBeforeProviderSelection() throws Exception {
        User user = createUser("general-demo-blocked@example.com");
        TradingAccount account = createAccount(user);
        Trade trade = createTrade(user, account, "MESM6", "DEV-DEMO-v5-GENERAL-" + user.getId() + "-10", ContractSymbolSource.ADMIN_BACKFILL);

        mockMvc.perform(post("/api/v1/trades/" + trade.getId() + "/replay/generate")
                        .header("Authorization", bearer(user)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("DEMO_REPLAY_UNAVAILABLE"))
                .andExpect(jsonPath("$.failureCode").value("DEMO_REPLAY_UNAVAILABLE"))
                .andExpect(jsonPath("$.provider").isEmpty())
                .andExpect(jsonPath("$.message").value("Replay is not available for this demo trade."));

        verifyNoInteractions(providerRegistry);
    }

    @Test
    void generalDemoRegenerateBypassAttemptIsBlockedBeforeProviderSelection() throws Exception {
        User user = createUser("general-demo-regenerate-blocked@example.com");
        TradingAccount account = createAccount(user);
        Trade trade = createTrade(user, account, "MESM6", "DEV-DEMO-v5-GENERAL-" + user.getId() + "-11", ContractSymbolSource.ADMIN_BACKFILL);

        mockMvc.perform(post("/api/v1/trades/" + trade.getId() + "/replay/regenerate")
                        .header("Authorization", bearer(user)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("DEMO_REPLAY_UNAVAILABLE"));

        verifyNoInteractions(providerRegistry);
    }

    @Test
    void contractUpdateCannotBypassGeneralDemoReplayGate() throws Exception {
        User user = createUser("general-demo-contract-blocked@example.com");
        TradingAccount account = createAccount(user);
        Trade trade = createTrade(user, account, "MES", "DEV-DEMO-v5-GENERAL-" + user.getId() + "-12", null);

        mockMvc.perform(put("/api/v1/trades/" + trade.getId() + "/replay/contract")
                        .header("Authorization", bearer(user))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"contractSymbol":"MESU6"}
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("DEMO_REPLAY_UNAVAILABLE"));

        verifyNoInteractions(providerRegistry);
        Trade persisted = tradeRepository.findById(trade.getId()).orElseThrow();
        org.assertj.core.api.Assertions.assertThat(persisted.getContractSymbol()).isNull();
    }

    private String bearer(User user) {
        return "Bearer " + jwtService.generateToken(user);
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

    private Trade createTrade(User user, TradingAccount account, String symbol, String externalTradeKey, ContractSymbolSource source) {
        Trade trade = Trade.builder()
                .user(user)
                .account(account)
                .symbol(symbol)
                .market("FUTURES")
                .tradeDate(LocalDate.of(2026, 7, 8))
                .entryTime(LocalDateTime.of(2026, 7, 8, 5, 45))
                .exitTime(LocalDateTime.of(2026, 7, 8, 5, 57))
                .side("LONG")
                .session(SessionType.NY_AM)
                .entryPrice(new BigDecimal("7492.00"))
                .exitPrice(new BigDecimal("7494.00"))
                .quantity(1)
                .pnlAmount(new BigDecimal("10.00"))
                .outcome(OutcomeType.WIN)
                .externalTradeKey(externalTradeKey)
                .build();
        if (!"MES".equals(symbol)) {
            trade.setContractSymbol(symbol);
            trade.setContractSymbolSource(source == null ? null : source.name());
        }
        return tradeRepository.save(trade);
    }
}
