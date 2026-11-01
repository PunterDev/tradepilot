package com.tradepilot;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.tradepilot.billing.PlanType;
import com.tradepilot.billing.SubscriptionStatus;
import com.tradepilot.entity.TradingAccount;
import com.tradepilot.entity.Trade;
import com.tradepilot.entity.User;
import com.tradepilot.enums.OutcomeType;
import com.tradepilot.enums.PropFirmType;
import com.tradepilot.enums.SessionType;
import com.tradepilot.enums.TradingAccountStatus;
import com.tradepilot.enums.TradingAccountType;
import com.tradepilot.enums.UserRole;
import com.tradepilot.repository.TradingAccountRepository;
import com.tradepilot.repository.TradeRepository;
import com.tradepilot.repository.UserRepository;
import com.tradepilot.survival.ProtectionCommitmentStatus;
import com.tradepilot.survival.entity.NextSessionProtectionCommitment;
import com.tradepilot.survival.repository.NextSessionProtectionCommitmentRepository;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class AccountSurvivalOwnershipIntegrationTest {
    @Autowired MockMvc mockMvc;
    @Autowired ObjectMapper objectMapper;
    @Autowired UserRepository users;
    @Autowired TradingAccountRepository accounts;
    @Autowired TradeRepository trades;
    @Autowired NextSessionProtectionCommitmentRepository commitments;
    @Autowired PasswordEncoder passwordEncoder;

    @Test
    void ownerCanUseEverySurvivalResourceWhileAnotherUserGetsTheSamePrivacySafeNotFoundResponseAsUnknownAccount() throws Exception {
        User owner = user("survival-owner@example.com");
        User other = user("survival-other@example.com");
        TradingAccount account = account(owner, "Owner survival account");
        LocalDate date = LocalDate.of(2026, 10, 1);
        String ownerToken = login(owner.getEmail());
        String otherToken = login(other.getEmail());

        String profile = """
                {"currentBalance":50000,"failureThreshold":48000,"dailyLossLimit":500,"plannedRiskPerTrade":100,"timezone":"America/New_York","effectiveDate":"2026-10-01"}
                """;
        mockMvc.perform(put(profilePath(account)).header("Authorization", bearer(ownerToken)).contentType(MediaType.APPLICATION_JSON).content(profile))
                .andExpect(status().isOk()).andExpect(jsonPath("$.accountId").value(account.getId()));
        mockMvc.perform(get(profilePath(account)).header("Authorization", bearer(ownerToken)))
                .andExpect(status().isOk()).andExpect(jsonPath("$.currentBalance").value(50000));
        mockMvc.perform(get(base(account)).param("date", date.toString()).header("Authorization", bearer(ownerToken)))
                .andExpect(status().isOk()).andExpect(jsonPath("$.remainingBuffer").value(2000));

        String commitment = """
                {"effectiveSessionDate":"2026-10-01","maxAttempts":2,"maxNormalizedExposure":20,"maxSessionLoss":200,"postLossPauseMinutes":20,"allowedWindowStart":"09:30:00","allowedWindowEnd":"12:00:00","requireFreshSetupConfirmation":true}
                """;
        mockMvc.perform(put(commitmentPath(account)).header("Authorization", bearer(ownerToken)).contentType(MediaType.APPLICATION_JSON).content(commitment))
                .andExpect(status().isOk()).andExpect(jsonPath("$.status").value("DRAFT"));
        mockMvc.perform(post(commitmentPath(account) + "/" + date + "/accept").header("Authorization", bearer(ownerToken)))
                .andExpect(status().isOk()).andExpect(jsonPath("$.status").value("ACCEPTED"));
        mockMvc.perform(get(commitmentPath(account)).param("date", date.toString()).header("Authorization", bearer(ownerToken)))
                .andExpect(status().isOk()).andExpect(jsonPath("$.maxAttempts").value(2));
        // Once accepted, the commitment must remain immutable and cannot be re-drafted.
        mockMvc.perform(put(commitmentPath(account)).header("Authorization", bearer(ownerToken))
                .contentType(MediaType.APPLICATION_JSON).content(commitment))
                .andExpect(status().isBadRequest());
        mockMvc.perform(get(commitmentPath(account)).param("date", date.toString()).header("Authorization", bearer(ownerToken)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("ACCEPTED"))
                .andExpect(jsonPath("$.acceptedAt").exists())
                .andExpect(jsonPath("$.maxAttempts").value(2));
        mockMvc.perform(post(commitmentPath(account) + "/" + date + "/accept").header("Authorization", bearer(ownerToken)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("ACCEPTED"));
        mockMvc.perform(post(commitmentPath(account) + "/" + date + "/evaluate").header("Authorization", bearer(ownerToken)))
                .andExpect(status().isOk()).andExpect(jsonPath("$.available").value(false));
        mockMvc.perform(get(commitmentPath(account) + "/" + date + "/evaluation").header("Authorization", bearer(ownerToken)))
                .andExpect(status().isOk()).andExpect(jsonPath("$.available").value(false));
        mockMvc.perform(get(commitmentPath(account) + "/" + date + "/evaluations").header("Authorization", bearer(ownerToken)))
                .andExpect(status().isOk()).andExpect(jsonPath("$.evaluatedSessions").value(0));

        for (String path : new String[] { profilePath(account), base(account) + "?date=" + date, commitmentPath(account) + "?date=" + date,
                commitmentPath(account) + "/" + date + "/evaluation", commitmentPath(account) + "/" + date + "/evaluations" }) {
            String body = mockMvc.perform(get(path).header("Authorization", bearer(otherToken))).andExpect(status().isNotFound()).andReturn().getResponse().getContentAsString();
            org.assertj.core.api.Assertions.assertThat(body).doesNotContain("50000", "48000", "Owner survival account", "maxAttempts", "America/New_York");
        }
        mockMvc.perform(put(profilePath(account)).header("Authorization", bearer(otherToken)).contentType(MediaType.APPLICATION_JSON).content(profile)).andExpect(status().isNotFound());
        mockMvc.perform(put(commitmentPath(account)).header("Authorization", bearer(otherToken)).contentType(MediaType.APPLICATION_JSON).content(commitment)).andExpect(status().isNotFound());
        mockMvc.perform(post(commitmentPath(account) + "/" + date + "/accept").header("Authorization", bearer(otherToken))).andExpect(status().isNotFound());
        mockMvc.perform(post(commitmentPath(account) + "/" + date + "/evaluate").header("Authorization", bearer(otherToken))).andExpect(status().isNotFound());
        mockMvc.perform(get(profilePath(account)).header("Authorization", bearer(otherToken))).andExpect(status().isNotFound());
        mockMvc.perform(get(profilePath(999999L)).header("Authorization", bearer(otherToken))).andExpect(status().isNotFound());
        mockMvc.perform(get(profilePath(account))).andExpect(status().isUnauthorized());
        String unknownBody = mockMvc.perform(get("/api/accounts/999999/survival").param("date", date.toString()).header("Authorization", bearer(ownerToken)))
                .andExpect(status().isNotFound()).andReturn().getResponse().getContentAsString();
        org.assertj.core.api.Assertions.assertThat(unknownBody).doesNotContain("Owner survival account", "50000", "48000", "maxAttempts");

        for (String malformedDate : new String[] { "not-a-date", "2026-13-40", "10-25-2026" }) {
            String errorBody = mockMvc.perform(get(base(account)).param("date", malformedDate).header("Authorization", bearer(ownerToken)))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.errorCode").value("INVALID_REQUEST"))
                    .andExpect(jsonPath("$.userMessage").value("Invalid value for parameter `date`. Expected format: YYYY-MM-DD."))
                    .andExpect(jsonPath("$.requestId").exists())
                    .andReturn().getResponse().getContentAsString();
            org.assertj.core.api.Assertions.assertThat(errorBody).doesNotContain("Owner survival account", "50000", "48000", "maxAttempts", "TradePilotApplication");
        }
        mockMvc.perform(get(commitmentPath(account)).param("date", "not-a-date").header("Authorization", bearer(ownerToken)))
                .andExpect(status().isBadRequest()).andExpect(jsonPath("$.errorCode").value("INVALID_REQUEST"));
        mockMvc.perform(post(commitmentPath(account) + "/not-a-date/evaluate").header("Authorization", bearer(ownerToken)))
                .andExpect(status().isBadRequest()).andExpect(jsonPath("$.errorCode").value("INVALID_REQUEST"));
        mockMvc.perform(get(commitmentPath(account) + "/2026-13-40/evaluation").header("Authorization", bearer(ownerToken)))
                .andExpect(status().isBadRequest()).andExpect(jsonPath("$.errorCode").value("INVALID_REQUEST"));
        mockMvc.perform(get(commitmentPath(account) + "/2026-13-40/evaluations").header("Authorization", bearer(ownerToken)))
                .andExpect(status().isBadRequest()).andExpect(jsonPath("$.errorCode").value("INVALID_REQUEST"));
        mockMvc.perform(get(base(account)).param("date", "not-a-date")).andExpect(status().isUnauthorized());
    }

    @Test
    void crossAccountTurningPointIsVisibleOnlyToTheOwnersParticipatingAccounts() throws Exception {
        User owner = user("survival-cross-owner@example.com");
        User other = user("survival-cross-other@example.com");
        TradingAccount source = account(owner, "Owner source account");
        TradingAccount later = account(owner, "Owner later account");
        TradingAccount unrelated = account(owner, "Owner unrelated account");
        LocalDate date = LocalDate.of(2026, 10, 2);
        trades.save(trade(owner, source, date, "NQ", "SHORT", "10:00:05", "10:04:05", "-240", 2));
        trades.save(trade(owner, later, date, "MNQ", "SHORT", "10:12:05", "10:18:05", "-540", 20));
        String ownerToken = login(owner.getEmail());
        String otherToken = login(other.getEmail());

        mockMvc.perform(get(base(source)).param("date", date.toString()).header("Authorization", bearer(ownerToken)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.turningPoint.type").value("CROSS_ACCOUNT_CONTINUATION"))
                .andExpect(jsonPath("$.turningPoint.crossAccountEvidence.sourceAccounts[0]").value("Owner source account"))
                .andExpect(jsonPath("$.turningPoint.crossAccountEvidence.laterAccounts[0]").value("Owner later account"))
                .andExpect(jsonPath("$.turningPoint.stopBeforeActionPnl").value(-240))
                .andExpect(jsonPath("$.turningPoint.lossesAvoided").value(540))
                .andExpect(jsonPath("$.turningPoint.winnersForgone").value(0));
        mockMvc.perform(get(base(later)).param("date", date.toString()).header("Authorization", bearer(ownerToken)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.turningPoint.type").value("CROSS_ACCOUNT_CONTINUATION"));
        mockMvc.perform(get(base(unrelated)).param("date", date.toString()).header("Authorization", bearer(ownerToken)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.turningPoint.detected").value(false));

        String body = mockMvc.perform(get(base(source)).param("date", date.toString()).header("Authorization", bearer(otherToken)))
                .andExpect(status().isNotFound()).andReturn().getResponse().getContentAsString();
        org.assertj.core.api.Assertions.assertThat(body).doesNotContain("Owner source account", "Owner later account", "10:12");
    }

    @Test
    void earlierExistingTurningPointWinsOverLaterCrossAccountCandidate() throws Exception {
        User owner = user("survival-order-owner@example.com");
        TradingAccount source = account(owner, "Ordering source account");
        TradingAccount later = account(owner, "Ordering later account");
        LocalDate date = LocalDate.of(2026, 10, 3);
        trades.save(trade(owner, source, date, "NQ", "LONG", "09:50:05", "09:54:05", "-100", 1));
        trades.save(trade(owner, source, date, "NQ", "LONG", "09:56:05", "09:58:05", "25", 2));
        trades.save(trade(owner, later, date, "MNQ", "LONG", "10:00:05", "10:04:05", "-100", 20));

        mockMvc.perform(get(base(source)).param("date", date.toString()).header("Authorization", bearer(login(owner.getEmail()))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.turningPoint.type").value("POST_LOSS_SIZE_ESCALATION"));
    }

    private String login(String email) throws Exception { String body = mockMvc.perform(post("/api/auth/login").contentType(MediaType.APPLICATION_JSON).content("{\"email\":\"%s\",\"password\":\"password123\"}".formatted(email))).andExpect(status().isOk()).andReturn().getResponse().getContentAsString(); return objectMapper.readTree(body).get("token").asText(); }
    private User user(String email) { return users.save(User.builder().name("Survival test user").email(email).passwordHash(passwordEncoder.encode("password123")).emailVerified(true).role(UserRole.USER).plan(PlanType.FREE).subscriptionStatus(SubscriptionStatus.ACTIVE).build()); }
    private TradingAccount account(User user, String name) { return accounts.save(TradingAccount.builder().user(user).name(name).propFirm(PropFirmType.TOPSTEP).accountType(TradingAccountType.PERSONAL).startingBalance(new BigDecimal("50000")).status(TradingAccountStatus.ACTIVE).build()); }
    private Trade trade(User user, TradingAccount account, LocalDate date, String symbol, String side, String entry, String exit, String pnl, int quantity) {
        return Trade.builder().user(user).account(account).symbol(symbol).market("FUTURES").tradeDate(date).entryTime(time(date, entry)).exitTime(time(date, exit))
                .side(side).session(SessionType.NY_AM).quantity(quantity).pnlAmount(new BigDecimal(pnl))
                .outcome(new BigDecimal(pnl).signum() < 0 ? OutcomeType.LOSS : OutcomeType.WIN).externalTradeKey(account.getId() + "-" + entry).build();
    }
    private LocalDateTime time(LocalDate date, String value) { return LocalDateTime.parse(date + "T" + value); }
    private String base(TradingAccount account) { return "/api/accounts/" + account.getId() + "/survival"; }
    private String profilePath(TradingAccount account) { return base(account) + "/profile"; }
    private String commitmentPath(TradingAccount account) { return base(account) + "/commitment"; }
    private String profilePath(long accountId) { return "/api/accounts/" + accountId + "/survival/profile"; }
    private String bearer(String token) { return "Bearer " + token; }
}
