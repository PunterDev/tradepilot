package com.tradepilot;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.tradepilot.billing.PlanType;
import com.tradepilot.billing.SubscriptionStatus;
import com.tradepilot.config.OpenAiProperties;
import com.tradepilot.dto.auth.LoginRequest;
import com.tradepilot.dto.auth.RegisterRequest;
import com.tradepilot.dto.auth.ResetPasswordRequest;
import com.tradepilot.dto.auth.TokenRequest;
import com.tradepilot.entity.User;
import com.tradepilot.enums.AuthTokenType;
import com.tradepilot.enums.UserRole;
import com.tradepilot.repository.UserRepository;
import com.tradepilot.security.JwtService;
import com.tradepilot.service.AuthService;
import com.tradepilot.service.AuthTokenService;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import io.jsonwebtoken.security.Keys;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.Base64;
import java.util.Date;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class AuthHardeningTests {

    private static final String TEST_JWT_SECRET = "0123456789abcdef0123456789abcdef";

    @Autowired MockMvc mockMvc;
    @Autowired ObjectMapper objectMapper;
    @Autowired UserRepository userRepository;
    @Autowired PasswordEncoder passwordEncoder;
    @Autowired JwtService jwtService;
    @Autowired AuthService authService;
    @Autowired AuthTokenService authTokenService;

    @Test
    void protectedEndpointRejectsExpiredMalformedWrongSecretAndMissingSubjectTokens() throws Exception {
        User user = createUser("auth-hardening-tokens@example.com", UserRole.USER);

        mockMvc.perform(get("/api/v1/accounts").header("Authorization", "Bearer " + expiredToken(user)))
                .andExpect(status().isUnauthorized());
        mockMvc.perform(get("/api/v1/accounts").header("Authorization", "Bearer malformed.token.value"))
                .andExpect(status().isUnauthorized());
        mockMvc.perform(get("/api/v1/accounts").header("Authorization", "Bearer " + wrongSecretToken(user)))
                .andExpect(status().isUnauthorized());
        mockMvc.perform(get("/api/v1/accounts").header("Authorization", "Bearer " + missingSubjectToken()))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void forgedAdminClaimDoesNotOverrideDatabaseRole() throws Exception {
        User user = createUser("auth-hardening-forged-admin@example.com", UserRole.USER);
        String forgedAdminToken = signedToken(user.getEmail(), Map.of("role", "ADMIN", "tokenVersion", tokenVersion(user)), futureDate());

        mockMvc.perform(get("/api/v1/admin/users").header("Authorization", "Bearer " + forgedAdminToken))
                .andExpect(status().isForbidden());
    }

    @Test
    void deletedUserTokenIsRejectedWithUnauthorized() throws Exception {
        User user = createUser("auth-hardening-deleted@example.com", UserRole.USER);
        String token = jwtService.generateToken(user);
        userRepository.delete(user);

        mockMvc.perform(get("/api/v1/accounts").header("Authorization", "Bearer " + token))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void passwordResetTokensExpireAndAreSingleUse() {
        User user = createUser("auth-hardening-reset-token@example.com", UserRole.USER);
        String expiredResetToken = authTokenService.createToken(user, AuthTokenType.PASSWORD_RESET, Duration.ofSeconds(-1));

        assertThatThrownBy(() -> authService.resetPassword(new ResetPasswordRequest(expiredResetToken, "newPassword1")))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Invalid or expired token.");

        String resetToken = authTokenService.createToken(user, AuthTokenType.PASSWORD_RESET, Duration.ofMinutes(30));
        assertThat(authService.resetPassword(new ResetPasswordRequest(resetToken, "newPassword1")).message())
                .isEqualTo("Password reset. You can now log in.");
        assertThatThrownBy(() -> authService.resetPassword(new ResetPasswordRequest(resetToken, "newPassword2")))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Invalid or expired token.");
    }

    @Test
    void passwordResetInvalidatesExistingAccessTokens() throws Exception {
        User user = createUser("auth-hardening-reset-invalidates@example.com", UserRole.USER);
        String oldAccessToken = login(user.getEmail(), "password123");
        String resetToken = authTokenService.createToken(user, AuthTokenType.PASSWORD_RESET, Duration.ofMinutes(30));

        authService.resetPassword(new ResetPasswordRequest(resetToken, "newPassword1"));

        mockMvc.perform(get("/api/v1/auth/me").header("Authorization", "Bearer " + oldAccessToken))
                .andExpect(status().isUnauthorized());
        String newAccessToken = login(user.getEmail(), "newPassword1");
        mockMvc.perform(get("/api/v1/auth/me").header("Authorization", "Bearer " + newAccessToken))
                .andExpect(status().isOk());
    }

    @Test
    void authRequestToStringRedactsSecrets() {
        LoginRequest loginRequest = new LoginRequest();
        loginRequest.setEmail("secret-login@example.com");
        loginRequest.setPassword("plain-login-password");
        RegisterRequest registerRequest = new RegisterRequest();
        registerRequest.setName("Secret Register");
        registerRequest.setEmail("secret-register@example.com");
        registerRequest.setPassword("plain-register-password");

        assertThat(loginRequest.toString()).contains("password=[REDACTED]").doesNotContain("plain-login-password");
        assertThat(registerRequest.toString()).contains("password=[REDACTED]").doesNotContain("plain-register-password");
        assertThat(new ResetPasswordRequest("raw-reset-token", "plain-reset-password").toString())
                .contains("token=[REDACTED]", "newPassword=[REDACTED]")
                .doesNotContain("raw-reset-token", "plain-reset-password");
        assertThat(new TokenRequest("raw-verification-token").toString())
                .contains("token=[REDACTED]")
                .doesNotContain("raw-verification-token");
    }

    @Test
    void openAiPropertiesToStringDoesNotExposeApiKey() {
        OpenAiProperties properties = new OpenAiProperties();
        properties.setApiKey("super-secret-test-openai-key");
        properties.setModel("gpt-test-model");

        assertThat(properties.toString())
                .contains("apiKeyConfigured=true")
                .contains("model=gpt-test-model")
                .doesNotContain("super-secret-test-openai-key");
    }

    private User createUser(String email, UserRole role) {
        return userRepository.save(User.builder()
                .name(email)
                .email(email)
                .passwordHash(passwordEncoder.encode("password123"))
                .emailVerified(true)
                .role(role)
                .plan(PlanType.PRO)
                .subscriptionStatus(SubscriptionStatus.ACTIVE)
                .build());
    }

    private String login(String email, String password) throws Exception {
        String response = mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"email":"%s","password":"%s"}
                                """.formatted(email, password)))
                .andExpect(status().isOk())
                .andReturn()
                .getResponse()
                .getContentAsString();
        return objectMapper.readTree(response).get("token").asText();
    }

    private String expiredToken(User user) {
        return signedToken(user.getEmail(), Map.of("tokenVersion", tokenVersion(user)), new Date(System.currentTimeMillis() - 60_000L));
    }

    private String wrongSecretToken(User user) {
        byte[] wrongSecret = "abcdef0123456789abcdef0123456789".getBytes(StandardCharsets.UTF_8);
        return Jwts.builder()
                .setClaims(Map.of("tokenVersion", tokenVersion(user)))
                .setSubject(user.getEmail())
                .setIssuedAt(new Date())
                .setExpiration(futureDate())
                .signWith(Keys.hmacShaKeyFor(wrongSecret), SignatureAlgorithm.HS256)
                .compact();
    }

    private String missingSubjectToken() {
        return signedToken(null, Map.of("tokenVersion", 0), futureDate());
    }

    private String signedToken(String subject, Map<String, Object> claims, Date expiration) {
        var builder = Jwts.builder()
                .setClaims(claims)
                .setIssuedAt(new Date())
                .setExpiration(expiration)
                .signWith(Keys.hmacShaKeyFor(TEST_JWT_SECRET.getBytes(StandardCharsets.UTF_8)), SignatureAlgorithm.HS256);
        if (subject != null) {
            builder.setSubject(subject);
        }
        return builder.compact();
    }

    private Date futureDate() {
        return new Date(System.currentTimeMillis() + 1_800_000L);
    }

    private int tokenVersion(User user) {
        return user.getTokenVersion() == null ? 0 : user.getTokenVersion();
    }
}
