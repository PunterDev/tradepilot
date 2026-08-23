package com.tradepilot;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.tradepilot.billing.PlanType;
import com.tradepilot.billing.SubscriptionStatus;
import com.tradepilot.entity.User;
import com.tradepilot.enums.UserRole;
import com.tradepilot.featureusage.*;
import com.tradepilot.repository.UserRepository;
import com.tradepilot.security.JwtService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class FeatureFunnelAnalyticsTests {
    @Autowired MockMvc mockMvc;
    @Autowired ObjectMapper objectMapper;
    @Autowired UserRepository userRepository;
    @Autowired FeatureUsageRepository featureUsageRepository;
    @Autowired PasswordEncoder passwordEncoder;
    @Autowired JwtService jwtService;

    @Test
    void adminCanViewLeakDetectorFunnelAndUserCannot() throws Exception {
        User admin = createUser("admin-funnel@tradepilot.local", UserRole.ADMIN, PlanType.PRO);
        User pro = createUser("pro-funnel@trader.local", UserRole.USER, PlanType.PRO);
        LocalDateTime start = LocalDateTime.now().minusDays(3);
        save(pro, FeatureKey.AI_LEAK_DETECTOR, FeatureUsageAction.LEAK_DETECTOR_VIEWED, FeatureUsageEventType.PAGE_VIEW, start);
        save(pro, FeatureKey.AI_LEAK_DETECTOR, FeatureUsageAction.LEAK_DETECTOR_GENERATED, FeatureUsageEventType.AI_GENERATION, start.plusHours(1));
        save(pro, FeatureKey.AI_LEAK_DETECTOR, FeatureUsageAction.LEAK_DETECTOR_LEAK_OPENED, FeatureUsageEventType.USER_ACTION, start.plusHours(2));
        save(pro, FeatureKey.AI_LEAK_DETECTOR, FeatureUsageAction.LEAK_DETECTOR_RECOVERY_PLAN_VIEWED, FeatureUsageEventType.USER_ACTION, start.plusHours(3));
        save(pro, FeatureKey.AI_LEAK_DETECTOR, FeatureUsageAction.LEAK_DETECTOR_RETURNED, FeatureUsageEventType.USER_ACTION, start.plusDays(2));

        String response = mockMvc.perform(get("/api/admin/feature-funnels")
                        .param("funnel", "LEAK_DETECTOR")
                        .param("range", "LAST_30_DAYS")
                        .header("Authorization", "Bearer " + jwtService.generateToken(admin)))
                .andExpect(status().isOk())
                .andReturn()
                .getResponse()
                .getContentAsString();

        JsonNode json = objectMapper.readTree(response);
        JsonNode summary = json.get("funnelSummaries").get(0);
        assertThat(summary.get("funnelKey").asText()).isEqualTo("LEAK_DETECTOR");
        assertThat(summary.get("enteredUsers").asLong()).isGreaterThanOrEqualTo(1);
        assertThat(summary.get("completedUsers").asLong()).isGreaterThanOrEqualTo(1);
        assertThat(summary.get("steps").get(0).get("passive").asBoolean()).isTrue();
        assertThat(json.get("conversionAssociations").get(0).get("caveat").asText()).contains("does not establish causation");

        mockMvc.perform(get("/api/admin/feature-funnels")
                        .header("Authorization", "Bearer " + jwtService.generateToken(pro)))
                .andExpect(status().isForbidden());
    }

    @Test
    void outOfOrderEventsDoNotCompleteFunnel() throws Exception {
        User admin = createUser("admin-funnel-order@tradepilot.local", UserRole.ADMIN, PlanType.PRO);
        User pro = createUser("pro-funnel-order@trader.local", UserRole.USER, PlanType.PRO);
        LocalDateTime start = LocalDateTime.now().minusDays(2);
        save(pro, FeatureKey.AI_LEAK_DETECTOR, FeatureUsageAction.LEAK_DETECTOR_GENERATED, FeatureUsageEventType.AI_GENERATION, start);
        save(pro, FeatureKey.AI_LEAK_DETECTOR, FeatureUsageAction.LEAK_DETECTOR_VIEWED, FeatureUsageEventType.PAGE_VIEW, start.plusHours(1));

        String response = mockMvc.perform(get("/api/admin/feature-funnels/LEAK_DETECTOR")
                        .param("range", "LAST_30_DAYS")
                        .header("Authorization", "Bearer " + jwtService.generateToken(admin)))
                .andExpect(status().isOk())
                .andReturn()
                .getResponse()
                .getContentAsString();

        JsonNode json = objectMapper.readTree(response);
        assertThat(json.get("enteredUsers").asLong()).isGreaterThanOrEqualTo(1);
        assertThat(json.get("completedUsers").asLong()).isEqualTo(0);
    }

    @Test
    void historicalEventsWithoutActionProduceDataQualityWarningAndMaskedDrilldown() throws Exception {
        User admin = createUser("admin-funnel-quality@tradepilot.local", UserRole.ADMIN, PlanType.PRO);
        User pro = createUser("quality-funnel@trader.local", UserRole.USER, PlanType.PRO);
        featureUsageRepository.save(FeatureUsageEvent.builder()
                .userId(pro.getId())
                .feature(FeatureKey.PLAYBOOK)
                .createdAt(LocalDateTime.now().minusDays(1))
                .build());

        String dashboard = mockMvc.perform(get("/api/admin/feature-funnels")
                        .param("funnel", "PLAYBOOK_ACTIVATION")
                        .param("range", "LAST_30_DAYS")
                        .header("Authorization", "Bearer " + jwtService.generateToken(admin)))
                .andExpect(status().isOk())
                .andReturn()
                .getResponse()
                .getContentAsString();

        JsonNode dashboardJson = objectMapper.readTree(dashboard);
        assertThat(dashboardJson.get("dataQualityWarnings").get(0).asText()).contains("missing action metadata");

        String users = mockMvc.perform(get("/api/admin/feature-funnels/PLAYBOOK_ACTIVATION/users")
                        .param("range", "LAST_30_DAYS")
                        .header("Authorization", "Bearer " + jwtService.generateToken(admin)))
                .andExpect(status().isOk())
                .andReturn()
                .getResponse()
                .getContentAsString();
        JsonNode usersJson = objectMapper.readTree(users);
        assertThat(usersJson.get(0).get("maskedEmail").asText()).contains("***@trader.local");
        assertThat(usersJson.get(0).has("email")).isFalse();
    }

    private User createUser(String email, UserRole role, PlanType plan) {
        return userRepository.save(User.builder()
                .name(email)
                .email(email)
                .passwordHash(passwordEncoder.encode("password123"))
                .emailVerified(true)
                .role(role)
                .plan(plan)
                .subscriptionStatus(plan == PlanType.PRO ? SubscriptionStatus.ACTIVE : SubscriptionStatus.NONE)
                .build());
    }

    private void save(User user, FeatureKey feature, FeatureUsageAction action, FeatureUsageEventType eventType, LocalDateTime occurredAt) {
        featureUsageRepository.save(FeatureUsageEvent.builder()
                .userId(user.getId())
                .feature(feature)
                .action(action)
                .eventType(eventType)
                .occurredAt(occurredAt)
                .createdAt(occurredAt)
                .planAtEvent(user.getPlan().name())
                .build());
    }
}
