package com.tradepilot.controller;

import com.tradepilot.dto.auth.*;
import com.tradepilot.entity.User;
import com.tradepilot.service.AuthService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/auth")
public class AuthController {
    private final AuthService authService;

    @PostMapping("/register")
    @ResponseStatus(HttpStatus.CREATED)
    public MessageResponse register(@Valid @RequestBody RegisterRequest request) {
        return authService.register(request);
    }

    @PostMapping("/login")
    public AuthResponse login(@Valid @RequestBody LoginRequest request) {
        return authService.login(request);
    }

    @GetMapping("/me")
    public UserMeResponse me(@AuthenticationPrincipal User user) {
        if (user == null) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Authentication required");
        }
        return authService.me(user);
    }

    @PostMapping("/verify-email")
    public MessageResponse verifyEmail(@Valid @RequestBody TokenRequest request) {
        return authService.verifyEmail(request);
    }

    @PostMapping("/resend-verification")
    public MessageResponse resendVerification(@Valid @RequestBody EmailRequest request) {
        return authService.resendVerification(request);
    }

    @PostMapping("/forgot-password")
    public MessageResponse forgotPassword(@Valid @RequestBody EmailRequest request) {
        return authService.forgotPassword(request);
    }

    @PostMapping("/reset-password")
    public MessageResponse resetPassword(@Valid @RequestBody ResetPasswordRequest request) {
        return authService.resetPassword(request);
    }

    @java.lang.SuppressWarnings("all")
    public AuthController(final AuthService authService) {
        this.authService = authService;
    }
}
