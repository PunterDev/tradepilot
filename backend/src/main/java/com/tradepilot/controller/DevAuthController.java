package com.tradepilot.controller;

import com.tradepilot.dto.auth.AuthResponse;
import com.tradepilot.entity.User;
import com.tradepilot.repository.UserRepository;
import com.tradepilot.security.JwtService;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/dev")
@ConditionalOnProperty(name = "app.dev-auth.enabled", havingValue = "true")
public class DevAuthController {
    private final UserRepository userRepository;
    private final JwtService jwtService;
    private final PasswordEncoder passwordEncoder;

    @GetMapping("/token")
    public AuthResponse token() {
        User user = userRepository.findAll().stream().findFirst().orElseGet(() -> {
            User newUser = User.builder().name("Dev User").email("dev@tradepilot.com").passwordHash(passwordEncoder.encode("password")).emailVerified(true).build();
            return userRepository.save(newUser);
        });
        String token = jwtService.generateToken(user);
        return AuthResponse.of(token, user.getId(), user.getName(), user.getEmail(), user.getPlan().name(), user.getRole().name());
    }

    @java.lang.SuppressWarnings("all")
    public DevAuthController(final UserRepository userRepository, final JwtService jwtService, final PasswordEncoder passwordEncoder) {
        this.userRepository = userRepository;
        this.jwtService = jwtService;
        this.passwordEncoder = passwordEncoder;
    }
}
