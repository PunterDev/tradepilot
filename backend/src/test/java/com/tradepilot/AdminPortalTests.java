package com.tradepilot;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.tradepilot.billing.PlanType;
import com.tradepilot.billing.SubscriptionStatus;
import com.tradepilot.dto.admin.AdminPlanChangeRequest;
import com.tradepilot.entity.ContactMessage;
import com.tradepilot.entity.User;
import com.tradepilot.enums.AdminAuditAction;
import com.tradepilot.enums.ContactMessageStatus;
import com.tradepilot.enums.UserRole;
import com.tradepilot.featureusage.FeatureKey;
import com.tradepilot.featureusage.FeatureUsageEvent;
import com.tradepilot.featureusage.FeatureUsageRepository;
import com.tradepilot.observability.CacheMetric;
import com.tradepilot.observability.CacheMetricRepository;
import com.tradepilot.observability.EmailDeliveryEvent;
import com.tradepilot.observability.EmailDeliveryEventRepository;
import com.tradepilot.observability.ObservabilityAlertService;
import com.tradepilot.observability.ObservabilityCleanupService;
import com.tradepilot.observability.RequestMetric;
import com.tradepilot.observability.RequestMetricRepository;
import com.tradepilot.observability.StripeEventMetric;
import com.tradepilot.observability.StripeEventMetricRepository;
import com.tradepilot.observability.SystemAlert;
import com.tradepilot.observability.SystemAlertRepository;
import com.tradepilot.repository.AdminAuditLogRepository;
import com.tradepilot.repository.ContactMessageRepository;
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
import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class AdminPortalTests {
    @Autowired MockMvc mockMvc;
    @Autowired ObjectMapper objectMapper;
    @Autowired UserRepository userRepository;
    @Autowired AdminAuditLogRepository auditLogRepository;
    @Autowired ContactMessageRepository contactMessageRepository;
    @Autowired FeatureUsageRepository featureUsageRepository;
    @Autowired RequestMetricRepository requestMetricRepository;
    @Autowired CacheMetricRepository cacheMetricRepository;
    @Autowired EmailDeliveryEventRepository emailDeliveryEventRepository;
    @Autowired StripeEventMetricRepository stripeEventMetricRepository;
    @Autowired SystemAlertRepository systemAlertRepository;
    @Autowired ObservabilityAlertService observabilityAlertService;
    @Autowired ObservabilityCleanupService observabilityCleanupService;
    @Autowired com.tradepilot.service.AggregateCacheService aggregateCacheService;
    @Autowired PasswordEncoder passwordEncoder;

    @Test
    void adminCanListUsersAndUserCannotListUsers() throws Exception {
        User admin = createUser("admin-list@example.com", UserRole.ADMIN, PlanType.PRO);
        createUser("normal-list@example.com", UserRole.USER, PlanType.FREE);

        mockMvc.perform(get("/api/admin/users")
                        .header("Authorization", "Bearer " + tokenFor(admin)))
                .andExpect(status().isOk());

        User normalUser = userRepository.findByEmail("normal-list@example.com").orElseThrow();
        mockMvc.perform(get("/api/admin/users")
                        .header("Authorization", "Bearer " + tokenFor(normalUser)))
                .andExpect(status().isForbidden());
    }

    @Test
    void adminCanViewDashboardSummaryAndUserCannot() throws Exception {
        User admin = createUser("admin-dashboard@example.com", UserRole.ADMIN, PlanType.PRO);
        User normal = createUser("normal-dashboard@example.com", UserRole.USER, PlanType.FREE);
        contactMessageRepository.save(ContactMessage.builder()
                .name("Dashboard Lead")
                .email("dashboard-lead@example.com")
                .subject("Pricing")
                .message("Tell me more about Pro.")
                .status(ContactMessageStatus.NEW)
                .emailSent(false)
                .build());

        String response = mockMvc.perform(get("/api/admin/dashboard/summary")
                        .header("Authorization", "Bearer " + tokenFor(admin)))
                .andExpect(status().isOk())
                .andReturn()
                .getResponse()
                .getContentAsString();

        JsonNode json = objectMapper.readTree(response);
        assertThat(json.get("totalUsers").asLong()).isGreaterThanOrEqualTo(2);
        assertThat(json.get("proUsers").asLong()).isGreaterThanOrEqualTo(1);
        assertThat(json.get("newContactMessages").asLong()).isGreaterThanOrEqualTo(1);
        assertThat(json.get("recentUsers").isArray()).isTrue();
        assertThat(json.get("recentContactMessages").isArray()).isTrue();

        mockMvc.perform(get("/api/admin/dashboard/summary")
                        .header("Authorization", "Bearer " + tokenFor(normal)))
                .andExpect(status().isForbidden());
    }

    @Test
    void adminCanViewBillingSummaryAndUserCannot() throws Exception {
        User admin = createUser("admin-billing@example.com", UserRole.ADMIN, PlanType.PRO);
        User normal = createUser("normal-billing@example.com", UserRole.USER, PlanType.FREE);
        aggregateCacheService.evict("admin-billing-summary");

        long expectedTotalUsers = userRepository.count();
        long expectedProUsers = userRepository.countByPlan(PlanType.PRO);
        long expectedStripeSubscribers = userRepository.countByPlanAndSubscriptionStatusInAndStripeSubscriptionIdIsNotNull(
                PlanType.PRO,
                List.of(SubscriptionStatus.ACTIVE, SubscriptionStatus.TRIALING)
        );
        long expectedManualProUsers = Math.max(0, expectedProUsers - expectedStripeSubscribers);

        String response = mockMvc.perform(get("/api/admin/billing/summary")
                        .header("Authorization", "Bearer " + tokenFor(admin)))
                .andExpect(status().isOk())
                .andReturn()
                .getResponse()
                .getContentAsString();

        JsonNode json = objectMapper.readTree(response);
        assertThat(json.get("totalUsers").asLong()).isEqualTo(expectedTotalUsers);
        assertThat(json.get("proUsers").asLong()).isEqualTo(expectedProUsers);
        assertThat(json.get("stripePayingSubscribers").asLong()).isEqualTo(expectedStripeSubscribers);
        assertThat(json.get("manualProUsers").asLong()).isEqualTo(expectedManualProUsers);
        assertThat(json.get("estimatedMrr").decimalValue())
                .isEqualByComparingTo(BigDecimal.valueOf(expectedStripeSubscribers).multiply(new BigDecimal("29.99")));
        assertThat(json.get("potentialMrr").decimalValue())
                .isEqualByComparingTo(BigDecimal.valueOf(expectedProUsers).multiply(new BigDecimal("29.99")));
        assertThat(json.get("recentPlanChanges").isArray()).isTrue();

        mockMvc.perform(get("/api/admin/billing/summary")
                        .header("Authorization", "Bearer " + tokenFor(normal)))
                .andExpect(status().isForbidden());
    }

    @Test
    void adminCanViewFeatureUsageAndUserCannot() throws Exception {
        featureUsageRepository.deleteAll();
        User admin = createUser("admin-feature@example.com", UserRole.ADMIN, PlanType.PRO);
        User normal = createUser("normal-feature@example.com", UserRole.USER, PlanType.FREE);
        featureUsageRepository.save(FeatureUsageEvent.builder()
                .userId(normal.getId())
                .feature(FeatureKey.AI_REVIEW)
                .build());
        featureUsageRepository.save(FeatureUsageEvent.builder()
                .userId(normal.getId())
                .feature(FeatureKey.AI_REVIEW)
                .build());
        featureUsageRepository.save(FeatureUsageEvent.builder()
                .userId(normal.getId())
                .feature(FeatureKey.PERFORMANCE_TIMELINE)
                .build());
        featureUsageRepository.save(FeatureUsageEvent.builder()
                .userId(normal.getId())
                .feature(FeatureKey.PLAYBOOK)
                .build());

        String response = mockMvc.perform(get("/api/admin/feature-usage/summary")
                        .header("Authorization", "Bearer " + tokenFor(admin)))
                .andExpect(status().isOk())
                .andReturn()
                .getResponse()
                .getContentAsString();

        JsonNode json = objectMapper.readTree(response);
        assertThat(json.get("activeUsers30d").asLong()).isGreaterThanOrEqualTo(1);
        assertThat(json.get("features").isArray()).isTrue();
        assertThat(json.get("mostUsedFeature").asText()).isEqualTo("AI_REVIEW");
        JsonNode aiReviewFeature = null;
        for (JsonNode feature : json.get("features")) {
            if ("AI_REVIEW".equals(feature.get("feature").asText())) {
                aiReviewFeature = feature;
                break;
            }
        }
        assertThat(aiReviewFeature).isNotNull();
        assertThat(aiReviewFeature.get("displayName").asText()).isEqualTo("AI Trade Review");
        assertThat(aiReviewFeature.get("overallAdoptionRate").asDouble()).isGreaterThan(0);
        assertThat(aiReviewFeature.get("activeUserAdoptionRate").asDouble()).isEqualTo(100.0);
        assertFeatureHasDisplayName(json, FeatureKey.PERFORMANCE_TIMELINE, "AI Performance Timeline");
        assertFeatureHasDisplayName(json, FeatureKey.PLAYBOOK, "AI Trading Playbook");

        mockMvc.perform(get("/api/admin/feature-usage/summary")
                        .header("Authorization", "Bearer " + tokenFor(normal)))
                .andExpect(status().isForbidden());
    }

    @Test
    void adminCanViewRetentionAnalyticsAndUserCannot() throws Exception {
        User admin = createUser("admin-retention@example.com", UserRole.ADMIN, PlanType.PRO);
        User activePro = createUser("active-retention@example.com", UserRole.USER, PlanType.PRO);
        User inactiveFree = createUser("inactive-retention@example.com", UserRole.USER, PlanType.FREE);
        inactiveFree.setCreatedAt(LocalDateTime.now().minusDays(35));
        userRepository.save(inactiveFree);

        for (int i = 0; i < 10; i++) {
            featureUsageRepository.save(FeatureUsageEvent.builder()
                    .userId(activePro.getId())
                    .feature(i % 2 == 0 ? FeatureKey.AI_REVIEW : FeatureKey.DAILY_COACH)
                    .createdAt(LocalDateTime.now().minusHours(i))
                    .build());
        }

        mockMvc.perform(get("/api/admin/retention/summary")
                        .header("Authorization", "Bearer " + tokenFor(inactiveFree)))
                .andExpect(status().isForbidden());

        String response = mockMvc.perform(get("/api/admin/retention/summary")
                        .header("Authorization", "Bearer " + tokenFor(admin)))
                .andExpect(status().isOk())
                .andReturn()
                .getResponse()
                .getContentAsString();

        JsonNode json = objectMapper.readTree(response);
        assertThat(json.get("dau").asLong()).isGreaterThanOrEqualTo(1);
        assertThat(json.get("wau").asLong()).isGreaterThanOrEqualTo(1);
        assertThat(json.get("mau").asLong()).isGreaterThanOrEqualTo(1);
        assertThat(json.get("powerUsers").asLong()).isGreaterThanOrEqualTo(1);
        assertThat(json.get("proUsers").get("mau").asLong()).isGreaterThanOrEqualTo(1);

        mockMvc.perform(get("/api/admin/retention/trends")
                        .header("Authorization", "Bearer " + tokenFor(admin)))
                .andExpect(status().isOk());
        mockMvc.perform(get("/api/admin/retention/cohorts")
                        .header("Authorization", "Bearer " + tokenFor(admin)))
                .andExpect(status().isOk());
        String usersResponse = mockMvc.perform(get("/api/admin/retention/users")
                        .header("Authorization", "Bearer " + tokenFor(admin)))
                .andExpect(status().isOk())
                .andReturn()
                .getResponse()
                .getContentAsString();
        JsonNode usersJson = objectMapper.readTree(usersResponse);
        assertThat(usersJson.isArray()).isTrue();
        boolean activeUserPresent = false;
        for (JsonNode userNode : usersJson) {
            if (activePro.getId().equals(userNode.get("userId").asLong())) {
                activeUserPresent = true;
                assertThat(userNode.get("email").asText()).isEqualTo("active-retention@example.com");
                assertThat(userNode.get("plan").asText()).isEqualTo("PRO");
                assertThat(userNode.get("activityCount30d").asLong()).isEqualTo(10);
                assertThat(userNode.get("status").asText()).isEqualTo("POWER_USER");
            }
        }
        assertThat(activeUserPresent).isTrue();
    }

    @Test
    void observabilityCreatesDedupesAndResolvesAlerts() throws Exception {
        User admin = createUser("admin-observability@example.com", UserRole.ADMIN, PlanType.PRO);
        LocalDateTime now = LocalDateTime.now();

        for (int i = 0; i < 20; i++) {
            requestMetricRepository.save(RequestMetric.builder()
                    .endpoint("/api/test")
                    .method("GET")
                    .status(i < 2 ? 500 : 200)
                    .durationMs(i >= 18 ? 2_500L : 100L)
                    .createdAt(now.minusMinutes(1))
                    .build());
        }
        for (int i = 0; i < 21; i++) {
            cacheMetricRepository.save(CacheMetric.builder()
                    .cacheName("test-cache")
                    .hit(i < 5)
                    .createdAt(now.minusMinutes(1))
                    .build());
        }
        for (int i = 0; i < 4; i++) {
            emailDeliveryEventRepository.save(EmailDeliveryEvent.builder()
                    .recipient("ops@example.com")
                    .template("test")
                    .success(false)
                    .durationMs(10L)
                    .errorMessage("failed")
                    .createdAt(now.minusMinutes(1))
                    .build());
            stripeEventMetricRepository.save(StripeEventMetric.builder()
                    .eventType("test")
                    .success(false)
                    .durationMs(10L)
                    .errorMessage("failed")
                    .createdAt(now.minusMinutes(1))
                    .build());
        }

        observabilityAlertService.evaluateAlerts();
        observabilityAlertService.evaluateAlerts();

        assertThat(systemAlertRepository.findByStatusOrderByCreatedAtDesc("OPEN", org.springframework.data.domain.PageRequest.of(0, 20)))
                .extracting("type")
                .contains("API_ERROR_RATE_HIGH", "API_P95_LATENCY_HIGH", "CACHE_HIT_RATE_LOW", "EMAIL_FAILURES_HIGH", "STRIPE_FAILURES_HIGH");
        assertThat(systemAlertRepository.findByStatusOrderByCreatedAtDesc("OPEN", org.springframework.data.domain.PageRequest.of(0, 20)))
                .filteredOn(alert -> "API_ERROR_RATE_HIGH".equals(alert.getType()))
                .hasSize(1);

        SystemAlert alert = systemAlertRepository.findByStatusOrderByCreatedAtDesc("OPEN", org.springframework.data.domain.PageRequest.of(0, 1)).get(0);
        mockMvc.perform(patch("/api/admin/observability/alerts/{id}/resolve", alert.getId())
                        .header("Authorization", "Bearer " + tokenFor(admin)))
                .andExpect(status().isOk());

        SystemAlert resolved = systemAlertRepository.findById(alert.getId()).orElseThrow();
        assertThat(resolved.getStatus()).isEqualTo("RESOLVED");
        assertThat(resolved.getResolved()).isTrue();
        assertThat(resolved.getResolvedAt()).isNotNull();
        assertThat(resolved.getResolvedBy()).isEqualTo(admin.getId());
    }

    @Test
    void observabilityDateFiltersAndCleanupWork() throws Exception {
        User admin = createUser("admin-observability-filter@example.com", UserRole.ADMIN, PlanType.PRO);
        requestMetricRepository.save(RequestMetric.builder()
                .endpoint("/api/today")
                .method("GET")
                .status(200)
                .durationMs(100L)
                .createdAt(LocalDateTime.now().minusHours(1))
                .build());
        requestMetricRepository.save(RequestMetric.builder()
                .endpoint("/api/old")
                .method("GET")
                .status(200)
                .durationMs(100L)
                .createdAt(LocalDateTime.now().minusDays(75))
                .build());

        String response = mockMvc.perform(get("/api/admin/observability/summary")
                        .param("range", "TODAY")
                        .header("Authorization", "Bearer " + tokenFor(admin)))
                .andExpect(status().isOk())
                .andReturn()
                .getResponse()
                .getContentAsString();

        JsonNode json = objectMapper.readTree(response);
        assertThat(json.get("requestsToday").asLong()).isGreaterThanOrEqualTo(1);

        ObservabilityCleanupService.CleanupCounts counts = observabilityCleanupService.cleanupNow(LocalDateTime.now());
        assertThat(counts.apiRequestMetrics()).isGreaterThanOrEqualTo(1);
        assertThat(requestMetricRepository.findAll())
                .extracting(RequestMetric::getEndpoint)
                .doesNotContain("/api/old");
    }

    @Test
    void observabilitySummaryShowsNoEventStatesAndDiagnosticBreakdowns() throws Exception {
        User admin = createUser("admin-observability-diagnostics@example.com", UserRole.ADMIN, PlanType.PRO);
        LocalDateTime sampleTime = LocalDateTime.of(2031, 1, 7, 10, 0);
        requestMetricRepository.save(RequestMetric.builder()
                .endpoint("/api/accounts/{id}/risk")
                .method("PATCH")
                .status(400)
                .durationMs(180L)
                .requestId("req-400")
                .errorCode("BAD_REQUEST")
                .createdAt(sampleTime)
                .build());
        requestMetricRepository.save(RequestMetric.builder()
                .endpoint("/api/accounts/{id}/risk")
                .method("PATCH")
                .status(500)
                .durationMs(2400L)
                .requestId("req-500")
                .errorCode("SERVER_ERROR")
                .createdAt(sampleTime.plusMinutes(1))
                .build());
        requestMetricRepository.save(RequestMetric.builder()
                .endpoint("/api/ai/leak-detector")
                .method("GET")
                .status(200)
                .durationMs(31000L)
                .requestId("req-ai")
                .createdAt(sampleTime.plusMinutes(2))
                .build());

        String response = mockMvc.perform(get("/api/v1/admin/observability/summary")
                        .param("range", "CUSTOM")
                        .param("startDate", "2031-01-07")
                        .param("endDate", "2031-01-07")
                        .header("Authorization", "Bearer " + tokenFor(admin)))
                .andExpect(status().isOk())
                .andReturn()
                .getResponse()
                .getContentAsString();

        JsonNode json = objectMapper.readTree(response);
        assertThat(json.get("emailHasAttempts").asBoolean()).isFalse();
        assertThat(json.get("emailSuccessRateDisplay").asText()).isEqualTo("N/A");
        assertThat(json.get("stripeHasEvents").asBoolean()).isFalse();
        assertThat(json.get("stripeSuccessRateDisplay").asText()).isEqualTo("N/A");
        assertThat(json.get("errorsByStatus")).anyMatch(node -> node.get("status").asInt() == 400 && node.get("count").asLong() == 1);
        assertThat(json.get("errorsByStatus")).anyMatch(node -> node.get("status").asInt() == 500 && node.get("count").asLong() == 1);
        assertThat(json.get("errorsByEndpoint").get(0).get("latestRequestId").asText()).isEqualTo("req-500");
        assertThat(json.get("slowestEndpoints")).anyMatch(node ->
                "/api/ai/leak-detector".equals(node.get("endpoint").asText())
                        && "AI Generation".equals(node.get("category").asText())
                        && node.get("requestCount").asLong() == 1
                        && node.get("maxDurationMs").asLong() == 31000L);
        assertThat(json.get("lastApiFailure").get("requestId").asText()).isEqualTo("req-500");
    }

    @Test
    void adminUpgradeAndDowngradeChangePlanAndCreateAuditLogs() throws Exception {
        User admin = createUser("admin-plan@example.com", UserRole.ADMIN, PlanType.PRO);
        User target = createUser("target-plan@example.com", UserRole.USER, PlanType.FREE);

        AdminPlanChangeRequest upgrade = new AdminPlanChangeRequest("Manual beta upgrade", null);
        mockMvc.perform(post("/api/admin/users/{userId}/upgrade-pro", target.getId())
                        .header("Authorization", "Bearer " + tokenFor(admin))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(upgrade)))
                .andExpect(status().isOk());

        User upgraded = userRepository.findById(target.getId()).orElseThrow();
        assertThat(upgraded.getPlan()).isEqualTo(PlanType.PRO);
        assertThat(upgraded.getSubscriptionStatus()).isEqualTo(SubscriptionStatus.ACTIVE);

        AdminPlanChangeRequest downgrade = new AdminPlanChangeRequest("Manual downgrade", null);
        mockMvc.perform(post("/api/admin/users/{userId}/downgrade-free", target.getId())
                        .header("Authorization", "Bearer " + tokenFor(admin))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(downgrade)))
                .andExpect(status().isOk());

        User downgraded = userRepository.findById(target.getId()).orElseThrow();
        assertThat(downgraded.getPlan()).isEqualTo(PlanType.FREE);
        assertThat(downgraded.getSubscriptionStatus()).isEqualTo(SubscriptionStatus.NONE);
        assertThat(auditLogRepository.findByTargetUserIdOrderByCreatedAtDesc(target.getId()))
                .extracting("action")
                .contains(AdminAuditAction.UPGRADE_TO_PRO, AdminAuditAction.DOWNGRADE_TO_FREE);
    }

    @Test
    void adminCanMakeAndRemoveAdminAndCreateAuditLogs() throws Exception {
        User admin = createUser("admin-role@example.com", UserRole.ADMIN, PlanType.PRO);
        User target = createUser("target-role@example.com", UserRole.USER, PlanType.FREE);

        mockMvc.perform(post("/api/admin/users/{userId}/make-admin", target.getId())
                        .header("Authorization", "Bearer " + tokenFor(admin))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "reason": "Trusted operator"
                                }
                                """))
                .andExpect(status().isOk());

        assertThat(userRepository.findById(target.getId()).orElseThrow().getRole())
                .isEqualTo(UserRole.ADMIN);

        mockMvc.perform(post("/api/admin/users/{userId}/remove-admin", target.getId())
                        .header("Authorization", "Bearer " + tokenFor(admin))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "reason": "Access no longer required"
                                }
                                """))
                .andExpect(status().isOk());

        assertThat(userRepository.findById(target.getId()).orElseThrow().getRole())
                .isEqualTo(UserRole.USER);
        assertThat(auditLogRepository.findByTargetUserIdOrderByCreatedAtDesc(target.getId()))
                .extracting("action")
                .contains(AdminAuditAction.ROLE_CHANGE);
    }

    @Test
    void publicContactSubmissionSavesMessageEvenWhenEmailIsNotSent() throws Exception {
        String body = """
                {
                  "name": "Contact Tester",
                  "email": "contact-tester@example.com",
                  "subject": "Question about Pro",
                  "message": "Can I use this for NQ?"
                }
                """;

        mockMvc.perform(post("/api/contact")
                        .contentType(MediaType.APPLICATION_JSON)
                        .header("User-Agent", "JUnit")
                        .content(body))
                .andExpect(status().isOk());

        ContactMessage saved = contactMessageRepository.findAllByOrderByCreatedAtDesc().get(0);
        for (int i = 0; i < 10 && saved.getEmailError() == null; i++) {
            Thread.sleep(100);
            saved = contactMessageRepository.findById(saved.getId()).orElseThrow();
        }
        assertThat(saved.getEmail()).isEqualTo("contact-tester@example.com");
        assertThat(saved.getStatus()).isEqualTo(ContactMessageStatus.NEW);
        assertThat(saved.isEmailSent()).isFalse();
        assertThat(saved.getEmailError()).contains("Resend");
        assertThat(saved.getUserAgent()).isEqualTo("JUnit");
    }

    @Test
    void adminCanListAndUpdateContactMessagesButNormalUserCannot() throws Exception {
        User admin = createUser("admin-contact@example.com", UserRole.ADMIN, PlanType.PRO);
        User normal = createUser("normal-contact@example.com", UserRole.USER, PlanType.FREE);
        ContactMessage message = contactMessageRepository.save(ContactMessage.builder()
                .name("Lead")
                .email("lead@example.com")
                .subject("Hello")
                .message("Interested")
                .status(ContactMessageStatus.NEW)
                .build());

        mockMvc.perform(get("/api/admin/contact-messages")
                        .header("Authorization", "Bearer " + tokenFor(normal)))
                .andExpect(status().isForbidden());

        mockMvc.perform(get("/api/admin/contact-messages")
                        .header("Authorization", "Bearer " + tokenFor(admin)))
                .andExpect(status().isOk());

        mockMvc.perform(patch("/api/admin/contact-messages/{id}/status", message.getId())
                        .header("Authorization", "Bearer " + tokenFor(admin))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "status": "OPEN"
                                }
                                """))
                .andExpect(status().isOk());

        assertThat(contactMessageRepository.findById(message.getId()).orElseThrow().getStatus())
                .isEqualTo(ContactMessageStatus.OPEN);

        mockMvc.perform(patch("/api/admin/contact-messages/{id}/notes", message.getId())
                        .header("Authorization", "Bearer " + tokenFor(admin))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "adminNotes": "Potential Pro lead."
                                }
                                """))
                .andExpect(status().isOk());

        assertThat(contactMessageRepository.findById(message.getId()).orElseThrow().getAdminNotes())
                .isEqualTo("Potential Pro lead.");

        ContactMessage newMessage = contactMessageRepository.save(ContactMessage.builder()
                .name("Open Me")
                .email("open@example.com")
                .subject("Open workflow")
                .message("Please open")
                .status(ContactMessageStatus.NEW)
                .build());

        mockMvc.perform(get("/api/admin/contact-messages/{id}", newMessage.getId())
                        .header("Authorization", "Bearer " + tokenFor(admin)))
                .andExpect(status().isOk());

        ContactMessage opened = contactMessageRepository.findById(newMessage.getId()).orElseThrow();
        assertThat(opened.getStatus()).isEqualTo(ContactMessageStatus.OPEN);
        assertThat(opened.getLastViewedAt()).isNotNull();

        mockMvc.perform(post("/api/admin/contact-messages/{id}/reply", opened.getId())
                        .header("Authorization", "Bearer " + tokenFor(admin))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "subject": "Re: Open workflow",
                                  "message": "Thanks for reaching out."
                                }
                                """))
                .andExpect(status().isBadRequest());

        ContactMessage failedReply = contactMessageRepository.findById(opened.getId()).orElseThrow();
        assertThat(failedReply.getStatus()).isEqualTo(ContactMessageStatus.OPEN);
        assertThat(failedReply.getReplySubject()).isEqualTo("Re: Open workflow");
        assertThat(failedReply.getReplyBody()).isEqualTo("Thanks for reaching out.");
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

        JsonNode json = objectMapper.readTree(response);
        return json.get("token").asText();
    }

    private void assertFeatureHasDisplayName(JsonNode summary, FeatureKey featureKey, String expectedDisplayName) {
        JsonNode features = summary.get("features");
        assertThat(features.isArray()).isTrue();
        for (JsonNode feature : features) {
            if (featureKey.name().equals(feature.get("feature").asText())) {
                assertThat(feature.get("displayName").asText()).isEqualTo(expectedDisplayName);
                assertThat(feature.get("displayName").asText()).isNotBlank();
                return;
            }
        }
        throw new AssertionError("Feature not found in summary: " + featureKey);
    }
}
