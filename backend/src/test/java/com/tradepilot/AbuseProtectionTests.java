package com.tradepilot;

import com.tradepilot.billing.PlanType;
import com.tradepilot.billing.SubscriptionStatus;
import com.tradepilot.entity.User;
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
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest(classes = {TradePilotApplication.class, AbuseProtectionTests.FailingController.class})
@AutoConfigureMockMvc
@ActiveProfiles("test")
@TestPropertySource(properties = {
        "app.rate-limit.auth-login.max-requests=1",
        "app.rate-limit.auth-login.window-seconds=60"
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
                        .andExpect(jsonPath("$.error").value("Too many requests. Please wait and try again."));
                return;
            }
        }

        throw new AssertionError("Expected repeated login attempts to be rate limited");
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
                .andExpect(jsonPath("$.error").value("Something went wrong. Please try again."))
                .andExpect(jsonPath("$.requestId").value("safe-500-request-id"))
                .andExpect(jsonPath("$.trace").doesNotExist());
    }

    @RestController
    @RequestMapping("/api/test")
    static class FailingController {
        @GetMapping("/fail")
        void fail() {
            throw new IllegalStateException("internal implementation detail");
        }
    }
}
