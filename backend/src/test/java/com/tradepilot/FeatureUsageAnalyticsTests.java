package com.tradepilot;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.tradepilot.billing.PlanType;
import com.tradepilot.billing.SubscriptionStatus;
import com.tradepilot.entity.User;
import com.tradepilot.enums.UserRole;
import com.tradepilot.featureusage.FeatureKey;
import com.tradepilot.featureusage.FeatureUsageAction;
import com.tradepilot.featureusage.FeatureUsageEvent;
import com.tradepilot.featureusage.FeatureUsageEventType;
import com.tradepilot.featureusage.FeatureUsageRepository;
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
class FeatureUsageAnalyticsTests {
    @Autowired MockMvc mockMvc;
    @Autowired ObjectMapper objectMapper;
    @Autowired UserRepository userRepository;
    @Autowired FeatureUsageRepository featureUsageRepository;
    @Autowired PasswordEncoder passwordEncoder;
    @Autowired JwtService jwtService;

    @Test
    void summarySeparatesActivePassiveAiPreviousPeriodAndDistinctDateRepeatUsers() throws Exception {
        User admin = createUser("admin-feature-usage-analytics@tradepilot.local", UserRole.ADMIN, PlanType.PRO);
        User userOne = createUser("feature-usage-one@trader.local", UserRole.USER, PlanType.PRO);
        User userTwo = createUser("feature-usage-two@trader.local", UserRole.USER, PlanType.PRO);
        User userThree = createUser("feature-usage-three@trader.local", UserRole.USER, PlanType.FREE);
        LocalDateTime currentStart = LocalDateTime.of(2026, 6, 10, 9, 30);
        LocalDateTime previousStart = LocalDateTime.of(2026, 6, 7, 9, 30);

        save(userOne, FeatureKey.AI_REVIEW, FeatureUsageAction.TRADE_REVIEW_GENERATED, FeatureUsageEventType.AI_GENERATION, currentStart);
        save(userOne, FeatureKey.AI_REVIEW, FeatureUsageAction.TRADE_REVIEW_ACCEPTED, FeatureUsageEventType.USER_ACTION, currentStart.plusHours(1));
        save(userTwo, FeatureKey.AI_REVIEW, FeatureUsageAction.TRADE_REVIEW_GENERATED, FeatureUsageEventType.AI_GENERATION, currentStart.plusDays(1));
        save(userTwo, FeatureKey.AI_REVIEW, FeatureUsageAction.TRADE_REVIEW_ACCEPTED, FeatureUsageEventType.USER_ACTION, currentStart.plusDays(2));
        save(userThree, FeatureKey.ANALYTICS_VIEW, FeatureUsageAction.PAGE_VIEWED, FeatureUsageEventType.PAGE_VIEW, currentStart.plusHours(2));
        save(admin, FeatureKey.AI_REVIEW, FeatureUsageAction.TRADE_REVIEW_GENERATED, FeatureUsageEventType.AI_GENERATION, currentStart.plusHours(3));

        save(userOne, FeatureKey.AI_REVIEW, FeatureUsageAction.TRADE_REVIEW_GENERATED, FeatureUsageEventType.AI_GENERATION, previousStart);
        save(userTwo, FeatureKey.ANALYTICS_VIEW, FeatureUsageAction.PAGE_VIEWED, FeatureUsageEventType.PAGE_VIEW, previousStart.plusDays(1));

        JsonNode json = objectMapper.readTree(mockMvc.perform(get("/api/admin/feature-usage/summary")
                        .param("range", "CUSTOM")
                        .param("startDate", "2026-06-10")
                        .param("endDate", "2026-06-12")
                        .header("Authorization", "Bearer " + jwtService.generateToken(admin)))
                .andExpect(status().isOk())
                .andReturn()
                .getResponse()
                .getContentAsString());

        assertThat(json.get("activeUsers").asLong()).isEqualTo(3);
        assertThat(json.get("activeActions").asLong()).isEqualTo(4);
        assertThat(json.get("passiveViews").asLong()).isEqualTo(1);
        assertThat(json.get("aiGenerations").asLong()).isEqualTo(2);
        assertThat(json.get("highestActiveAdoptionFeature").asText()).isEqualTo("AI_REVIEW");
        assertThat(json.get("activeUsersComparison").get("previousValue").asLong()).isEqualTo(2);
        assertThat(json.get("activeActionsComparison").get("direction").asText()).isEqualTo("UP");

        JsonNode aiReview = findFeature(json, "AI_REVIEW");
        assertThat(aiReview.get("uniqueUsers").asLong()).isEqualTo(2);
        assertThat(aiReview.get("repeatUsers").asLong()).isEqualTo(1);
        assertThat(aiReview.get("repeatUsageRate").asDouble()).isEqualTo(50.0);
        assertThat(aiReview.get("aiGenerationCount").asLong()).isEqualTo(2);
        assertThat(aiReview.get("previousPeriodActiveEvents").asLong()).isEqualTo(1);
        assertThat(aiReview.get("adoptionDeltaPercentagePoints").asDouble()).isEqualTo(16.7);

        JsonNode analytics = findFeature(json, "ANALYTICS_VIEW");
        assertThat(analytics.get("passiveEventCount").asLong()).isEqualTo(1);
        assertThat(analytics.get("activeEventCount").asLong()).isEqualTo(0);
    }

    @Test
    void nonAdminCannotReadFeatureUsageSummary() throws Exception {
        User user = createUser("feature-usage-non-admin@trader.local", UserRole.USER, PlanType.PRO);

        mockMvc.perform(get("/api/admin/feature-usage/summary")
                        .header("Authorization", "Bearer " + jwtService.generateToken(user)))
                .andExpect(status().isForbidden());
    }

    private JsonNode findFeature(JsonNode json, String feature) {
        for (JsonNode item : json.get("features")) {
            if (feature.equals(item.get("feature").asText())) {
                return item;
            }
        }
        throw new AssertionError("Missing feature " + feature);
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
