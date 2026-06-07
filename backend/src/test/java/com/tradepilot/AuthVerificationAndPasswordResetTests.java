package com.tradepilot;

import com.tradepilot.dto.auth.*;
import com.tradepilot.entity.User;
import com.tradepilot.enums.AuthTokenType;
import com.tradepilot.exception.EmailNotVerifiedException;
import com.tradepilot.repository.UserRepository;
import com.tradepilot.service.AuthService;
import com.tradepilot.service.AuthTokenService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.test.context.ActiveProfiles;

import java.time.Duration;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@SpringBootTest
@ActiveProfiles("test")
class AuthVerificationAndPasswordResetTests {

    @Autowired
    AuthService authService;

    @Autowired
    AuthTokenService authTokenService;

    @Autowired
    UserRepository userRepository;

    @Test
    void emailVerificationAndPasswordResetFlow() {
        RegisterRequest register = new RegisterRequest();
        register.setName("Reset Flow");
        register.setEmail("reset-flow@example.com");
        register.setPassword("oldPassword1");

        MessageResponse registerResponse = authService.register(register);
        assertThat(registerResponse.message()).isEqualTo("Account created. Please check your email to verify your account.");

        assertThatThrownBy(() -> authService.login(login("reset-flow@example.com", "oldPassword1")))
                .isInstanceOf(EmailNotVerifiedException.class)
                .hasMessage("Please verify your email before logging in.");

        User user = userRepository.findByEmail("reset-flow@example.com").orElseThrow();
        String verificationToken = authTokenService.createToken(user, AuthTokenType.EMAIL_VERIFICATION, Duration.ofHours(1));

        assertThat(authService.verifyEmail(new TokenRequest(verificationToken)).message())
                .isEqualTo("Email verified. You can now log in.");
        assertThat(authService.login(login("reset-flow@example.com", "oldPassword1")).getToken()).isNotBlank();

        assertThat(authService.forgotPassword(new EmailRequest("reset-flow@example.com")).message())
                .isEqualTo("If an account exists, a reset link has been sent.");

        String resetToken = authTokenService.createToken(user, AuthTokenType.PASSWORD_RESET, Duration.ofHours(1));
        assertThat(authService.resetPassword(new ResetPasswordRequest(resetToken, "newPassword1")).message())
                .isEqualTo("Password reset. You can now log in.");

        assertThatThrownBy(() -> authService.login(login("reset-flow@example.com", "oldPassword1")))
                .isInstanceOf(BadCredentialsException.class);
        assertThat(authService.login(login("reset-flow@example.com", "newPassword1")).getToken()).isNotBlank();
        assertThatThrownBy(() -> authService.resetPassword(new ResetPasswordRequest(resetToken, "anotherPassword1")))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Invalid or expired token.");
    }

    private LoginRequest login(String email, String password) {
        LoginRequest request = new LoginRequest();
        request.setEmail(email);
        request.setPassword(password);
        return request;
    }
}
