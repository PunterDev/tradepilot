package com.tradepilot;

import com.tradepilot.billing.PlanType;
import com.tradepilot.billing.SubscriptionStatus;
import com.tradepilot.entity.User;
import com.tradepilot.repository.TradeRepository;
import com.tradepilot.repository.TradingAccountRepository;
import com.tradepilot.repository.UserRepository;
import com.tradepilot.security.JwtService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@TestPropertySource(properties = "app.dev-tools-enabled=true")
class DevToolsEnabledTests {
    @Autowired MockMvc mockMvc;
    @Autowired UserRepository userRepository;
    @Autowired TradingAccountRepository accountRepository;
    @Autowired TradeRepository tradeRepository;
    @Autowired JwtService jwtService;

    @Test
    void seedDataBelongsOnlyToCurrentUser() throws Exception {
        User userA = createUser("dev-seed-a@example.com");
        User userB = createUser("dev-seed-b@example.com");

        mockMvc.perform(post("/api/dev/seed-demo-data")
                        .contentType(MediaType.APPLICATION_JSON)
                        .header("Authorization", bearer(userA)))
                .andExpect(status().isOk());

        assertThat(accountRepository.countByUser(userA)).isEqualTo(2);
        assertThat(tradeRepository.countByUser(userA)).isBetween(150L, 250L);
        assertThat(accountRepository.countByUser(userB)).isZero();
        assertThat(tradeRepository.countByUser(userB)).isZero();
    }

    @Test
    void clearDataOnlyDeletesCurrentUserData() throws Exception {
        User userA = createUser("dev-clear-a@example.com");
        User userB = createUser("dev-clear-b@example.com");

        mockMvc.perform(post("/api/dev/seed-demo-data").header("Authorization", bearer(userA)))
                .andExpect(status().isOk());
        mockMvc.perform(post("/api/dev/seed-demo-data").header("Authorization", bearer(userB)))
                .andExpect(status().isOk());

        mockMvc.perform(post("/api/dev/clear-demo-data").header("Authorization", bearer(userA)))
                .andExpect(status().isOk());

        assertThat(accountRepository.countByUser(userA)).isZero();
        assertThat(tradeRepository.countByUser(userA)).isZero();
        assertThat(accountRepository.countByUser(userB)).isEqualTo(2);
        assertThat(tradeRepository.countByUser(userB)).isBetween(150L, 250L);
    }

    @Test
    void mockUpgradeAndDowngradeMutateOnlyCurrentUserPlan() throws Exception {
        User user = createUser("dev-plan@example.com");

        mockMvc.perform(post("/api/dev/mock-upgrade-pro").header("Authorization", bearer(user)))
                .andExpect(status().isOk());

        User upgraded = userRepository.findById(user.getId()).orElseThrow();
        assertThat(upgraded.getPlan()).isEqualTo(PlanType.PRO);
        assertThat(upgraded.getSubscriptionStatus()).isEqualTo(SubscriptionStatus.ACTIVE);
        assertThat(upgraded.getStripeCustomerId()).isEqualTo("mock_customer");
        assertThat(upgraded.getStripeSubscriptionId()).isEqualTo("mock_subscription");

        mockMvc.perform(post("/api/dev/mock-downgrade-free").header("Authorization", bearer(upgraded)))
                .andExpect(status().isOk());

        User downgraded = userRepository.findById(user.getId()).orElseThrow();
        assertThat(downgraded.getPlan()).isEqualTo(PlanType.FREE);
        assertThat(downgraded.getSubscriptionStatus()).isEqualTo(SubscriptionStatus.NONE);
        assertThat(downgraded.getStripeCustomerId()).isNull();
        assertThat(downgraded.getStripeSubscriptionId()).isNull();
    }

    private User createUser(String email) {
        return userRepository.save(User.builder()
                .name(email)
                .email(email)
                .passwordHash("test-password-hash")
                .emailVerified(true)
                .plan(PlanType.FREE)
                .subscriptionStatus(SubscriptionStatus.NONE)
                .build());
    }

    private String bearer(User user) {
        return "Bearer " + jwtService.generateToken(user);
    }
}
