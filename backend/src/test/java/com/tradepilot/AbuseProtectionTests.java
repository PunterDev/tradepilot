package com.tradepilot;

import com.tradepilot.billing.PlanType;
import com.tradepilot.billing.SubscriptionStatus;
import com.tradepilot.entity.User;
import com.tradepilot.enums.UserRole;
import com.tradepilot.repository.UserRepository;
import com.tradepilot.security.JwtService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest(classes = {TradePilotApplication.class, AbuseProtectionTests.FailingController.class})
@AutoConfigureMockMvc
@ActiveProfiles("test")
@TestPropertySource(properties = {
        "app.rate-limit.auth-login.max-requests=1",
        "app.rate-limit.auth-login.window-seconds=60",
        "app.rate-limit.csv-import.max-requests=1",
        "app.rate-limit.csv-import.window-seconds=60",
        "app.rate-limit.ai-generation.max-requests=1",
        "app.rate-limit.ai-generation.window-seconds=60",
        "app.rate-limit.billing-checkout.max-requests=1",
        "app.rate-limit.billing-checkout.window-seconds=60",
        "app.rate-limit.trust-forwarded-headers=true"
})
class AbuseProtectionTests {
    @Autowired MockMvc mockMvc;
    @Autowired UserRepository userRepository;
    @Autowired JwtService jwtService;

    @Test
    void rateLimitBlocksRepeatedLoginRequests() throws Exception {
        String isolatedClientIp = "203.0.113.42";
        String loginBody = """
                {
                  "email": "blocked-login@example.com",
                  "password": "wrong-password"
                }
                """;

        mockMvc.perform(post("/api/auth/login")
                        .header("X-Forwarded-For", isolatedClientIp)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(loginBody))
                .andExpect(status().isUnauthorized());

        for (int i = 0; i < 10; i++) {
            var result = mockMvc.perform(post("/api/auth/login")
                            .header("X-Forwarded-For", isolatedClientIp)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(loginBody))
                    .andReturn();

            if (result.getResponse().getStatus() == 429) {
                mockMvc.perform(post("/api/auth/login")
                                .header("X-Forwarded-For", isolatedClientIp)
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(loginBody))
                        .andExpect(status().isTooManyRequests())
                        .andExpect(header().exists("Retry-After"))
                        .andExpect(jsonPath("$.error").value("Too many requests. Please wait and try again."))
                        .andExpect(jsonPath("$.userMessage").value("Too many requests. Please wait and try again."))
                        .andExpect(jsonPath("$.errorCode").value("RATE_LIMIT_EXCEEDED"))
                        .andExpect(jsonPath("$.status").value(429))
                        .andExpect(jsonPath("$.requestId").exists());
                return;
            }
        }

        throw new AssertionError("Expected repeated login attempts to be rate limited");
    }

    @Test
    void rateLimitCoversVersionedLoginRequests() throws Exception {
        String isolatedClientIp = "203.0.113.43";
        String loginBody = """
                {
                  "email": "blocked-v1-login@example.com",
                  "password": "wrong-password"
                }
                """;

        mockMvc.perform(post("/api/v1/auth/login")
                        .header("X-Forwarded-For", isolatedClientIp)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(loginBody))
                .andExpect(status().isUnauthorized());

        mockMvc.perform(post("/api/v1/auth/login")
                        .header("X-Forwarded-For", isolatedClientIp)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(loginBody))
                .andExpect(status().isTooManyRequests())
                .andExpect(jsonPath("$.errorCode").value("RATE_LIMIT_EXCEEDED"))
                .andExpect(jsonPath("$.path").value("/api/auth/login"));
    }

    @Test
    void rateLimitBlocksRepeatedCsvImports() throws Exception {
        User user = createUser("csv-limit@example.com", UserRole.USER);
        String token = jwtService.generateToken(user);

        mockMvc.perform(post("/api/v1/trades/import")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("[]"))
                .andExpect(status().isOk());

        mockMvc.perform(post("/api/v1/trades/import")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("[]"))
                .andExpect(status().isTooManyRequests())
                .andExpect(jsonPath("$.errorCode").value("RATE_LIMIT_EXCEEDED"))
                .andExpect(jsonPath("$.path").value("/api/trades/import"));
    }

    @Test
    void rateLimitBlocksRepeatedCsvImportPreviewRequests() throws Exception {
        User user = createUser("csv-preview-limit@example.com", UserRole.USER);
        String token = jwtService.generateToken(user);

        MockMultipartFile file = new MockMultipartFile("file", "trades.csv", "text/csv", "Symbol,Date\nMES,2026-04-01\n".getBytes());

        mockMvc.perform(multipart("/api/v1/trades/import/preview")
                        .file(file)
                        .param("accountId", "999999")
                        .header("Authorization", "Bearer " + token))
                .andReturn();

        mockMvc.perform(multipart("/api/v1/trades/import/preview")
                        .file(file)
                        .param("accountId", "999999")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isTooManyRequests())
                .andExpect(jsonPath("$.errorCode").value("RATE_LIMIT_EXCEEDED"))
                .andExpect(jsonPath("$.path").value("/api/trades/import/preview"));
    }

    @Test
    void rateLimitBlocksRepeatedCsvImportSaveRequests() throws Exception {
        User user = createUser("csv-save-limit@example.com", UserRole.USER);
        String token = jwtService.generateToken(user);

        MockMultipartFile file = new MockMultipartFile("file", "trades.csv", "text/csv", "Symbol,Date\nMES,2026-04-01\n".getBytes());

        mockMvc.perform(multipart("/api/v1/trades/import/csv")
                        .file(file)
                        .param("accountId", "999999")
                        .header("Authorization", "Bearer " + token))
                .andReturn();

        mockMvc.perform(multipart("/api/v1/trades/import/csv")
                        .file(file)
                        .param("accountId", "999999")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isTooManyRequests())
                .andExpect(jsonPath("$.errorCode").value("RATE_LIMIT_EXCEEDED"))
                .andExpect(jsonPath("$.path").value("/api/trades/import/csv"));
    }

    @Test
    void rateLimitBlocksRepeatedBillingCheckoutAttempts() throws Exception {
        User user = createUser("billing-limit@example.com", UserRole.USER);
        String token = jwtService.generateToken(user);

        mockMvc.perform(post("/api/v1/billing/create-credit-pack-checkout-session")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"packId\":\"STARTER_25\"}"))
                .andReturn();

        mockMvc.perform(post("/api/v1/billing/create-credit-pack-checkout-session")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"packId\":\"STARTER_25\"}"))
                .andExpect(status().isTooManyRequests())
                .andExpect(jsonPath("$.errorCode").value("RATE_LIMIT_EXCEEDED"))
                .andExpect(jsonPath("$.path").value("/api/billing/create-credit-pack-checkout-session"));
    }

    @Test
    void rateLimitBlocksRepeatedTradeReplayGenerationAttempts() throws Exception {
        User user = createUser("replay-limit@example.com", UserRole.USER);
        String token = jwtService.generateToken(user);

        mockMvc.perform(post("/api/v1/trades/999999/replay/generate")
                        .header("Authorization", "Bearer " + token))
                .andReturn();

        mockMvc.perform(post("/api/v1/trades/999999/replay/generate")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isTooManyRequests())
                .andExpect(jsonPath("$.errorCode").value("RATE_LIMIT_EXCEEDED"))
                .andExpect(jsonPath("$.path").value("/api/trades/999999/replay/generate"));
    }

    @Test
    void rateLimitBlocksRepeatedPlaybookAiDraftAttempts() throws Exception {
        User user = createUser("playbook-ai-limit@example.com", UserRole.USER);
        String token = jwtService.generateToken(user);

        mockMvc.perform(post("/api/v1/ai/playbook/draft-from-description")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andReturn();

        mockMvc.perform(post("/api/v1/ai/playbook/draft-from-description")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isTooManyRequests())
                .andExpect(jsonPath("$.errorCode").value("RATE_LIMIT_EXCEEDED"))
                .andExpect(jsonPath("$.path").value("/api/ai/playbook/draft-from-description"));
    }

    @Test
    void rateLimitBlocksRepeatedPerformanceTimelineRegenerateAttempts() throws Exception {
        User user = createUser("timeline-ai-limit@example.com", UserRole.USER);
        String token = jwtService.generateToken(user);

        mockMvc.perform(post("/api/v1/ai/performance-timeline/regenerate")
                        .header("Authorization", "Bearer " + token)
                        .param("accountId", "999999"))
                .andReturn();

        mockMvc.perform(post("/api/v1/ai/performance-timeline/regenerate")
                        .header("Authorization", "Bearer " + token)
                        .param("accountId", "999999"))
                .andExpect(status().isTooManyRequests())
                .andExpect(jsonPath("$.errorCode").value("RATE_LIMIT_EXCEEDED"))
                .andExpect(jsonPath("$.path").value("/api/ai/performance-timeline/regenerate"));
    }

    @Test
    void genericServerErrorReturnsSafeBodyWithRequestId() throws Exception {
        User user = userRepository.save(User.builder()
                .name("Failure Test")
                .email("failure-test@example.com")
                .passwordHash("test-password-hash")
                .emailVerified(true)
                .plan(PlanType.FREE)
                .subscriptionStatus(SubscriptionStatus.NONE)
                .build());

        mockMvc.perform(get("/api/test/fail")
                        .header("Authorization", "Bearer " + jwtService.generateToken(user))
                        .header("X-Request-Id", "safe-500-request-id"))
                .andExpect(status().isInternalServerError())
                .andExpect(header().string("X-Request-Id", "safe-500-request-id"))
                .andExpect(header().string("X-Request-ID", "safe-500-request-id"))
                .andExpect(jsonPath("$.error").value("Something went wrong. Please try again."))
                .andExpect(jsonPath("$.userMessage").value("Something went wrong. Please try again."))
                .andExpect(jsonPath("$.errorCode").value("INTERNAL_ERROR"))
                .andExpect(jsonPath("$.status").value(500))
                .andExpect(jsonPath("$.path").value("/api/test/fail"))
                .andExpect(jsonPath("$.requestId").value("safe-500-request-id"))
                .andExpect(jsonPath("$.trace").doesNotExist());
    }

    @Test
    void unauthenticatedRequestReturnsStructuredRequestId() throws Exception {
        mockMvc.perform(get("/api/v1/accounts")
                        .header("X-Request-ID", "unauth-request-id"))
                .andExpect(status().isUnauthorized())
                .andExpect(header().string("X-Request-ID", "unauth-request-id"))
                .andExpect(jsonPath("$.requestId").value("unauth-request-id"))
                .andExpect(jsonPath("$.status").value(401))
                .andExpect(jsonPath("$.errorCode").value("AUTHENTICATION_REQUIRED"))
                .andExpect(jsonPath("$.userMessage").value("Authentication required"))
                .andExpect(jsonPath("$.path").value("/api/accounts"));
    }

    @Test
    void accessDeniedReturnsStructuredRequestId() throws Exception {
        User user = createUser("forbidden-user@example.com", UserRole.USER);

        mockMvc.perform(get("/api/v1/admin/users")
                        .header("Authorization", "Bearer " + jwtService.generateToken(user))
                        .header("X-Request-ID", "forbidden-request-id"))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.requestId").value("forbidden-request-id"))
                .andExpect(jsonPath("$.status").value(403))
                .andExpect(jsonPath("$.errorCode").value("ACCESS_DENIED"))
                .andExpect(jsonPath("$.userMessage").value("Access denied"));
    }

    @Test
    void validationErrorReturnsStructuredRequestId() throws Exception {
        mockMvc.perform(post("/api/v1/auth/register")
                        .header("X-Request-ID", "validation-request-id")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.requestId").value("validation-request-id"))
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.errorCode").value("VALIDATION_FAILED"))
                .andExpect(jsonPath("$.userMessage").value("Validation failed"))
                .andExpect(jsonPath("$.details").exists());
    }

    @RestController
    @RequestMapping("/api/test")
    static class FailingController {
        @GetMapping("/fail")
        void fail() {
            throw new IllegalStateException("internal implementation detail");
        }
    }

    private User createUser(String email, UserRole role) {
        return userRepository.save(User.builder()
                .name("Test User")
                .email(email)
                .passwordHash("test-password-hash")
                .emailVerified(true)
                .role(role)
                .plan(PlanType.FREE)
                .subscriptionStatus(SubscriptionStatus.NONE)
                .build());
    }
}
