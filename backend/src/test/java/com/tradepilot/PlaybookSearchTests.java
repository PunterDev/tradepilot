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
import com.tradepilot.playbook.entity.PlaybookSetup;
import com.tradepilot.playbook.enums.PlaybookCategory;
import com.tradepilot.playbook.enums.PlaybookDirection;
import com.tradepilot.playbook.enums.PlaybookStatus;
import com.tradepilot.playbook.repository.PlaybookSetupRepository;
import com.tradepilot.playbook.service.PlaybookService;
import com.tradepilot.repository.TradingAccountRepository;
import com.tradepilot.repository.UserRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_CLASS)
class PlaybookSearchTests {
    @Autowired MockMvc mockMvc;
    @Autowired ObjectMapper objectMapper;
    @Autowired UserRepository userRepository;
    @Autowired TradingAccountRepository accountRepository;
    @Autowired PlaybookSetupRepository setupRepository;
    @Autowired PlaybookService playbookService;
    @Autowired PasswordEncoder passwordEncoder;

    @Test
    void serviceSearchTreatsNullEmptyAndWhitespaceAsAbsent() {
        User user = createUser("playbook-search-blank@example.com");
        TradingAccount account = createAccount(user, "Blank Search Account");
        createSetup(user, account, "IFVG Continuation", PlaybookStatus.TESTING);
        createSetup(user, account, "Opening Range Breakout", PlaybookStatus.DRAFT);

        assertThat(playbookService.list(user, null, null, null, 0, 20)).hasSize(2);
        assertThat(playbookService.list(user, null, null, "", 0, 20)).hasSize(2);
        assertThat(playbookService.list(user, null, null, "   ", 0, 20)).hasSize(2);
    }

    @Test
    void serviceSearchIsCaseInsensitivePartialAndPreservesFilters() {
        User user = createUser("playbook-search-filters@example.com");
        User other = createUser("playbook-search-other@example.com");
        TradingAccount accountOne = createAccount(user, "Account One");
        TradingAccount accountTwo = createAccount(user, "Account Two");
        createSetup(user, accountOne, "IFVG Continuation", PlaybookStatus.TESTING);
        createSetup(user, accountOne, "Opening Range Breakout", PlaybookStatus.DRAFT);
        createSetup(user, accountTwo, "IFVG London Reversal", PlaybookStatus.VALIDATED);
        createSetup(other, createAccount(other, "Other Account"), "IFVG Other User", PlaybookStatus.TESTING);

        assertThat(names(playbookService.list(user, null, null, "fvg", 0, 20)))
                .containsExactlyInAnyOrder("IFVG Continuation", "IFVG London Reversal");
        assertThat(names(playbookService.list(user, null, null, "IFVG", 0, 20)))
                .containsExactlyInAnyOrder("IFVG Continuation", "IFVG London Reversal");
        assertThat(names(playbookService.list(user, accountOne.getId(), null, "ifvg", 0, 20)))
                .containsExactly("IFVG Continuation");
        assertThat(names(playbookService.list(user, null, PlaybookStatus.VALIDATED, "ifvg", 0, 20)))
                .containsExactly("IFVG London Reversal");
        assertThat(playbookService.list(user, null, null, "does-not-exist", 0, 20)).isEmpty();
    }

    @Test
    void serviceSearchPaginatesAndOrdersNewestFirst() {
        User user = createUser("playbook-search-order@example.com");
        TradingAccount account = createAccount(user, "Order Account");
        createSetup(user, account, "First Setup", PlaybookStatus.TESTING);
        createSetup(user, account, "Second Setup", PlaybookStatus.TESTING);

        List<String> page = names(playbookService.list(user, null, null, null, 0, 1));

        assertThat(page).containsExactly("Second Setup");
    }

    @Test
    void controllerLegacyAndV1RoutesHandleMissingAndBlankSearch() throws Exception {
        User user = createUser("playbook-controller-routes@example.com");
        TradingAccount account = createAccount(user, "Controller Account");
        createSetup(user, account, "FVG Morning Setup", PlaybookStatus.TESTING);
        createSetup(user, account, "Breaker Setup", PlaybookStatus.DRAFT);
        String token = tokenFor(user);

        assertThat(arraySize(getJson(token, "/api/playbook"))).isEqualTo(2);
        assertThat(arraySize(getJson(token, "/api/playbook?search="))).isEqualTo(2);
        JsonNode fvg = getJson(token, "/api/playbook?search=fvg");
        assertThat(arraySize(fvg)).isEqualTo(1);
        assertThat(fvg.get(0).get("name").asText()).isEqualTo("FVG Morning Setup");
        assertThat(getJson(token, "/api/playbook/overview").get("totalActiveSetups").asInt()).isEqualTo(2);
        assertThat(arraySize(getJson(token, "/api/v1/playbook"))).isEqualTo(2);
        assertThat(getJson(token, "/api/v1/playbook/overview").get("totalActiveSetups").asInt()).isEqualTo(2);
    }

    @Test
    void controllerSearchEnforcesOwnership() throws Exception {
        User user = createUser("playbook-controller-owner@example.com");
        User other = createUser("playbook-controller-owner-other@example.com");
        createSetup(user, createAccount(user, "Owner Account"), "FVG Owner Setup", PlaybookStatus.TESTING);
        createSetup(other, createAccount(other, "Other Owner Account"), "FVG Hidden Setup", PlaybookStatus.TESTING);
        String token = tokenFor(user);

        JsonNode response = getJson(token, "/api/playbook?search=fvg");

        assertThat(arraySize(response)).isEqualTo(1);
        assertThat(response.get(0).get("name").asText()).isEqualTo("FVG Owner Setup");
    }

    private List<String> names(List<com.tradepilot.playbook.dto.PlaybookSetupSummaryResponse> responses) {
        return responses.stream().map(com.tradepilot.playbook.dto.PlaybookSetupSummaryResponse::name).toList();
    }

    private int arraySize(JsonNode node) {
        assertThat(node.isArray()).isTrue();
        return node.size();
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
                .name("Playbook Search User")
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

    private PlaybookSetup createSetup(User user, TradingAccount account, String name, PlaybookStatus status) {
        PlaybookSetup setup = new PlaybookSetup();
        setup.setUser(user);
        setup.setAccount(account);
        setup.setName(name);
        setup.setShortDescription(name);
        setup.setDescription("Test setup");
        setup.setCategory(PlaybookCategory.CUSTOM);
        setup.setStatus(status);
        setup.setDirection(PlaybookDirection.BOTH);
        setup.setInstrumentsJson("[]");
        setup.setSessionsJson("[]");
        setup.setTimeframesJson("[]");
        setup.setTagsJson("[]");
        setup.setMarketContextJson("[]");
        setup.setRiskRulesJson("[]");
        setup.setManagementRulesJson("[]");
        setup.setPostTradeRulesJson("[]");
        return setupRepository.save(setup);
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
