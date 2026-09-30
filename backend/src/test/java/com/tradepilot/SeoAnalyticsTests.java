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
import com.tradepilot.seoanalytics.SeoAcquisitionEventRepository;
import com.tradepilot.seoanalytics.SeoAnalyticsService;
import com.tradepilot.seoanalytics.UserAcquisitionAttributionRepository;
import com.tradepilot.seoanalytics.dto.SeoAttributionRequest;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import java.sql.Timestamp;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class SeoAnalyticsTests {
    @Autowired MockMvc mockMvc;
    @Autowired ObjectMapper objectMapper;
    @Autowired UserRepository userRepository;
    @Autowired FeatureUsageRepository featureUsageRepository;
    @Autowired SeoAcquisitionEventRepository eventRepository;
    @Autowired UserAcquisitionAttributionRepository attributionRepository;
    @Autowired PasswordEncoder passwordEncoder;
    @Autowired JwtService jwtService;
    @Autowired SeoAnalyticsService seoAnalyticsService;
    @Autowired NamedParameterJdbcTemplate jdbcTemplate;

    @Test
    void anonymousSeoEventsCanBeRecordedWithoutAuthentication() throws Exception {
        String body = """
                {"events":[{"eventId":"11111111-1111-4111-8111-111111111111","event":"seo_landing_page_view","anonymousId":"anon-seo-1","landingPage":"/futures-trading-journal","utmSource":"google","utmMedium":"organic","timestamp":"2001-01-01T00:00:00Z"}]}
                """;

        LocalDateTime before = LocalDateTime.now().minusSeconds(1);
        mockMvc.perform(post("/api/v1/seo/events")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isNoContent());

        var saved = eventRepository.findAll().stream()
                .filter(event -> "11111111-1111-4111-8111-111111111111".equals(event.getEventId()))
                .findFirst()
                .orElseThrow();
        assertThat(saved.getEventName()).isEqualTo("seo_landing_page_view");
        assertThat(saved.getOccurredAt()).isAfterOrEqualTo(before);
        assertThat(saved.getOccurredAt().getYear()).isNotEqualTo(2001);
    }

    @Test
    void publicSeoEventsRejectUnknownOversizedMalformedAndUnexpectedPayloads() throws Exception {
        expectBadRequest("""
                {"events":[{"eventId":"22222222-2222-4222-8222-222222222222","event":"made_up_event","landingPage":"/revenge-trading"}]}
                """);
        expectBadRequest("""
                {"events":[{"event":"seo_landing_page_view_over_64_characters_abcdefghijklmnopqrstuvwxyz","landingPage":"/revenge-trading"}]}
                """);
        expectBadRequest("""
                {"events":[{"event":"seo_landing_page_view","landingPage":"https://tradepilotcoach.com/revenge-trading"}]}
                """);
        expectBadRequest("""
                {"events":[{"event":"seo_landing_page_view","landingPage":"/revenge-trading?token=secret"}]}
                """);
        expectBadRequest("""
                {"events":[{"event":"seo_landing_page_view","landingPage":"/revenge-trading","utmSource":"%s"}]}
                """.formatted("a".repeat(101)));
        expectBadRequest("""
                {"events":[{"event":"seo_landing_page_view","landingPage":"/revenge-trading","pnl":123,"expectancy":4.2,"strategy":"secret"}]}
                """);
    }

    @Test
    void duplicateEventIdIsIdempotent() throws Exception {
        String eventId = "33333333-3333-4333-8333-333333333333";
        postEvent(eventId, "seo_landing_page_view", "anon-seo-dup", "/tools/overtrading-calculator", "overtrading");
        postEvent(eventId, "seo_landing_page_view", "anon-seo-dup", "/tools/overtrading-calculator", "overtrading");

        long count = eventRepository.findAll().stream().filter(event -> eventId.equals(event.getEventId())).count();
        assertThat(count).isEqualTo(1);
    }

    @Test
    void seoAnalyticsDashboardRequiresAdminAndAggregatesActivation() throws Exception {
        String unique = UUID.randomUUID().toString();
        User admin = createUser("seo-admin-" + unique + "@tradepilot.local", UserRole.ADMIN, PlanType.PRO);
        User normal = createUser("seo-user-" + unique + "@tradepilot.local", UserRole.USER, PlanType.FREE);
        String anonymousId = "anon-seo-" + unique;
        LocalDateTime eventStart = LocalDateTime.now().minusMinutes(5);

        mockMvc.perform(get("/api/v1/admin/analytics/seo"))
                .andExpect(status().isUnauthorized());
        mockMvc.perform(get("/api/v1/admin/analytics/seo")
                        .header("Authorization", "Bearer " + jwtService.generateToken(normal)))
                .andExpect(status().isForbidden());
        mockMvc.perform(get("/api/v1/admin/analytics/seo/search-console"))
                .andExpect(status().isUnauthorized());
        mockMvc.perform(get("/api/v1/admin/analytics/seo/search-console")
                        .header("Authorization", "Bearer " + jwtService.generateToken(normal)))
                .andExpect(status().isForbidden());
        mockMvc.perform(get("/api/v1/admin/analytics/seo/search-console")
                        .header("Authorization", "Bearer " + jwtService.generateToken(admin)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.error").value("Google Search Console is not connected."));

        postEvent(UUID.randomUUID().toString(), "seo_landing_page_view", anonymousId, "/tools/revenge-trading-calculator", "revenge_trading");
        postEvent(UUID.randomUUID().toString(), "revenge_calculator_started", anonymousId, "/tools/revenge-trading-calculator", "revenge_trading");
        postEvent(UUID.randomUUID().toString(), "revenge_calculator_completed", anonymousId, "/tools/revenge-trading-calculator", "revenge_trading");
        postEvent(UUID.randomUUID().toString(), "revenge_calculator_cta_click", anonymousId, "/tools/revenge-trading-calculator", "revenge_trading");
        postEvent(UUID.randomUUID().toString(), "signup_started_from_seo", anonymousId, "/tools/revenge-trading-calculator", "revenge_trading");

        assertThat(eventRepository.findAll().stream().filter(event -> anonymousId.equals(event.getAnonymousId())).count()).isEqualTo(5);
        LocalDate startDate = LocalDate.now().minusDays(1);
        LocalDate endDate = LocalDate.now();
        eventRepository.findAll().stream()
                .filter(event -> anonymousId.equals(event.getAnonymousId()))
                .forEach(event -> assertThat(event.getOccurredAt()).isBetween(startDate.atStartOfDay(), endDate.plusDays(1).atStartOfDay()));
        long rawEvents = jdbcTemplate.queryForObject("""
                        select count(*) from seo_acquisition_events
                        where anonymous_id = :anonymousId
                        """,
                new MapSqlParameterSource("anonymousId", anonymousId),
                Long.class);
        assertThat(rawEvents).isEqualTo(5);
        List<String> rawOccurredAt = jdbcTemplate.queryForList("""
                        select cast(occurred_at as varchar) from seo_acquisition_events
                        where anonymous_id = :anonymousId
                        order by event_name
                        """,
                new MapSqlParameterSource("anonymousId", anonymousId),
                String.class);
        long eventsInDashboardRange = jdbcTemplate.queryForObject("""
                        select count(*) from seo_acquisition_events
                        where anonymous_id = :anonymousId
                          and occurred_at >= :start
                          and occurred_at < :end
                        """,
                new MapSqlParameterSource()
                        .addValue("anonymousId", anonymousId)
                        .addValue("start", toDatabaseTimestamp(startDate.atStartOfDay()))
                        .addValue("end", toDatabaseTimestamp(endDate.plusDays(1).atStartOfDay())),
                Long.class);
        assertThat(eventsInDashboardRange)
                .as("raw occurred_at values %s should be between %s and %s", rawOccurredAt, startDate.atStartOfDay(), endDate.plusDays(1).atStartOfDay())
                .isEqualTo(5);

        seoAnalyticsService.recordSignup(normal, attribution(anonymousId, "/tools/revenge-trading-calculator", "google", "organic", "revenge"));
        LocalDateTime signupAt = LocalDateTime.now();
        saveFeature(normal, FeatureUsageAction.FIRST_IMPORT_COMPLETED, signupAt.plusMinutes(8));
        seoAnalyticsService.markFirstImportCompleted(normal, signupAt.plusMinutes(8));
        saveFeature(normal, FeatureUsageAction.FIRST_INSIGHT_GENERATED, signupAt.plusMinutes(20));
        seoAnalyticsService.markFirstInsightGenerated(normal, signupAt.plusMinutes(20));
        normal.setPlan(PlanType.PRO);
        normal.setSubscriptionStatus(SubscriptionStatus.ACTIVE);
        seoAnalyticsService.markProConverted(normal, signupAt.plusMinutes(40));
        assertThat(attributionRepository.findByUserId(normal.getId()).orElseThrow().getFirstInsightGeneratedAt()).isEqualTo(signupAt.plusMinutes(20));

        String response = mockMvc.perform(get("/api/v1/admin/analytics/seo")
                        .param("startDate", startDate.toString())
                        .param("endDate", endDate.toString())
                        .header("Authorization", "Bearer " + jwtService.generateToken(admin)))
                .andExpect(status().isOk())
                .andReturn()
                .getResponse()
                .getContentAsString();

        JsonNode json = objectMapper.readTree(response);
        assertThat(json.get("funnel").get(0).get("count").asLong()).isGreaterThanOrEqualTo(1);
        assertThat(json.get("funnel").get(3).get("count").asLong()).isGreaterThanOrEqualTo(1);
        assertThat(json.get("funnel").get(5).get("count").asLong()).isGreaterThanOrEqualTo(1);
        assertThat(response).contains("revenge_trading").contains("google");
        assertThat(json.get("latency").get("medianSignupToFirstInsightMinutes").asDouble()).isGreaterThanOrEqualTo(20);
        assertThat(response).doesNotContain("pnlAmount").doesNotContain("setupType");
        assertThat(eventStart).isBefore(LocalDateTime.now());
    }

    @Test
    void seoAnalyticsExcludesAdminsAndExplicitlyExcludedAccountsFromConversionMetrics() throws Exception {
        String unique = UUID.randomUUID().toString();
        String landingPage = "/seo-analytics-exclusion-" + unique;
        User reportingAdmin = createUser("seo-reporting-admin-" + unique + "@tradepilot.local", UserRole.ADMIN, PlanType.PRO);
        User normal = createUser("seo-normal-" + unique + "@tradepilot.local", UserRole.USER, PlanType.FREE);
        User admin = createUser("seo-internal-admin-" + unique + "@tradepilot.local", UserRole.ADMIN, PlanType.PRO);
        User smoke = createUser("seo-smoke-" + unique + "@tradepilot.local", UserRole.USER, PlanType.FREE);
        smoke.setAnalyticsExcluded(true);
        userRepository.save(smoke);
        LocalDate today = LocalDate.now();
        JsonNode before = dashboardJson(reportingAdmin, today, today);

        for (int i = 0; i < 4; i++) {
            postEvent(UUID.randomUUID().toString(), "seo_landing_page_view", "seo-exclusion-view-" + unique + "-" + i, landingPage, null);
        }

        LocalDateTime occurredAt = LocalDateTime.now().minusMinutes(1);
        for (User user : List.of(normal, admin, smoke)) {
            seoAnalyticsService.recordSignup(user, attribution("seo-exclusion-" + user.getId(), landingPage, "google", "organic", "seo-exclusion"));
            seoAnalyticsService.markFirstImportCompleted(user, occurredAt.plusMinutes(1));
            seoAnalyticsService.markFirstInsightGenerated(user, occurredAt.plusMinutes(2));
            user.setPlan(PlanType.PRO);
            user.setSubscriptionStatus(SubscriptionStatus.ACTIVE);
            seoAnalyticsService.markProConverted(user, occurredAt.plusMinutes(3));
        }

        JsonNode json = dashboardJson(reportingAdmin, today, today);
        JsonNode landing = findLandingPage(json, landingPage);
        assertThat(landing.get("views").asLong()).isEqualTo(4);
        assertThat(landing.get("signups").asLong()).isEqualTo(1);
        assertThat(landing.get("firstImports").asLong()).isEqualTo(1);
        assertThat(landing.get("firstInsights").asLong()).isEqualTo(1);
        assertThat(landing.get("proConversions").asLong()).isEqualTo(1);
        assertThat(landing.get("signupRate").asDouble()).isEqualTo(25.0);
        assertThat(landing.get("activationRate").asDouble()).isEqualTo(100.0);
        assertThat(landing.get("paidConversionRate").asDouble()).isEqualTo(100.0);
        assertThat(funnelCount(json, "signup_completed") - funnelCount(before, "signup_completed")).isEqualTo(1);
        assertThat(funnelCount(json, "first_import") - funnelCount(before, "first_import")).isEqualTo(1);
        assertThat(funnelCount(json, "first_insight") - funnelCount(before, "first_insight")).isEqualTo(1);
        assertThat(funnelCount(json, "pro_upgrade") - funnelCount(before, "pro_upgrade")).isEqualTo(1);
        assertThat(json.get("generatedAt").asText()).endsWith("Z");
    }

    @Test
    void adminDateRangeValidationRejectsInvalidRanges() throws Exception {
        User admin = createUser("seo-date-admin@tradepilot.local", UserRole.ADMIN, PlanType.PRO);
        String token = jwtService.generateToken(admin);

        mockMvc.perform(get("/api/v1/admin/analytics/seo")
                        .param("startDate", "2026-08-30")
                        .param("endDate", "2026-08-01")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isBadRequest());

        mockMvc.perform(get("/api/v1/admin/analytics/seo")
                        .param("startDate", "2025-01-01")
                        .param("endDate", "2026-08-30")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isBadRequest());
    }

    @Test
    void firstTouchAttributionIsImmutableAndMilestonesAreServerOwned() {
        User user = createUser("seo-immutability@tradepilot.local", UserRole.USER, PlanType.FREE);
        seoAnalyticsService.recordSignup(user, attribution("anon-first", "/revenge-trading", "google", "organic", "revenge"));
        seoAnalyticsService.recordSignup(user, attribution("anon-later", "/", null, null, null));

        var attribution = attributionRepository.findByUserId(user.getId()).orElseThrow();
        assertThat(attribution.getLandingPage()).isEqualTo("/revenge-trading");
        assertThat(attribution.getUtmSource()).isEqualTo("google");
        assertThat(attribution.getUtmMedium()).isEqualTo("organic");
        assertThat(attribution.getFirstImportCompletedAt()).isNull();
        assertThat(attribution.getFirstInsightGeneratedAt()).isNull();
        assertThat(attribution.getProConvertedAt()).isNull();

        LocalDateTime firstImport = LocalDateTime.now().plusMinutes(10);
        LocalDateTime laterImport = firstImport.plusHours(1);
        seoAnalyticsService.markFirstImportCompleted(user, firstImport);
        seoAnalyticsService.markFirstImportCompleted(user, laterImport);
        assertThat(attributionRepository.findByUserId(user.getId()).orElseThrow().getFirstImportCompletedAt()).isEqualTo(firstImport);

        LocalDateTime firstInsight = LocalDateTime.now().plusMinutes(20);
        seoAnalyticsService.markFirstInsightGenerated(user, firstInsight);
        seoAnalyticsService.markFirstInsightGenerated(user, firstInsight.plusHours(2));
        assertThat(attributionRepository.findByUserId(user.getId()).orElseThrow().getFirstInsightGeneratedAt()).isEqualTo(firstInsight);

        assertThat(attributionRepository.findByUserId(user.getId()).orElseThrow().getProConvertedAt()).isNull();
        seoAnalyticsService.markProConverted(user, firstInsight.plusMinutes(5));
        assertThat(attributionRepository.findByUserId(user.getId()).orElseThrow().getProConvertedAt()).isNull();
        user.setPlan(PlanType.PRO);
        user.setSubscriptionStatus(SubscriptionStatus.ACTIVE);
        seoAnalyticsService.markProConverted(user, firstInsight.plusMinutes(15));
        seoAnalyticsService.markProConverted(user, firstInsight.plusHours(3));
        assertThat(attributionRepository.findByUserId(user.getId()).orElseThrow().getProConvertedAt()).isEqualTo(firstInsight.plusMinutes(15));
    }

    @Test
    void signupAttributionRejectsClientOwnedMilestoneFields() throws Exception {
        String body = """
                {
                  "name":"SEO Signup",
                  "email":"seo-signup-hardening@example.com",
                  "password":"password123",
                  "seoAttribution":{
                    "anonymousId":"anon-signup",
                    "landingPage":"/revenge-trading",
                    "utmSource":"google",
                    "firstInsightAt":"2026-08-30T00:00:00"
                  }
                }
                """;

        mockMvc.perform(post("/api/v1/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isBadRequest());
    }

    private void expectBadRequest(String body) throws Exception {
        mockMvc.perform(post("/api/v1/seo/events")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isBadRequest());
    }

    private void postEvent(String eventId, String event, String anonymousId, String landingPage, String tool) throws Exception {
        String body = """
                {"events":[{"eventId":"%s","event":"%s","anonymousId":"%s","landingPage":"%s","tool":"%s","utmSource":"google","utmMedium":"organic","utmCampaign":"revenge","timestamp":"2001-01-01T00:00:00Z"}]}
                """.formatted(eventId, event, anonymousId, landingPage, tool);
        mockMvc.perform(post("/api/v1/seo/events")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isNoContent());
    }

    private SeoAttributionRequest attribution(String anonymousId, String landingPage, String source, String medium, String campaign) {
        return new SeoAttributionRequest(anonymousId, landingPage, "https://www.google.com/", source, medium, campaign, null, null, LocalDateTime.now().toString());
    }

    private void saveFeature(User user, FeatureUsageAction action, LocalDateTime occurredAt) {
        featureUsageRepository.save(FeatureUsageEvent.builder()
                .userId(user.getId())
                .feature(FeatureKey.ACTIVATION)
                .action(action)
                .eventType(action == FeatureUsageAction.FIRST_INSIGHT_GENERATED ? FeatureUsageEventType.AI_GENERATION : FeatureUsageEventType.USER_ACTION)
                .createdAt(occurredAt)
                .occurredAt(occurredAt)
                .build());
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

    private JsonNode findLandingPage(JsonNode dashboard, String landingPage) {
        for (JsonNode row : dashboard.get("landingPages")) {
            if (landingPage.equals(row.get("landingPage").asText())) {
                return row;
            }
        }
        throw new AssertionError("Expected landing page row was not returned: " + landingPage);
    }

    private long funnelCount(JsonNode dashboard, String key) {
        for (JsonNode step : dashboard.get("funnel")) {
            if (key.equals(step.get("key").asText())) {
                return step.get("count").asLong();
            }
        }
        throw new AssertionError("Expected funnel step was not returned: " + key);
    }

    private JsonNode dashboardJson(User admin, LocalDate startDate, LocalDate endDate) throws Exception {
        String response = mockMvc.perform(get("/api/v1/admin/analytics/seo")
                        .param("startDate", startDate.toString())
                        .param("endDate", endDate.toString())
                        .header("Authorization", "Bearer " + jwtService.generateToken(admin)))
                .andExpect(status().isOk())
                .andReturn()
                .getResponse()
                .getContentAsString();
        return objectMapper.readTree(response);
    }

    private Timestamp toDatabaseTimestamp(LocalDateTime value) {
        return Timestamp.valueOf(value.atZone(ZoneId.systemDefault()).withZoneSameInstant(ZoneOffset.UTC).toLocalDateTime());
    }
}
