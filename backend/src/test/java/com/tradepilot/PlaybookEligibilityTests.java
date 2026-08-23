package com.tradepilot;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.tradepilot.billing.AiCreditUsageRepository;
import com.tradepilot.billing.PlanType;
import com.tradepilot.billing.SubscriptionStatus;
import com.tradepilot.entity.Trade;
import com.tradepilot.entity.TradingAccount;
import com.tradepilot.entity.User;
import com.tradepilot.enums.OutcomeType;
import com.tradepilot.enums.PropFirmType;
import com.tradepilot.enums.SessionType;
import com.tradepilot.enums.TradingAccountStatus;
import com.tradepilot.enums.TradingAccountType;
import com.tradepilot.enums.UserRole;
import com.tradepilot.playbook.entity.PlaybookChecklistItem;
import com.tradepilot.playbook.entity.PlaybookSetup;
import com.tradepilot.playbook.entity.TradePlaybookAssignment;
import com.tradepilot.playbook.enums.AssignmentSource;
import com.tradepilot.playbook.enums.ChecklistType;
import com.tradepilot.playbook.enums.ExecutionQualityGrade;
import com.tradepilot.playbook.enums.PlaybookCategory;
import com.tradepilot.playbook.enums.PlaybookDirection;
import com.tradepilot.playbook.enums.PlaybookStatus;
import com.tradepilot.playbook.enums.SetupQualityGrade;
import com.tradepilot.playbook.repository.PlaybookChecklistItemRepository;
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
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.annotation.DirtiesContext;
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
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_CLASS)
class PlaybookEligibilityTests {
    @Autowired MockMvc mockMvc;
    @Autowired ObjectMapper objectMapper;
    @Autowired UserRepository userRepository;
    @Autowired TradingAccountRepository accountRepository;
    @Autowired TradeRepository tradeRepository;
    @Autowired PlaybookSetupRepository setupRepository;
    @Autowired PlaybookChecklistItemRepository checklistRepository;
    @Autowired TradePlaybookAssignmentRepository assignmentRepository;
    @Autowired AiCreditUsageRepository creditUsageRepository;
    @Autowired PasswordEncoder passwordEncoder;

    @Test
    void detailResponseIncludesIncompleteEligibilityForZeroTradeDraft() throws Exception {
        User user = createUser("playbook-eligibility-incomplete@example.com");
        TradingAccount account = createAccount(user, "Incomplete Account");
        PlaybookSetup setup = createSetup(user, account, false);
        String token = tokenFor(user);

        JsonNode response = getJson(token, "/api/playbook/" + setup.getId());
        JsonNode eligibility = response.get("analysis").get("eligibility");

        assertThat(eligibility.get("detailState").asText()).isEqualTo("INCOMPLETE_SETUP");
        assertThat(eligibility.get("definitionComplete").asBoolean()).isFalse();
        assertThat(eligibility.get("linkedTradeCount").asLong()).isZero();
        assertThat(eligibility.get("reviewedTradeCount").asLong()).isZero();
        assertThat(eligibility.get("aiReviewEligible").asBoolean()).isFalse();
        assertThat(eligibility.get("missingRequirements").toString()).contains("MARKET_CONTEXT_REQUIRED", "TEN_REVIEWED_TRADES_REQUIRED");
    }

    @Test
    void ineligibleRegenerationReturnsStructuredConflictAndConsumesZeroCredits() throws Exception {
        User user = createUser("playbook-eligibility-conflict@example.com");
        TradingAccount account = createAccount(user, "Conflict Account");
        PlaybookSetup setup = createSetup(user, account, true);
        addAssignments(user, account, setup, 9, true);
        String token = tokenFor(user);
        long beforeCredits = creditUsageRepository.count();

        String body = mockMvc.perform(post("/api/ai/playbook/" + setup.getId() + "/regenerate").header("Authorization", "Bearer " + token))
                .andExpect(status().isConflict())
                .andReturn()
                .getResponse()
                .getContentAsString();

        JsonNode response = objectMapper.readTree(body);
        assertThat(response.get("errorCode").asText()).isEqualTo("PLAYBOOK_AI_REVIEW_INELIGIBLE");
        assertThat(response.get("requestId").asText()).isNotBlank();
        assertThat(response.get("requiredTradeCount").asInt()).isEqualTo(10);
        assertThat(response.get("currentEligibleTradeCount").asLong()).isEqualTo(9);
        assertThat(creditUsageRepository.count()).isEqualTo(beforeCredits);
    }

    @Test
    void exactlyTenReviewedTradesAllowsRegenerationAndConsumesThreeCredits() throws Exception {
        User user = createUser("playbook-eligibility-ready@example.com");
        TradingAccount account = createAccount(user, "Ready Account");
        PlaybookSetup setup = createSetup(user, account, true);
        addAssignments(user, account, setup, 10, true);
        String token = tokenFor(user);
        long beforeCredits = creditUsageRepository.count();

        String body = mockMvc.perform(post("/api/v1/ai/playbook/" + setup.getId() + "/regenerate").header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andReturn()
                .getResponse()
                .getContentAsString();

        JsonNode response = objectMapper.readTree(body);
        assertThat(response.get("creditsConsumed").asInt()).isEqualTo(3);
        assertThat(response.get("aiReview").isNull()).isFalse();
        assertThat(response.get("eligibility").get("aiReviewEligible").asBoolean()).isTrue();
        assertThat(creditUsageRepository.count()).isEqualTo(beforeCredits + 1);
    }

    @Test
    void detailStateAdvancesThroughNoTradesEarlyEvidenceComparisonAndMature() throws Exception {
        User user = createUser("playbook-eligibility-states@example.com");
        TradingAccount account = createAccount(user, "States Account");
        String token = tokenFor(user);

        PlaybookSetup noTrades = createSetup(user, account, true);
        assertThat(eligibilityState(token, noTrades)).isEqualTo("NO_LINKED_TRADES");

        PlaybookSetup early = createSetup(user, account, true);
        addAssignments(user, account, early, 4, true);
        assertThat(eligibilityState(token, early)).isEqualTo("EARLY_EVIDENCE");

        PlaybookSetup comparison = createSetup(user, account, true);
        addAssignments(user, account, comparison, 5, true);
        assertThat(eligibilityState(token, comparison)).isEqualTo("COMPARISON_ELIGIBLE");

        PlaybookSetup mature = createSetup(user, account, true);
        addAssignments(user, account, mature, 30, true);
        assertThat(eligibilityState(token, mature)).isEqualTo("MATURE_SETUP");
    }

    @Test
    void legacyAndVersionedDetailRoutesExposeSameEligibility() throws Exception {
        User user = createUser("playbook-eligibility-routes@example.com");
        TradingAccount account = createAccount(user, "Routes Account");
        PlaybookSetup setup = createSetup(user, account, true);
        String token = tokenFor(user);

        assertThat(getJson(token, "/api/playbook/" + setup.getId()).get("analysis").get("eligibility").get("detailState").asText()).isEqualTo("NO_LINKED_TRADES");
        assertThat(getJson(token, "/api/v1/playbook/" + setup.getId()).get("analysis").get("eligibility").get("detailState").asText()).isEqualTo("NO_LINKED_TRADES");
    }

    private String eligibilityState(String token, PlaybookSetup setup) throws Exception {
        return getJson(token, "/api/playbook/" + setup.getId()).get("analysis").get("eligibility").get("detailState").asText();
    }

    private JsonNode getJson(String token, String path) throws Exception {
        String response = mockMvc.perform(get(path).header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andReturn()
                .getResponse()
                .getContentAsString();
        return objectMapper.readTree(response);
    }

    private User createUser(String email) {
        return userRepository.save(User.builder()
                .name("Playbook Eligibility User")
                .email(email)
                .passwordHash(passwordEncoder.encode("password123"))
                .emailVerified(true)
                .role(UserRole.USER)
                .plan(PlanType.PRO)
                .subscriptionStatus(SubscriptionStatus.ACTIVE)
                .build());
    }

    private TradingAccount createAccount(User user, String name) {
        return accountRepository.save(TradingAccount.builder()
                .user(user)
                .name(name)
                .propFirm(PropFirmType.TOPSTEP)
                .accountType(TradingAccountType.PERSONAL)
                .startingBalance(BigDecimal.valueOf(50_000))
                .defaultRiskAmount(BigDecimal.valueOf(100))
                .status(TradingAccountStatus.ACTIVE)
                .build());
    }

    private PlaybookSetup createSetup(User user, TradingAccount account, boolean complete) {
        PlaybookSetup setup = new PlaybookSetup();
        setup.setUser(user);
        setup.setAccount(account);
        setup.setName(complete ? "NY AM FVG Continuation" : "playbook");
        setup.setShortDescription("Eligibility test setup");
        setup.setDescription(complete ? "Validated setup definition for testing." : "");
        setup.setCategory(PlaybookCategory.FVG);
        setup.setStatus(PlaybookStatus.DRAFT);
        setup.setDirection(PlaybookDirection.BOTH);
        setup.setInstrumentsJson("[\"MES\"]");
        setup.setSessionsJson("[\"NY_AM\"]");
        setup.setTimeframesJson("[]");
        setup.setTagsJson("[]");
        setup.setMarketContextJson(complete ? "[\"Trend day with clean displacement\"]" : "[]");
        setup.setRiskRulesJson(complete ? "[\"Risk 1R only\"]" : "[]");
        setup.setManagementRulesJson(complete ? "[\"Move stop only after target one\"]" : "[]");
        setup.setPostTradeRulesJson(complete ? "[\"Grade setup after close\"]" : "[]");
        PlaybookSetup saved = setupRepository.save(setup);
        if (complete) {
            addChecklist(saved, ChecklistType.ENTRY, "Entry condition is present");
            addChecklist(saved, ChecklistType.INVALIDATION, "Invalidation is clear");
            addChecklist(saved, ChecklistType.RISK, "Risk is within planned limits");
            addChecklist(saved, ChecklistType.MANAGEMENT, "Management plan is followed");
            addChecklist(saved, ChecklistType.POST_TRADE, "Trade is reviewed after exit");
        }
        return saved;
    }

    private void addChecklist(PlaybookSetup setup, ChecklistType type, String label) {
        PlaybookChecklistItem item = new PlaybookChecklistItem();
        item.setSetup(setup);
        item.setChecklistType(type);
        item.setLabel(label);
        item.setRequired(true);
        item.setWeight(1);
        item.setDisplayOrder(0);
        item.setActive(true);
        checklistRepository.save(item);
    }

    private void addAssignments(User user, TradingAccount account, PlaybookSetup setup, int count, boolean reviewed) {
        for (int i = 0; i < count; i++) {
            Trade trade = new Trade();
            trade.setUser(user);
            trade.setAccount(account);
            trade.setSymbol("MESU6");
            trade.setMarket("FUTURES");
            trade.setTradeDate(LocalDate.of(2026, 6, 1).plusDays(i));
            trade.setEntryTime(LocalDateTime.of(2026, 6, 1, 9, 30).plusDays(i));
            trade.setSide(i % 2 == 0 ? "LONG" : "SHORT");
            trade.setSession(SessionType.NY_AM);
            trade.setPnlAmount(BigDecimal.valueOf(i % 2 == 0 ? 150 : -50));
            trade.setRiskAmount(BigDecimal.valueOf(100));
            trade.setOutcome(i % 2 == 0 ? OutcomeType.WIN : OutcomeType.LOSS);
            trade.setExternalTradeKey(setup.getId() + "-eligibility-" + i);
            Trade savedTrade = tradeRepository.save(trade);

            TradePlaybookAssignment assignment = new TradePlaybookAssignment();
            assignment.setUser(user);
            assignment.setAccount(account);
            assignment.setTrade(savedTrade);
            assignment.setSetup(setup);
            assignment.setSetupQualityGrade(SetupQualityGrade.B);
            assignment.setExecutionQualityGrade(ExecutionQualityGrade.B);
            assignment.setComplianceScore(80);
            assignment.setAssignmentSource(AssignmentSource.MANUAL);
            assignment.setReviewedByUser(reviewed);
            assignmentRepository.save(assignment);
        }
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
