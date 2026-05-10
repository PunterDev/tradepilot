package com.tradepilot.controller;

import com.tradepilot.dto.auth.AuthResponse;
import com.tradepilot.entity.User;
import com.tradepilot.repository.UserRepository;
import com.tradepilot.security.JwtService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/dev")
@RequiredArgsConstructor
public class DevAuthController {

    private final UserRepository userRepository;
    private final JwtService jwtService;
    private final PasswordEncoder passwordEncoder;

    @GetMapping("/token")
    public AuthResponse token() {
        User user = userRepository.findAll().stream().findFirst().orElseGet(() -> {
            User newUser = User.builder()
                    .name("Dev User")
                    .email("dev@tradepilot.com")
                    .passwordHash(passwordEncoder.encode("password")) // ✅ FIXED
                    .build();

            return userRepository.save(newUser);
        });

        String token = jwtService.generateToken(user);

        return AuthResponse.builder()
                .token(token)
                .build();
    }
}