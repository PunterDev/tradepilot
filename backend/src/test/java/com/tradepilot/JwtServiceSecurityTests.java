package com.tradepilot;

import com.tradepilot.billing.PlanType;
import com.tradepilot.billing.SubscriptionStatus;
import com.tradepilot.entity.User;
import com.tradepilot.enums.UserRole;
import com.tradepilot.security.JwtService;
import org.junit.jupiter.api.Test;

import java.util.Base64;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class JwtServiceSecurityTests {

    @Test
    void rejectsMissingWeakOrMalformedSecrets() {
        assertThatThrownBy(() -> new JwtService("", 30))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("JWT secret is missing");
        assertThatThrownBy(() -> new JwtService("not-base64", 30))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("Base64");
        assertThatThrownBy(() -> new JwtService(Base64.getEncoder().encodeToString("short".getBytes()), 30))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("too weak");
        assertThatThrownBy(() -> new JwtService(strongSecret(), 0))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("expiration");
    }

    @Test
    void signsAndValidatesTokenWithStrongSecret() {
        JwtService jwtService = new JwtService(strongSecret(), 30);
        User user = User.builder()
                .email("jwt@example.com")
                .passwordHash("hash")
                .name("JWT User")
                .role(UserRole.USER)
                .plan(PlanType.FREE)
                .subscriptionStatus(SubscriptionStatus.NONE)
                .build();

        String token = jwtService.generateToken(user);

        assertThat(jwtService.extractUsername(token)).isEqualTo("jwt@example.com");
        assertThat(jwtService.isTokenValid(token, user)).isTrue();
    }

    private String strongSecret() {
        return Base64.getEncoder().encodeToString("0123456789abcdef0123456789abcdef".getBytes());
    }
}
