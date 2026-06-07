package com.tradepilot;

import com.tradepilot.dto.auth.MessageResponse;
import com.tradepilot.dto.auth.RegisterRequest;
import com.tradepilot.service.AuthService;
import com.tradepilot.service.EmailService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.context.ActiveProfiles;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doAnswer;

@SpringBootTest
@ActiveProfiles("test")
class AuthEmailAsyncTests {

    @Autowired
    AuthService authService;

    @MockBean
    EmailService emailService;

    @Test
    void registrationDoesNotWaitForSlowVerificationEmail() {
        doAnswer(invocation -> {
            Thread.sleep(1500);
            return null;
        }).when(emailService).sendVerificationEmail(anyString(), anyString());

        RegisterRequest register = new RegisterRequest();
        register.setName("Async Email");
        register.setEmail("async-email@example.com");
        register.setPassword("Password123");

        long start = System.nanoTime();
        MessageResponse response = authService.register(register);
        long elapsedMs = (System.nanoTime() - start) / 1_000_000;

        assertThat(response.message()).isEqualTo("Account created. Please check your email to verify your account.");
        assertThat(elapsedMs).isLessThan(1000);
    }
}
