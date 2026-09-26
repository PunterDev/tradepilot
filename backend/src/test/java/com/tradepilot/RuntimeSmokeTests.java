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
import com.tradepilot.enums.Side;
import com.tradepilot.enums.TradingAccountStatus;
import com.tradepilot.enums.TradingAccountType;
import com.tradepilot.enums.UserRole;
import com.tradepilot.repository.TradeRepository;
import com.tradepilot.repository.TradingAccountRepository;
import com.tradepilot.repository.UserRepository;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;
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
import java.time.LocalTime;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class RuntimeSmokeTests {
    private static final String TEST_JWT_SECRET = "MDEyMzQ1Njc4OWFiY2RlZjAxMjM0NTY3ODlhYmNkZWY=";

    @Autowired MockMvc mockMvc;
    @Autowired ObjectMapper objectMapper;
    @Autowired UserRepository userRepository;
    @Autowired TradingAccountRepository tradingAccountRepository;
    @Autowired TradeRepository tradeRepository;
    @Autowired PasswordEncoder passwordEncoder;

    @Test
    void publicHealthEndpointReportsOnlyApplicationStatus() throws Exception {
        mockMvc.perform(get("/actuator/health"))
                .andExpect(status().isOk())
                .andExpect(org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath("$.status").value("UP"))
                .andExpect(org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath("$.components").doesNotExist());
    }

    @Test
    void authenticatedCoreDashboardEndpointsReturnOk() throws Exception {
        User user = createUser("runtime-smoke@example.com");
        TradingAccount account = createAccount(user);
        createTrade(user, account);
        String token = tokenFor(user);

        mockMvc.perform(get("/api/accounts").header("Authorization", "Bearer " + token))
                .andExpect(status().isOk());

        String trades = mockMvc.perform(get("/api/trades").header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andReturn()
                .getResponse()
                .getContentAsString();
        JsonNode tradesJson = objectMapper.readTree(trades);
        assertThat(tradesJson.get("content")).hasSizeGreaterThanOrEqualTo(1);

        String calendar = mockMvc.perform(get("/api/analytics/calendar-summary").header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andReturn()
                .getResponse()
                .getContentAsString();
        JsonNode calendarJson = objectMapper.readTree(calendar);
        assertThat(calendarJson).hasSizeGreaterThanOrEqualTo(1);

        String heatmap = mockMvc.perform(get("/api/analytics/heatmap").header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andReturn()
                .getResponse()
                .getContentAsString();
        JsonNode heatmapJson = objectMapper.readTree(heatmap);
        assertThat(heatmapJson).hasSizeGreaterThanOrEqualTo(1);
        assertThat(heatmapJson)
                .anySatisfy(cell -> {
                    assertThat(cell.get("tradeCount").asInt()).isGreaterThanOrEqualTo(1);
                    assertThat(cell.get("sessionBucket").asText()).isEqualTo("NY_AM");
                });

        mockMvc.perform(get("/api/stats/summary").header("Authorization", "Bearer " + token))
                .andExpect(status().isOk());

        mockMvc.perform(get("/api/billing/usage").header("Authorization", "Bearer " + token))
                .andExpect(status().isOk());
    }

    @Test
    void heatmapBucketsTradesByEntryTimeInsteadOfStoredSession() throws Exception {
        User user = createUser("runtime-heatmap@example.com");
        TradingAccount account = createAccount(user);
        createTradeAt(user, account, LocalTime.of(9, 35), BigDecimal.valueOf(20));
        createTradeAt(user, account, LocalTime.of(10, 0), BigDecimal.valueOf(30));
        createTradeAt(user, account, LocalTime.of(11, 15), BigDecimal.valueOf(-10));
        createTradeAt(user, account, LocalTime.of(12, 45), BigDecimal.valueOf(40));
        createTradeAt(user, account, LocalTime.of(14, 0), BigDecimal.valueOf(-25));
        createTradeAt(user, account, LocalTime.of(18, 0), BigDecimal.valueOf(5));
        String token = tokenFor(user);

        String heatmap = mockMvc.perform(get("/api/analytics/heatmap").header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andReturn()
                .getResponse()
                .getContentAsString();

        Map<String, Integer> countsByBucket = new HashMap<>();
        objectMapper.readTree(heatmap).forEach(cell ->
                countsByBucket.put(cell.get("sessionBucket").asText(), cell.get("tradeCount").asInt())
        );

        assertThat(countsByBucket).containsEntry("NY_OPEN_FIRST_15", 1);
        assertThat(countsByBucket).containsEntry("NY_AM", 1);
        assertThat(countsByBucket).containsEntry("LATE_MORNING", 1);
        assertThat(countsByBucket).containsEntry("LUNCH", 1);
        assertThat(countsByBucket).containsEntry("PM_SESSION", 1);
        assertThat(countsByBucket).containsEntry("OTHER", 1);
    }

    @Test
    void behavioralInsightsUsePrimarySessionWhenOnlyOneSessionBucketExists() throws Exception {
        User user = createUser("runtime-behavioral-primary@example.com");
        TradingAccount account = createAccount(user);
        createTradeInSession(user, account, SessionType.NY_AM, BigDecimal.valueOf(971.50), LocalTime.of(9, 45));
        String token = tokenFor(user);

        JsonNode insights = behavioralInsights(token);

        assertThat(insights).anySatisfy(insight -> {
            assertThat(insight.get("title").asText()).isEqualTo("Primary trading session detected");
            assertThat(insight.get("message").asText())
                    .contains("NY_AM")
                    .contains("total PnL $971.50")
                    .contains("1 trades");
        });
        assertThat(insights).noneSatisfy(insight ->
                assertThat(insight.get("title").asText()).isIn("Weakest trading session detected", "Best trading session detected")
        );
    }

    @Test
    void behavioralInsightsShowBestAndWeakestWhenOneSessionIsNegative() throws Exception {
        User user = createUser("runtime-behavioral-negative@example.com");
        TradingAccount account = createAccount(user);
        createTradeInSession(user, account, SessionType.NY_AM, BigDecimal.valueOf(450), LocalTime.of(9, 50));
        createTradeInSession(user, account, SessionType.NY_PM, BigDecimal.valueOf(-125), LocalTime.of(14, 0));
        String token = tokenFor(user);

        JsonNode insights = behavioralInsights(token);

        assertThat(insights).anySatisfy(insight -> {
            assertThat(insight.get("title").asText()).isEqualTo("Best trading session detected");
            assertThat(insight.get("message").asText()).contains("NY_AM").contains("total PnL $450.00");
        });
        assertThat(insights).anySatisfy(insight -> {
            assertThat(insight.get("title").asText()).isEqualTo("Weakest trading session detected");
            assertThat(insight.get("message").asText()).contains("NY_PM").contains("total PnL -$125.00");
        });
    }

    @Test
    void behavioralInsightsUseSofterWordingWhenAllSessionsAreProfitable() throws Exception {
        User user = createUser("runtime-behavioral-profitable@example.com");
        TradingAccount account = createAccount(user);
        createTradeInSession(user, account, SessionType.NY_AM, BigDecimal.valueOf(500), LocalTime.of(10, 0));
        createTradeInSession(user, account, SessionType.NY_PM, BigDecimal.valueOf(120), LocalTime.of(14, 15));
        String token = tokenFor(user);

        JsonNode insights = behavioralInsights(token);

        assertThat(insights).anySatisfy(insight -> {
            assertThat(insight.get("title").asText()).isEqualTo("Best trading session detected");
            assertThat(insight.get("message").asText()).contains("NY_AM").contains("total PnL $500.00");
        });
        assertThat(insights).anySatisfy(insight -> {
            assertThat(insight.get("title").asText()).isEqualTo("Lowest-performing profitable session");
            assertThat(insight.get("message").asText()).contains("NY_PM").contains("total PnL $120.00");
        });
        assertThat(insights).noneSatisfy(insight ->
                assertThat(insight.get("title").asText()).isEqualTo("Weakest trading session detected")
        );
    }

    @Test
    void behavioralInsightsDoNotEmitDuplicateBestAndWorstForSameSession() throws Exception {
        User user = createUser("runtime-behavioral-duplicate@example.com");
        TradingAccount account = createAccount(user);
        createTradeInSession(user, account, SessionType.NY_AM, BigDecimal.valueOf(100), LocalTime.of(9, 50));
        createTradeInSession(user, account, SessionType.NY_AM, BigDecimal.valueOf(-25), LocalTime.of(10, 15));
        String token = tokenFor(user);

        JsonNode insights = behavioralInsights(token);

        long sessionInsightCount = 0;
        for (JsonNode insight : insights) {
            if (insight.get("type").asText().startsWith("SESSION")) {
                sessionInsightCount++;
            }
        }
        assertThat(sessionInsightCount).isEqualTo(1);
        assertThat(insights).anySatisfy(insight ->
                assertThat(insight.get("title").asText()).isEqualTo("Primary trading session detected")
        );
    }

    @Test
    void expiredJwtReturnsUnauthorized() throws Exception {
        User user = createUser("runtime-expired-token@example.com");
        String expiredToken = Jwts.builder()
                .setSubject(user.getEmail())
                .setIssuedAt(new Date(System.currentTimeMillis() - 120_000))
                .setExpiration(new Date(System.currentTimeMillis() - 60_000))
                .signWith(Keys.hmacShaKeyFor(Decoders.BASE64.decode(TEST_JWT_SECRET)), SignatureAlgorithm.HS256)
                .compact();

        mockMvc.perform(get("/api/accounts").header("Authorization", "Bearer " + expiredToken))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void authMeWithoutJwtReturnsUnauthorized() throws Exception {
        mockMvc.perform(get("/api/auth/me"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void authMeWithMalformedJwtReturnsUnauthorized() throws Exception {
        mockMvc.perform(get("/api/auth/me").header("Authorization", "Bearer malformed.invalid.jwt"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void authMeWithExpiredJwtReturnsUnauthorized() throws Exception {
        User user = createUser("runtime-auth-me-expired-token@example.com");
        String expiredToken = Jwts.builder()
                .setSubject(user.getEmail())
                .setIssuedAt(new Date(System.currentTimeMillis() - 120_000))
                .setExpiration(new Date(System.currentTimeMillis() - 60_000))
                .signWith(Keys.hmacShaKeyFor(Decoders.BASE64.decode(TEST_JWT_SECRET)), SignatureAlgorithm.HS256)
                .compact();

        mockMvc.perform(get("/api/auth/me").header("Authorization", "Bearer " + expiredToken))
                .andExpect(status().isUnauthorized());
    }

    private User createUser(String email) {
        return userRepository.save(User.builder()
                .name("Runtime Smoke")
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
                .name("Runtime Account")
                .propFirm(PropFirmType.TOPSTEP)
                .accountType(TradingAccountType.PERSONAL)
                .startingBalance(BigDecimal.valueOf(50_000))
                .status(TradingAccountStatus.ACTIVE)
                .build());
    }

    private Trade createTrade(User user, TradingAccount account) {
        return tradeRepository.save(Trade.builder()
                .user(user)
                .account(account)
                .symbol("NQ")
                .market("FUTURES")
                .tradeDate(LocalDate.of(2026, 5, 29))
                .entryTime(LocalDateTime.of(2026, 5, 29, 9, 45))
                .exitTime(LocalDateTime.of(2026, 5, 29, 10, 5))
                .side(Side.LONG.name())
                .setup(SetupType.FVG)
                .session(SessionType.NY_AM)
                .entryPrice(BigDecimal.valueOf(19000))
                .exitPrice(BigDecimal.valueOf(19020))
                .quantity(1)
                .riskAmount(BigDecimal.valueOf(100))
                .pnlAmount(BigDecimal.valueOf(120))
                .pnlR(BigDecimal.valueOf(1.2))
                .outcome(OutcomeType.WIN)
                .followedPlan(true)
                .build());
    }

    private Trade createTradeAt(User user, TradingAccount account, LocalTime entryTime, BigDecimal pnl) {
        LocalDate date = LocalDate.of(2026, 6, 1);
        return tradeRepository.save(Trade.builder()
                .user(user)
                .account(account)
                .symbol("NQ")
                .market("FUTURES")
                .tradeDate(date)
                .entryTime(LocalDateTime.of(date, entryTime))
                .exitTime(LocalDateTime.of(date, entryTime.plusMinutes(10)))
                .side(Side.LONG.name())
                .setup(SetupType.FVG)
                .session(SessionType.NY_AM)
                .entryPrice(BigDecimal.valueOf(19000))
                .exitPrice(BigDecimal.valueOf(19010))
                .quantity(1)
                .riskAmount(BigDecimal.valueOf(100))
                .pnlAmount(pnl)
                .pnlR(BigDecimal.ONE)
                .outcome(pnl.signum() >= 0 ? OutcomeType.WIN : OutcomeType.LOSS)
                .followedPlan(true)
                .build());
    }

    private Trade createTradeInSession(User user, TradingAccount account, SessionType session, BigDecimal pnl, LocalTime entryTime) {
        LocalDate date = LocalDate.of(2026, 6, 2);
        return tradeRepository.save(Trade.builder()
                .user(user)
                .account(account)
                .symbol("NQ")
                .market("FUTURES")
                .tradeDate(date)
                .entryTime(LocalDateTime.of(date, entryTime))
                .exitTime(LocalDateTime.of(date, entryTime.plusMinutes(10)))
                .side(Side.LONG.name())
                .setup(SetupType.FVG)
                .session(session)
                .entryPrice(BigDecimal.valueOf(19000))
                .exitPrice(BigDecimal.valueOf(19010))
                .quantity(1)
                .riskAmount(BigDecimal.valueOf(100))
                .pnlAmount(pnl)
                .pnlR(pnl.divide(BigDecimal.valueOf(100), 2, java.math.RoundingMode.HALF_UP))
                .outcome(pnl.signum() >= 0 ? OutcomeType.WIN : OutcomeType.LOSS)
                .followedPlan(true)
                .build());
    }

    private JsonNode behavioralInsights(String token) throws Exception {
        String response = mockMvc.perform(get("/api/analytics/behavioral-insights").header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andReturn()
                .getResponse()
                .getContentAsString();
        return objectMapper.readTree(response);
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
