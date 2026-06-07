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

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@TestPropertySource(properties = "app.dev-tools-enabled=false")
class DevToolsDisabledTests {
    @Autowired MockMvc mockMvc;
    @Autowired UserRepository userRepository;
    @Autowired JwtService jwtService;

    @Test
    void devEndpointDisabledOutsideDevWhenPropertyFalse() throws Exception {
        User user = userRepository.save(User.builder()
                .name("disabled")
                .email("dev-disabled@example.com")
                .passwordHash("test-password-hash")
                .emailVerified(true)
                .plan(PlanType.FREE)
                .subscriptionStatus(SubscriptionStatus.NONE)
                .build());

        mockMvc.perform(post("/api/dev/seed-demo-data")
                        .header("Authorization", "Bearer " + jwtService.generateToken(user)))
                .andExpect(status().isForbidden());
    }

    @Test
    void requestIdHeaderIsEchoedOnApiResponses() throws Exception {
        mockMvc.perform(post("/api/auth/register")
                        .header("X-Request-Id", "test-request-id-123")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "name": "Request Id",
                                  "email": "request-id@example.com",
                                  "password": "password123"
                                }
                                """))
                .andExpect(status().isCreated())
                .andExpect(header().string("X-Request-Id", "test-request-id-123"));
    }
}
